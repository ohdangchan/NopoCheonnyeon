package com.nopo.cheonnyeon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [StoreEntity::class, MyVisitEntity::class, PendingOpEntity::class], version = 2, exportSchema = false)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun myVisitDao(): MyVisitDao
    abstract fun pendingOpDao(): PendingOpDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "nopo.db")// 개발 중에는 스키마가 바뀌면 다시 만든다(카탈로그는 JSON에서 재적재). 출시 전에 정식 마이그레이션으로 교체할 것
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
