package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRejectionPolicyTest {
    @Test fun `U-306-1 첫 방문이고 서버에 방문 문서가 있으면 이미 올라간 것`() =
        assertEquals(SyncRejection.ALREADY_SYNCED, SyncRejectionPolicy.decide(firstVisit = true, localVisitCount = 1, serverVisitCount = 1))

    @Test fun `U-306-1 첫 방문이고 서버에 방문 문서가 없으면 중복 영수증`() =
        assertEquals(SyncRejection.DUPLICATE_RECEIPT, SyncRejectionPolicy.decide(firstVisit = true, localVisitCount = 1, serverVisitCount = 0))

    @Test fun `서버를 확인하지 못하면 나중에 다시`() {
        assertEquals(SyncRejection.RETRY_LATER, SyncRejectionPolicy.decide(true, 1, null))
        assertEquals(SyncRejection.RETRY_LATER, SyncRejectionPolicy.decide(false, 2, null))
    }

    @Test fun `재방문은 서버 횟수가 기기 횟수에 이르렀으면 이미 올라간 것`() {
        assertEquals(SyncRejection.ALREADY_SYNCED, SyncRejectionPolicy.decide(firstVisit = false, localVisitCount = 2, serverVisitCount = 2))
        assertEquals(SyncRejection.ALREADY_SYNCED, SyncRejectionPolicy.decide(firstVisit = false, localVisitCount = 2, serverVisitCount = 3))
    }

    @Test fun `재방문인데 서버 횟수가 모자라면 중복 영수증`() =
        assertEquals(SyncRejection.DUPLICATE_RECEIPT, SyncRejectionPolicy.decide(firstVisit = false, localVisitCount = 3, serverVisitCount = 2))
}
