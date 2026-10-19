package com.nopo.cheonnyeon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    /** 카탈로그 교체: 한 트랜잭션이라 중간에 실패하면 이전 데이터가 그대로 남는다 */
    @Transaction
    suspend fun replaceAll(stores: List<StoreEntity>) {
        deleteCatalog()
        insertAll(stores)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(stores: List<StoreEntity>)

    @Query("DELETE FROM stores WHERE isTest = 0") suspend fun deleteCatalog()

    @Query("SELECT COUNT(*) FROM stores") suspend fun count(): Int

    @Query("SELECT * FROM stores WHERE id = :id") suspend fun byId(id: String): StoreEntity?

    @Query("SELECT * FROM stores WHERE lat BETWEEN :south AND :north AND lng BETWEEN :west AND :east")
    suspend fun inBounds(south: Double, north: Double, west: Double, east: Double): List<StoreEntity>

    @Query("SELECT * FROM stores") suspend fun all(): List<StoreEntity>
}

@Dao
interface MyVisitDao {
    @Query("SELECT * FROM my_visits WHERE storeId = :storeId") suspend fun byStore(storeId: String): MyVisitEntity?

    @Query("SELECT * FROM my_visits ORDER BY lastVisitedAt DESC") fun observeAll(): Flow<List<MyVisitEntity>>

    @Query("SELECT COALESCE(SUM(yearsAwarded), 0) FROM my_visits WHERE storeId NOT LIKE 'TEST-%'") fun observeTotalYears(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(visit: MyVisitEntity)

    @Query("UPDATE my_visits SET synced = 1 WHERE storeId = :storeId") suspend fun markSynced(storeId: String)

    @Query("DELETE FROM my_visits") suspend fun clear()

    @Query("DELETE FROM my_visits WHERE storeId = :storeId") suspend fun delete(storeId: String)

    /** 처음이면 새로 넣고, 이미 있으면 세월은 그대로 두고 횟수·마지막 시각만 갱신한다. */
    @Transaction
    suspend fun record(storeId: String, years: Int, method: String, at: Long) {
        val old = byStore(storeId)
        put(
            if (old == null) MyVisitEntity(storeId, years, method, at, at)
            else old.copy(lastVisitedAt = at, visitCount = old.visitCount + 1, synced = false),
        )
    }
}

@Dao
interface PendingOpDao {
    @Insert suspend fun add(op: PendingOpEntity): Long

    @Query("SELECT * FROM pending_ops ORDER BY id") suspend fun all(): List<PendingOpEntity>

    @Query("SELECT COUNT(*) FROM pending_ops") fun observeCount(): Flow<Int>

    @Query("DELETE FROM pending_ops WHERE id = :id") suspend fun remove(id: Long)

    @Query("UPDATE pending_ops SET attempts = attempts + 1 WHERE id = :id") suspend fun bumpAttempts(id: Long)

    @Query("DELETE FROM pending_ops") suspend fun clear()
}
