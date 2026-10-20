package com.nopo.cheonnyeon.data.visit

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.nopo.cheonnyeon.core.common.Clock
import com.nopo.cheonnyeon.core.common.DispatcherProvider
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.data.local.MyVisitDao
import com.nopo.cheonnyeon.data.local.PendingOpDao
import com.nopo.cheonnyeon.data.local.PendingOpEntity
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.VerifyMethod
import com.nopo.cheonnyeon.domain.model.VisitRef
import com.nopo.cheonnyeon.domain.rule.StampGradePolicy
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Date

/** 인증 결과: 얻은 세월과 등급, 칭호 승급 여부 (S-701 도장 연출이 쓴다) */
data class AwardResult(
    val storeName: String,
    val years: Int,
    val grade: com.nopo.cheonnyeon.domain.model.StampGrade,
    val firstVisit: Boolean,
    val totalYearsAfter: Int,
    val titleUp: com.nopo.cheonnyeon.domain.model.Title?,
    val method: VerifyMethod,
    val pendingSync: Boolean,
)

@Serializable
private data class PendingVisit(
    val storeId: String,
    val storeName: String,
    val sigungu: String,
    val years: Int,
    val grade: String,
    val method: String,
    val photoVerified: Boolean,
    val atMillis: Long,
    val firstVisit: Boolean,
    val visitCount: Int,
    val receiptHash: String?,
    val titleId: String,
)

/**
 * 방문 기록 (S-302·S-306, ADR-09): 기기에 먼저 기록하고 대기열에 넣은 뒤 서버로 묶음 쓰기.
 * 위치 좌표·영수증 내용은 저장하지 않는다 (가게 id·이름·세월·방법만).
 */
