package com.nopo.cheonnyeon.domain.model

import java.time.Instant

/** 하루 한도 판정에 쓰는 방문 기록 최소 정보. 테스트 가게 기록은 [isTest]로 구분한다. */
data class VisitRef(val storeId: String, val at: Instant, val isTest: Boolean)
