package com.nopo.cheonnyeon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 내가 인증한 가게 하나당 한 줄. 세월(yearsAwarded)은 첫 인증 때 정해지고 다시 인증해도 바뀌지 않는다 (FR-STP-01). */
@Entity(tableName = "my_visits")
data class MyVisitEntity(
    @PrimaryKey val storeId: String,
    val yearsAwarded: Int,
    val method: String,
    /** 첫 인증 시각 (epoch ms) */
    val firstVisitedAt: Long,
    val lastVisitedAt: Long,
    val visitCount: Int = 1,
    val synced: Boolean = false,
)