class VisitRepository(
    private val visitDao: MyVisitDao,
    private val pendingDao: PendingOpDao,
    private val db: () -> FirebaseFirestore,
    private val uid: () -> String?,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val syncLock = Mutex()
    private val _duplicateNotices = MutableStateFlow<List<String>>(emptyList())

    /** 서버가 같은 영수증을 이미 썼다고 거부해서 기록이 되돌려진 가게 이름들. 화면이 알린 뒤 [dismissDuplicateNotice]로 비운다 */
    val duplicateNotices: StateFlow<List<String>> = _duplicateNotices

    fun dismissDuplicateNotice() { _duplicateNotices.value = emptyList() }

    /** 내 방문 문서가 서버에 있는지. 확인하지 못하면 null */
    private suspend fun visitExistsOnServer(user: String, storeId: String): Boolean? = try {
        withTimeout(SYNC_TIMEOUT_MS) { db().collection("users").document(user).collection("visits").document(storeId).get(com.google.firebase.firestore.Source.SERVER).await().exists() }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) null else throw e
    } catch (_: Exception) {
        null
    }

    val pendingCount: Flow<Int> get() = pendingDao.observeCount()

    suspend fun visitRefs(): List<VisitRef> =
        visitDao.observeAll().first().map { VisitRef(it.storeId, Instant.ofEpochMilli(it.lastVisitedAt), isTest = it.storeId.startsWith(Store.TEST_PREFIX)) }

    suspend fun record(store: Store, method: VerifyMethod, photoVerified: Boolean, receiptHash: String?): AwardResult =
        withContext(dispatchers.io) {
            val now = clock.now()
            val year = now.atZone(SeoulZone).year
            val before = visitDao.observeAll().first()
            val totalBefore = before.filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sumOf { it.yearsAwarded }
            val firstVisit = before.none { it.storeId == store.id }
            val years = YearsCalculator.yearsAtVisit(store.foundedYear, year)
            visitDao.record(store.id, years, method.name, now.toEpochMilli())
            val saved = visitDao.byStore(store.id)!!
            val awarded = if (firstVisit && !store.isTest) years else 0
            val totalAfter = totalBefore + awarded
            val titleUp = TitlePolicy.titleUp(totalBefore, totalAfter)
            val grade = StampGradePolicy.gradeFor(saved.yearsAwarded)
            val op = PendingVisit(
                storeId = store.id, storeName = store.name, sigungu = store.sigungu, years = saved.yearsAwarded,
                grade = grade.name, method = method.name, photoVerified = photoVerified, atMillis = now.toEpochMilli(),
                firstVisit = firstVisit, visitCount = saved.visitCount, receiptHash = receiptHash,
                titleId = TitlePolicy.titleFor(totalAfter).name,
            )
            // 테스트 가게 기록은 기기에만 남기고 서버로 보내지 않는다 (FR-TST-02)
            if (!store.isTest) pendingDao.add(PendingOpEntity(type = TYPE_VISIT, payload = json.encodeToString(op), createdAt = now.toEpochMilli()))
            AwardResult(store.name, saved.yearsAwarded, grade, firstVisit, totalAfter, titleUp, method, pendingSync = true)
        }

    /** 대기열을 순서대로 서버에 보낸다. 연결이 없으면 남겨 두고 다음 기회에 다시 보낸다. @return 남은 개수 */
    suspend fun sync(): Int = syncLock.withLock {
        val user = uid() ?: return@withLock pendingDao.all().size
        for (op in pendingDao.all()) {
            val visit = try { json.decodeFromString<PendingVisit>(op.payload) } catch (_: Exception) { pendingDao.remove(op.id); continue }
            try {
                withTimeout(SYNC_TIMEOUT_MS) { commit(user, visit) }
                pendingDao.remove(op.id)
                visitDao.markSynced(visit.storeId)
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) { pendingDao.bumpAttempts(op.id); break } else throw e
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && visit.receiptHash != null) {
                    // 거부됐다. 첫 방문 기록이 이미 서버에 있으면 앞선 전송이 성공했는데 응답만 못 받은 재전송이라 성공으로 본다 (아키텍처 5.3)
                    val onServer = if (visit.firstVisit) visitExistsOnServer(user, visit.storeId) else false
                    when {
                        onServer == null -> { pendingDao.bumpAttempts(op.id); break } // 확인하지 못했으니 다음에 다시
                        onServer -> { pendingDao.remove(op.id); visitDao.markSynced(visit.storeId) }
                        else -> {
                            // 해시 문서가 이미 있음 = 같은 영수증 재사용. 기기 기록을 되돌리고 사용자에게 알린다
                            pendingDao.remove(op.id)
                            if (visit.firstVisit) visitDao.delete(visit.storeId)
                            _duplicateNotices.update { it + visit.storeName }
                        }
                    }
                } else {
                    pendingDao.bumpAttempts(op.id)
                    break
                }
            } catch (_: Exception) {
                pendingDao.bumpAttempts(op.id)
                break
            }
        }
        pendingDao.all().size
    }

    /**
     * 서버의 방문 기록을 기기로 내려받는다 (S-307, FR-AUTH-10). 재설치·다른 기기에서 로그인했을 때 세월과 도장을 되살린다.
     * 기기에 이미 있는 가게는 건드리지 않는다. @return 새로 채운 도장 수, 실패하면 null
     */
    suspend fun restoreFromServer(): Int? {
        val user = uid() ?: return null
        return try {
            val docs = withTimeout(SYNC_TIMEOUT_MS) { db().collection("users").document(user).collection("visits").get().await() }
            var added = 0
            for (d in docs.documents) {
                if (visitDao.byStore(d.id) != null) continue
                val first = d.getTimestamp("firstVisitedAt")?.toDate()?.time ?: continue
                visitDao.put(
                    com.nopo.cheonnyeon.data.local.MyVisitEntity(
                        storeId = d.id,
                        yearsAwarded = (d.getLong("yearsAwarded") ?: continue).toInt(),
                        method = d.getString("method") ?: VerifyMethod.RECEIPT.name,
                        firstVisitedAt = first,
                        lastVisitedAt = d.getTimestamp("lastVisitedAt")?.toDate()?.time ?: first,
                        visitCount = (d.getLong("visitCount") ?: 1L).toInt(),
                        synced = true,
                    ),
                )
                added++
            }
            added
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) null else throw e
        } catch (_: Exception) {
            null
        }
    }

    /** 테스트 가게의 방문 기록을 지운다. 서버에는 올라간 적이 없다 (FR-TST-03) */
    suspend fun clearTestData() = withContext(dispatchers.io) { visitDao.deleteTestVisits() }

    /** 로그아웃 때 이 기기의 사용자 데이터를 지운다 (카탈로그는 유지, FR-AUTH-05). 다른 계정에 이전 기록이 보이면 안 된다. */
    suspend fun clearUserData() {
        visitDao.clear()
        pendingDao.clear()
    }

    private suspend fun commit(uid: String, v: PendingVisit) {
        val firestore = db()
        val seoulMidnight = Date.from(Instant.ofEpochMilli(v.atMillis).atZone(SeoulZone).toLocalDate().atStartOfDay(SeoulZone).toInstant())
        val batch = firestore.batch()
        val visitRef = firestore.collection("users").document(uid).collection("visits").document(v.storeId)
        if (v.firstVisit) {
            batch.set(
                visitRef,
                mapOf(
                    "storeId" to v.storeId, "storeName" to v.storeName, "sigungu" to v.sigungu,
                    "yearsAwarded" to v.years, "grade" to v.grade, "method" to v.method, "photoVerified" to v.photoVerified,
                    "firstVisitedAt" to Timestamp(seoulMidnight), "lastVisitedAt" to Timestamp(seoulMidnight), "visitCount" to 1,
                ),
            )
        } else {
            batch.update(visitRef, mapOf("lastVisitedAt" to Timestamp(seoulMidnight), "visitCount" to v.visitCount))
        }
        v.receiptHash?.let {
            batch.set(firestore.collection("receiptHashes").document(it), mapOf("uid" to uid, "storeId" to v.storeId, "createdAt" to FieldValue.serverTimestamp()))
        }
        batch.update(
            firestore.collection("users").document(uid),
            mapOf(
                "totalYears" to FieldValue.increment(if (v.firstVisit) v.years.toLong() else 0L),
                "visitCount" to FieldValue.increment(1),
                "titleId" to v.titleId,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.commit().await()
    }

    private companion object {
        const val TYPE_VISIT = "VISIT"
        const val SYNC_TIMEOUT_MS = 15_000L
    }
}
