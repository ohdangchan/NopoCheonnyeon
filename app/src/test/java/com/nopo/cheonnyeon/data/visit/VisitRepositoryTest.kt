package com.nopo.cheonnyeon.data.visit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nopo.cheonnyeon.data.local.AppDatabase
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.model.VerifyMethod
import com.nopo.cheonnyeon.testutil.FixedClock
import com.nopo.cheonnyeon.testutil.UnconfinedDispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S-302·S-306: 인증을 기기에 기록하는 흐름 (서버는 쓰지 않는다). 방문 연도는 2026(서울). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class VisitRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: VisitRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        repo = newRepo(uid = "me")
    }

    @After fun tearDown() = db.close()

    private fun newRepo(uid: String?) =
        VisitRepository(db.myVisitDao(), db.pendingOpDao(), { error("이 시험은 서버를 쓰지 않는다") }, { uid }, FixedClock(), UnconfinedDispatchers)

    private fun store(id: String, founded: Int?, test: Boolean = false) = Store(id = id, name = "가게$id", sigungu = "종로구", foundedYear = founded, isTest = test)

    @Test fun `첫 인증은 업력만큼 세월을 얻고 대기열에 들어간다`() = runBlocking {
        val r = repo.record(store("BN-1", 1990), VerifyMethod.RECEIPT, photoVerified = true, receiptHash = "h1")
        assertEquals(36, r.years)
        assertEquals(StampGrade.NORMAL, r.grade)
        assertTrue(r.firstVisit)
        assertEquals(36, r.totalYearsAfter)
        assertNull(r.titleUp)
        assertTrue(r.pendingSync)
        assertEquals(1, db.pendingOpDao().all().size)
        assertEquals(36, db.myVisitDao().byStore("BN-1")!!.yearsAwarded)
    }

    @Test fun `100년 넘은 가게는 금빛 도장과 칭호 승급이 나온다`() = runBlocking {
        val r = repo.record(store("BN-1", 1900), VerifyMethod.PHOTO, photoVerified = true, receiptHash = null)
        assertEquals(126, r.years)
        assertEquals(StampGrade.GOLD, r.grade)
        assertEquals(Title.REGULAR, r.titleUp)
    }

    @Test fun `창업연도를 모르면 30년`() = runBlocking {
        assertEquals(30, repo.record(store("BN-1", null), VerifyMethod.RECEIPT, true, "h").years)
    }

    @Test fun `U-302-1 같은 가게를 다시 인증하면 세월은 그대로 두고 대기열만 늘어난다`() = runBlocking {
        repo.record(store("BN-1", 1990), VerifyMethod.RECEIPT, true, "h1")
        val second = repo.record(store("BN-1", 1990), VerifyMethod.RECEIPT, true, "h2")
        assertFalse(second.firstVisit)
        assertEquals(36, second.totalYearsAfter)
        assertEquals(2, db.myVisitDao().byStore("BN-1")!!.visitCount)
        assertEquals(2, db.pendingOpDao().all().size)
    }

    @Test fun `U-306-2 테스트 가게 기록은 대기열에 넣지 않고 세월에도 넣지 않는다`() = runBlocking {
        val r = repo.record(store("TEST-NOPO", 1980, test = true), VerifyMethod.RECEIPT, true, "h")
        assertEquals(0, r.totalYearsAfter)
        assertTrue(db.pendingOpDao().all().isEmpty())
        assertEquals(1, db.myVisitDao().byStore("TEST-NOPO")!!.visitCount)
    }

    @Test fun `칭호 경계를 두 번째 인증으로 넘으면 승급`() = runBlocking {
        repo.record(store("A", 1930), VerifyMethod.RECEIPT, true, "a") // 96
        val r = repo.record(store("B", 1990), VerifyMethod.RECEIPT, true, "b") // +36 = 132
        assertEquals(Title.REGULAR, r.titleUp)
        assertEquals(132, r.totalYearsAfter)
    }

    @Test fun `로그인 정보가 없으면 동기화는 아무것도 보내지 않고 남은 개수를 돌려준다`() = runBlocking {
        repo.record(store("BN-1", 1990), VerifyMethod.RECEIPT, true, "h")
        assertEquals(1, newRepo(uid = null).sync())
    }

    @Test fun `테스트 기록 삭제와 사용자 데이터 삭제`() = runBlocking {
        repo.record(store("BN-1", 1990), VerifyMethod.RECEIPT, true, "h")
        repo.record(store("TEST-NOPO", 1980, test = true), VerifyMethod.RECEIPT, true, "t")
        repo.clearTestData()
        assertNull(db.myVisitDao().byStore("TEST-NOPO"))
        assertEquals(36, db.myVisitDao().byStore("BN-1")!!.yearsAwarded)
        repo.clearUserData()
        assertNull(db.myVisitDao().byStore("BN-1"))
        assertTrue(db.pendingOpDao().all().isEmpty())
    }

    @Test fun `오늘 인증 목록 기준 방문 참조에서 테스트 가게는 구분된다`() = runBlocking {
        repo.record(store("BN-1", 1990), VerifyMethod.RECEIPT, true, "h")
        repo.record(store("TEST-NOPO", 1980, test = true), VerifyMethod.RECEIPT, true, "t")
        val refs = repo.visitRefs().associateBy { it.storeId }
        assertFalse(refs.getValue("BN-1").isTest)
        assertTrue(refs.getValue("TEST-NOPO").isTest)
    }

    @Test fun `중복 영수증 알림은 처음에 비어 있고 닫으면 비워진다`() {
        assertTrue(repo.duplicateNotices.value.isEmpty())
        repo.dismissDuplicateNotice()
        assertTrue(repo.duplicateNotices.value.isEmpty())
    }
}
