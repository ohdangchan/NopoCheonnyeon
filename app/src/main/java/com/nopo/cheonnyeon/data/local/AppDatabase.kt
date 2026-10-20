package com.nopo.cheonnyeon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [StoreEntity::class, MyVisitEntity::class, PendingOpEntity::class], version = 2, exportSchema = true)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun myVisitDao(): MyVisitDao
    abstract fun pendingOpDao(): PendingOpDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "nopo.db")// v1은 개발 중에만 있었다(카탈로그는 JSON에서 재적재). v2 이상은 스키마를 바꿀 때 Migration을 추가해야 한다 — 내 방문·대기열이 사라지면 안 된다
                .fallbackToDestructiveMigrationFrom(true, 1)
                .build()
    }
}
