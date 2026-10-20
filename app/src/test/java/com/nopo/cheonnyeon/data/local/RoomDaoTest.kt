package com.nopo.cheonnyeon.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nopo.cheonnyeon.domain.model.Store
import kotlinx.coroutines.flow.first
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

/** U-301·U-302: 기기 DB(Room) 규칙을 메모리 DB로 확인한다 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RoomDaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    private fun store(id: String, test: Boolean = false) = StoreEntity.from(Store(id = id, name = "가게$id", isTest = test, lat = 37.5, lng = 127.0))

    @Test fun `U-302-1 같은 가게를 다시 기록하면 세월은 그대로이고 횟수만 오른다`() = runBlocking {
        val dao = db.myVisitDao()
        dao.record("BN-1", 62, "RECEIPT", 1_000L)
        dao.record("BN-1", 99, "PHOTO", 2_000L) // 두 번째 값은 무시된다
        val v = dao.byStore("BN-1")!!
        assertEquals(62, v.yearsAwarded)
        assertEquals(2, v.visitCount)
        assertEquals(1_000L, v.firstVisitedAt)
        assertEquals(2_000L, v.lastVisitedAt)
        assertEquals("RECEIPT", v.method)
    }

    @Test fun `다시 기록하면 동기화 완료 표시가 풀린다`() = runBlocking {
        val dao = db.myVisitDao()
        dao.record("BN-1", 40, "RECEIPT", 1_000L)
        dao.markSynced("BN-1")
        assertTrue(dao.byStore("BN-1")!!.synced)
        dao.record("BN-1", 40, "RECEIPT", 2_000L)
        assertFalse(dao.byStore("BN-1")!!.synced)
    }

    @Test fun `U-306-3 세월 합계는 테스트 가게 기록을 뺀다`() = runBlocking {
        val dao = db.myVisitDao()
        dao.record("BN-1", 40, "RECEIPT", 1_000L)
        dao.record("TEST-NOPO", 50, "RECEIPT", 1_000L)
        assertEquals(40, dao.observeTotalYears().first())
        assertEquals(2, dao.observeAll().first().size)
    }

    @Test fun `테스트 방문만 지울 수 있다`() = runBlocking {
        val dao = db.myVisitDao()
        dao.record("BN-1", 40, "RECEIPT", 1_000L)
        dao.record("TEST-NOPO", 50, "RECEIPT", 1_000L)
        dao.deleteTestVisits()
        assertNull(dao.byStore("TEST-NOPO"))
        assertEquals(40, dao.byStore("BN-1")!!.yearsAwarded)
    }

    @Test fun `삭제는 한 가게만 지우고 clear는 전부 지운다`() = runBlocking {
        val dao = db.myVisitDao()
        dao.record("A", 30, "RECEIPT", 1L)
        dao.record("B", 30, "RECEIPT", 1L)
        dao.delete("A")
        assertEquals(listOf("B"), dao.observeAll().first().map { it.storeId })
        dao.clear()
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test fun `U-301-2 카탈로그를 교체해도 테스트 가게는 남는다`() = runBlocking {
        val dao = db.storeDao()
        dao.insertAll(listOf(store("BN-1"), store("BN-2"), store("TEST-NOPO", test = true)))
        dao.replaceAll(listOf(store("BN-3")))
        assertEquals(setOf("BN-3", "TEST-NOPO"), dao.all().map { it.id }.toSet())
    }

    @Test fun `테스트 가게만 지운다`() = runBlocking {
        val dao = db.storeDao()
        dao.insertAll(listOf(store("BN-1"), store("TEST-NOPO", test = true)))
        dao.deleteTestStores()
        assertEquals(listOf("BN-1"), dao.all().map { it.id })
    }

    @Test fun `영역 안 가게만 고른다`() = runBlocking {
        val dao = db.storeDao()
        dao.insertAll(listOf(StoreEntity.from(Store(id = "IN", name = "안", lat = 37.5, lng = 127.0)), StoreEntity.from(Store(id = "OUT", name = "밖", lat = 35.0, lng = 129.0))))
        assertEquals(listOf("IN"), dao.inBounds(37.0, 38.0, 126.0, 128.0).map { it.id })
    }

    @Test fun `대기열은 넣은 순서대로 나오고 시도 횟수를 올릴 수 있다`() = runBlocking {
        val dao = db.pendingOpDao()
        dao.add(PendingOpEntity(type = "VISIT", payload = "a", createdAt = 1))
        dao.add(PendingOpEntity(type = "VISIT", payload = "b", createdAt = 2))
        val all = dao.all()
        assertEquals(listOf("a", "b"), all.map { it.payload })
        dao.bumpAttempts(all[0].id)
        assertEquals(1, dao.all()[0].attempts)
        dao.remove(all[0].id)
        assertEquals(listOf("b"), dao.all().map { it.payload })
        dao.clear()
        assertTrue(dao.all().isEmpty())
    }
}
