package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRejectionPolicyTest {
    @Test fun `U-306-1 첫 방문이고 서버에 방문 문서가 있으면 이미 올라간 것`() =
        assertEquals(SyncRejection.ALREADY_SYNCED, SyncRejectionPolicy.decide(firstVisit = true, visitOnServer = true))

    @Test fun `U-306-1 첫 방문이고 서버에 방문 문서가 없으면 중복 영수증`() =
        assertEquals(SyncRejection.DUPLICATE_RECEIPT, SyncRejectionPolicy.decide(firstVisit = true, visitOnServer = false))

    @Test fun `서버를 확인하지 못하면 나중에 다시`() =
        assertEquals(SyncRejection.RETRY_LATER, SyncRejectionPolicy.decide(firstVisit = true, visitOnServer = null))

    @Test fun `재방문 거부는 중복 영수증으로 본다`() {
        assertEquals(SyncRejection.DUPLICATE_RECEIPT, SyncRejectionPolicy.decide(firstVisit = false, visitOnServer = null))
        assertEquals(SyncRejection.DUPLICATE_RECEIPT, SyncRejectionPolicy.decide(firstVisit = false, visitOnServer = true))
    }
}
