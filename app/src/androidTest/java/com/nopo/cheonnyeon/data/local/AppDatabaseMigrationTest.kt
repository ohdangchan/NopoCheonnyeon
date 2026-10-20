package com.nopo.cheonnyeon.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** B-13: DB 스키마 v2 → v3(가게 이야기 열 추가)에서 내 방문·대기열이 그대로 남는지 실제 기기 SQLite로 확인한다 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun v2에서_v3으로_올려도_방문과_대기열이_남고_story는_비어_있다() {
        helper.createDatabase(DB, 2).apply {
            execSQL(
                "INSERT INTO stores (id,name,aliases,programs,categoryGroup,isFood,sido,sigungu,address,menuItems,amenities,photoUrls,isTest) " +
                    "VALUES ('BN-1','백년식당','[]','[\"BAEKNYEON\"]','음식점',1,'서울','종로구','주소','[]','[]','[]',0)",
            )
            execSQL("INSERT INTO my_visits (storeId,yearsAwarded,method,firstVisitedAt,lastVisitedAt,visitCount,synced) VALUES ('BN-1',76,'RECEIPT',1000,2000,2,1)")
            execSQL("INSERT INTO pending_ops (type,payload,createdAt,attempts) VALUES ('VISIT','{}',1,3)")
            close()
        }
        val db = helper.runMigrationsAndValidate(DB, 3, true, AppDatabase.MIGRATION_2_3)
        db.query("SELECT story FROM stores WHERE id='BN-1'").use {
            assertTrue(it.moveToFirst())
            assertTrue(it.isNull(0))
        }
        db.query("SELECT yearsAwarded, visitCount, synced FROM my_visits WHERE storeId='BN-1'").use {
            assertTrue(it.moveToFirst())
            assertEquals(76, it.getInt(0))
            assertEquals(2, it.getInt(1))
            assertEquals(1, it.getInt(2))
        }
        db.query("SELECT attempts FROM pending_ops").use {
            assertTrue(it.moveToFirst())
            assertEquals(3, it.getInt(0))
        }
        // 새 열에 이야기를 쓸 수 있다
        db.execSQL("UPDATE stores SET story='이야기' WHERE id='BN-1'")
        db.query("SELECT story FROM stores WHERE id='BN-1'").use {
            it.moveToFirst()
            assertEquals("이야기", it.getString(0))
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
