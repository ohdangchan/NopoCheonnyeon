package com.nopo.cheonnyeon.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nopo.cheonnyeon.domain.model.Store
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 실제 폰의 SQLite로 DAO 핵심 규칙을 확인한다 (메모리 DB라 앱 데이터는 건드리지 않는다) */
@RunWith(AndroidJUnit4::class)
class RoomOnDeviceTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    @Test fun 같은_가게를_다시_기록하면_세월은_그대로이고_횟수만_오른다() = runBlocking {
        val dao = db.myVisitDao()
        dao.record("BN-1", 62, "RECEIPT", 1_000L)
        dao.markSynced("BN-1")
        dao.record("BN-1", 99, "PHOTO", 2_000L)
        val v = dao.byStore("BN-1")!!
        assertEquals(62, v.yearsAwarded)
        assertEquals(2, v.visitCount)
        assertFalse(v.synced)
    }

    @Test fun 세월_합계는_테스트_가게를_뺀다() = runBlocking {
        val dao = db.myVisitDao()
        dao.record("BN-1", 40, "RECEIPT", 1L)
        dao.record("TEST-NOPO", 50, "RECEIPT", 1L)
        assertEquals(40, dao.observeTotalYears().first())
    }

    @Test fun 카탈로그를_교체해도_테스트_가게는_남고_이야기가_읽힌다() = runBlocking {
        val dao = db.storeDao()
        dao.insertAll(listOf(StoreEntity.from(Store(id = "TEST-NOPO", name = "테스트", isTest = true))))
        dao.replaceAll(listOf(StoreEntity.from(Store(id = "BN-3", name = "새 가게", story = "이야기 한 줄"))))
        assertNotNull(dao.byId("TEST-NOPO"))
        assertEquals("이야기 한 줄", dao.byId("BN-3")!!.toDomain().story)
    }
}
