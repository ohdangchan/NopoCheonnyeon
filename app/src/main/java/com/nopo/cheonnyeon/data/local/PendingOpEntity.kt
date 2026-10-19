package com.nopo.cheonnyeon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 서버로 아직 못 보낸 작업(대기열). 앱을 닫아도 남아 있다가 연결되면 SyncWorker가 보낸다. */
@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
)
