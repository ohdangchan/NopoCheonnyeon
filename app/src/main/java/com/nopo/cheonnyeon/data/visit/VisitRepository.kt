package com.nopo.cheonnyeon.data.visit

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.nopo.cheonnyeon.core.common.Clock
import com.nopo.cheonnyeon.core.common.DispatcherProvider
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.data.local.MyVisitDao
import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.data.local.PendingOpDao
import com.nopo.cheonnyeon.data.local.PendingOpEntity
import com.nopo.cheonnyeon.data.remote.DemoReviews
import com.nopo.cheonnyeon.data.remote.DemoVisit
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.model.VerifyMethod
import com.nopo.cheonnyeon.domain.model.VisitRef
import com.nopo.cheonnyeon.domain.rule.StampGradePolicy
import com.nopo.cheonnyeon.domain.rule.SyncRejection
import com.nopo.cheonnyeon.domain.rule.SyncRejectionPolicy
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    val grade: StampGrade,
    val firstVisit: Boolean,
    val totalYearsAfter: Int,
    val titleUp: Title?,
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

@Serializable
private data class PendingStats(val storeId: String, val visitors: Int, val revisitors: Int)

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
    /** 기기 기록과 대기열 추가를 한 트랜잭션으로 묶는다 (앱이 그 사이에 종료돼도 둘이 어긋나지 않게). 기본은 그냥 실행 */
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { it() },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val syncLock = Mutex()
    private val _duplicateNotices = MutableStateFlow<List<String>>(emptyList())

    /** 서버가 같은 영수증을 이미 썼다고 거부해서 기록이 되돌려진 가게 이름들. 화면이 알린 뒤 [dismissDuplicateNotice]로 비운다 */
    val duplicateNotices: StateFlow<List<String>> = _duplicateNotices

    fun dismissDuplicateNotice() { _duplicateNotices.value = emptyList() }

    private fun userDoc(uid: String): DocumentReference = db().collection(USERS).document(uid)

    private fun visitsOf(uid: String): CollectionReference = userDoc(uid).collection(VISITS)

    /** 내 방문 문서의 서버 쪽 방문 횟수(문서가 없으면 0). 확인하지 못하면 null */
    private suspend fun serverVisitCount(user: String, storeId: String): Int? = try {
        withTimeout(SYNC_TIMEOUT_MS) {
            val d = visitsOf(user).document(storeId).get(Source.SERVER).await()
            if (d.exists()) (d.getLong("visitCount") ?: 1L).toInt() else 0
        }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) null else throw e
    } catch (_: Exception) {
        null
    }

    val pendingCount: Flow<Int> get() = pendingDao.observeCount()

    suspend fun visitRefs(): List<VisitRef> = visitDao.observeAll().first().map {
        VisitRef(it.storeId, Instant.ofEpochMilli(it.lastVisitedAt), isTest = it.storeId.startsWith(Store.TEST_PREFIX))
    }

    suspend fun record(store: Store, method: VerifyMethod, photoVerified: Boolean, receiptHash: String?): AwardResult =
        withContext(dispatchers.io) {
            val now = clock.now()
            val year = now.atZone(SeoulZone).year
            val before = visitDao.observeAll().first()
            val totalBefore = before.filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sumOf { it.yearsAwarded }
            // 시연용 패스포트 도장(DEMO)이 있는 가게를 실제로 인증하면 그 도장은 실제 첫 방문으로 바꾼다
            val firstVisit = before.none { it.storeId == store.id && it.method != DEMO_METHOD }
            val years = YearsCalculator.yearsAtVisit(store.foundedYear, year)
            val awarded = if (firstVisit && !store.isTest) years else 0
            val totalAfter = totalBefore + awarded
            val titleUp = TitlePolicy.titleUp(totalBefore, totalAfter)
            lateinit var result: AwardResult
            inTransaction {
                if (before.any { it.storeId == store.id && it.method == DEMO_METHOD }) visitDao.delete(store.id)
                visitDao.record(store.id, years, method.name, now.toEpochMilli())
                val saved = visitDao.byStore(store.id)!!
                val grade = StampGradePolicy.gradeFor(saved.yearsAwarded)
                val op = PendingVisit(
                    storeId = store.id, storeName = store.name, sigungu = store.sigungu, years = saved.yearsAwarded,
                    grade = grade.name, method = method.name, photoVerified = photoVerified, atMillis = now.toEpochMilli(),
                    firstVisit = firstVisit, visitCount = saved.visitCount, receiptHash = receiptHash,
                    titleId = TitlePolicy.titleFor(totalAfter).name,
                )
                // 테스트 가게 기록은 기기에만 남기고 서버로 보내지 않는다 (FR-TST-02)
                if (!store.isTest) {
                    pendingDao.add(PendingOpEntity(type = TYPE_VISIT, payload = json.encodeToString(op), createdAt = now.toEpochMilli()))
                }
                result = AwardResult(store.name, saved.yearsAwarded, grade, firstVisit, totalAfter, titleUp, method, pendingSync = true)
            }
            result
        }

    /** 대기열을 순서대로 서버에 보낸다. 연결이 없으면 남겨 두고 다음 기회에 다시 보낸다. @return 남은 개수 */
    suspend fun sync(): Int = syncLock.withLock {
        val user = uid() ?: return@withLock pendingDao.all().size
        for (op in pendingDao.all()) {
            if (op.type == TYPE_STATS) { retryStatsOp(op); continue }
            val visit = try {
                json.decodeFromString<PendingVisit>(op.payload)
            } catch (_: Exception) {
                pendingDao.remove(op.id)
                continue
            }
            try {
                withTimeout(SYNC_TIMEOUT_MS) { commit(user, visit) }
                pendingDao.remove(op.id)
                visitDao.markSynced(visit.storeId)
                // 집계는 따로 보내고, 실패하면 대기열에 남겨 다시 보낸다 (방문 기록에는 영향 없음)
                statsDelta(visit)?.let { d ->
                    if (!writeStats(d)) {
                        pendingDao.add(PendingOpEntity(
                            type = TYPE_STATS,
                            payload = json.encodeToString(d),
                            createdAt = clock.now().toEpochMilli()
                        ))
                    }
                }
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    pendingDao.bumpAttempts(op.id)
                    break
                } else {
                    throw e
                }
            } catch (e: FirebaseFirestoreException) {
                if (handleRejection(user, op, visit, e)) break
            } catch (_: Exception) {
                pendingDao.bumpAttempts(op.id)
                break
            }
        }
        pendingDao.all().size
    }

    /**
     * 서버가 방문 기록을 받아 주지 않았을 때의 처리. 뒤의 기록까지 멈춰야 하면 true(대기열 처리를 여기서 끝낸다), 건너뛰고 계속이면 false.
     */
    private suspend fun handleRejection(user: String, op: PendingOpEntity, visit: PendingVisit, e: FirebaseFirestoreException): Boolean {
        val denied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        if (denied && visit.receiptHash != null) {
            // 거부됐다. 첫 방문 기록이 이미 서버에 있으면 앞선 전송이 성공했는데 응답만 못 받은 재전송이라 성공으로 본다 (아키텍처 5.3)
            when (SyncRejectionPolicy.decide(visit.firstVisit, visit.visitCount, serverVisitCount(user, visit.storeId))) {
                SyncRejection.RETRY_LATER -> { // 확인하지 못했으니 다음에 다시
                    pendingDao.bumpAttempts(op.id)
                    return true
                }
                SyncRejection.ALREADY_SYNCED -> {
                    pendingDao.remove(op.id)
                    visitDao.markSynced(visit.storeId)
                }
                SyncRejection.DUPLICATE_RECEIPT -> {
                    // 해시 문서가 이미 있음 = 같은 영수증 재사용. 기기 기록을 되돌리고 사용자에게 알린다
                    pendingDao.remove(op.id)
                    if (visit.firstVisit) visitDao.delete(visit.storeId)
                    _duplicateNotices.update { it + visit.storeName }
                }
            }
            return false
        }
        if (denied) {
            // 영수증 해시가 없는 기록이 규칙에 막혔다. 뒤의 기록까지 막지 않도록 건너뛰고, 너무 여러 번 거부되면 대기열에서 뺀다
            // (기기 기록은 남아 "확인 중"으로 보이며, 원인을 고친 뒤 다시 인증하면 된다)
            if (op.attempts >= MAX_DENIED_ATTEMPTS) pendingDao.remove(op.id) else pendingDao.bumpAttempts(op.id)
            return false
        }
        pendingDao.bumpAttempts(op.id)
        return true
    }

    /**
     * 서버의 방문 기록을 기기로 내려받는다 (S-307, FR-AUTH-10). 재설치·다른 기기에서 로그인했을 때 세월과 도장을 되살린다.
     * 기기에 이미 있는 가게는 건드리지 않는다. @return 새로 채운 도장 수, 실패하면 null
     */
    suspend fun restoreFromServer(): Int? {
        val user = uid() ?: return null
        return try {
            val docs = withTimeout(SYNC_TIMEOUT_MS) { visitsOf(user).get().await() }
            var added = 0
            for (d in docs.documents) {
                if (visitDao.byStore(d.id) != null) continue
                val first = d.getTimestamp("firstVisitedAt")?.toDate()?.time ?: continue
                visitDao.put(
                    MyVisitEntity(
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

    /** 시연용 패스포트: 서버에 올리지 않는 도장(synced=true, 방법 DEMO)을 넣는다. 이미 실제 기록이 있는 가게는 건드리지 않는다. */
    suspend fun seedDemoVisits(visits: List<DemoVisit>): Int = withContext(dispatchers.io) {
        val now = clock.now().toEpochMilli()
        var added = 0
        inTransaction {
            for (v in visits) {
                if (visitDao.byStore(v.storeId) != null) continue
                val first = now - v.daysAgo * DAY_MS
                // 마지막 방문일은 방문 횟수에 맞춰 3일 간격으로 늘린다
                val last = first + (v.visitCount - 1) * DEMO_REVISIT_GAP_DAYS * DAY_MS
                visitDao.put(MyVisitEntity(v.storeId, v.years, DEMO_METHOD, first, last, v.visitCount, synced = true))
                added++
            }
        }
        added
    }

    suspend fun clearDemoVisits() = withContext(dispatchers.io) { visitDao.deleteDemoVisits() }

    suspend fun demoVisitCount(): Int = withContext(dispatchers.io) { visitDao.demoVisitCount() }

    /** 테스트 가게의 방문 기록을 지운다. 서버에는 올라간 적이 없다 (FR-TST-03) */
    suspend fun clearTestData() = withContext(dispatchers.io) { visitDao.deleteTestVisits() }

    /** 로그아웃 때 이 기기의 사용자 데이터를 지운다 (카탈로그는 유지, FR-AUTH-05). 다른 계정에 이전 기록이 보이면 안 된다. */
    suspend fun clearUserData() {
        visitDao.clear()
        pendingDao.clear()
    }

    private suspend fun commit(uid: String, v: PendingVisit) {
        val firestore = db()
        val seoulMidnight = Date.from(
            Instant.ofEpochMilli(v.atMillis).atZone(SeoulZone).toLocalDate().atStartOfDay(SeoulZone).toInstant(),
        )
        val batch = firestore.batch()
        val visitRef = visitsOf(uid).document(v.storeId)
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
            batch.set(
                firestore.collection(RECEIPT_HASHES).document(it),
                mapOf("uid" to uid, "storeId" to v.storeId, "createdAt" to FieldValue.serverTimestamp()),
            )
        }
        batch.update(
            firestore.collection(USERS).document(uid),
            mapOf(
                "totalYears" to FieldValue.increment(if (v.firstVisit) v.years.toLong() else 0L),
                "visitCount" to FieldValue.increment(1),
                "titleId" to v.titleId,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.commit().await()
    }

    /**
     * 가게별 방문자·재방문자 집계 증가분 (FR-STORE-07): 첫 방문이면 방문자 +1, 두 번째 인증이면 재방문자 +1, 그 밖은 없음.
     */
    private fun statsDelta(v: PendingVisit): PendingStats? = when {
        v.firstVisit -> PendingStats(v.storeId, visitors = 1, revisitors = 0)
        v.visitCount == 2 -> PendingStats(v.storeId, visitors = 0, revisitors = 1)
        else -> null
    }

    /** 집계를 서버에 보낸다. 방문 기록 전송과 따로 보내며, 성공하면 true */
    private suspend fun writeStats(d: PendingStats): Boolean = try {
        withTimeout(SYNC_TIMEOUT_MS) {
            val increments = mapOf(
                "visitors" to FieldValue.increment(d.visitors.toLong()),
                "revisitors" to FieldValue.increment(d.revisitors.toLong()),
            )
            db().collection(STORE_STATS).document(d.storeId).set(increments, SetOptions.merge()).await()
        }
        true
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) false else throw e
    } catch (_: Exception) {
        false
    }

    /** 방문 기록은 올라갔는데 집계를 못 보냈을 때 남겨 둔 작업을 다시 보낸다 (Q-04). 거듭 실패하면 버린다(집계는 참고용) */
    private suspend fun retryStatsOp(op: PendingOpEntity) {
        val d = try { json.decodeFromString<PendingStats>(op.payload) } catch (_: Exception) { pendingDao.remove(op.id); return }
        when {
            writeStats(d) -> pendingDao.remove(op.id)
            op.attempts >= MAX_DENIED_ATTEMPTS -> pendingDao.remove(op.id)
            else -> pendingDao.bumpAttempts(op.id)
        }
    }

    private companion object {
        const val TYPE_VISIT = "VISIT"
        const val DEMO_METHOD = DemoReviews.METHOD_DEMO
        const val DAY_MS = 86_400_000L
        const val DEMO_REVISIT_GAP_DAYS = 3
        const val TYPE_STATS = "STATS"
        const val USERS = "users"
        const val VISITS = "visits"
        const val RECEIPT_HASHES = "receiptHashes"
        const val STORE_STATS = "storeStats"
        const val SYNC_TIMEOUT_MS = 15_000L
        const val MAX_DENIED_ATTEMPTS = 10
    }
}
