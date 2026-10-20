package com.nopo.cheonnyeon.domain.rule

enum class SyncRejection {
    /** 서버를 확인하지 못했다. 대기열에 남기고 다음에 다시 보낸다 */
    RETRY_LATER,

    /** 앞선 전송이 이미 성공했다(응답만 못 받음). 대기열에서 지우고 동기화 완료로 본다 */
    ALREADY_SYNCED,

    /** 같은 영수증 재사용. 기기 기록을 되돌리고 사용자에게 알린다 */
    DUPLICATE_RECEIPT,
}

/**
 * 서버가 영수증 인증 기록을 거부(PERMISSION_DENIED)했을 때의 판정 (아키텍처 5.3).
 * 첫 방문이라면 서버에 방문 문서가 이미 있을 때 이전 전송이 성공한 것이다. 재방문은 서버에서 구분할 방법이 없어 중복으로 본다.
 * @param visitOnServer 첫 방문일 때 서버에 방문 문서가 있는지. 확인하지 못하면 null
 */
object SyncRejectionPolicy {
    fun decide(firstVisit: Boolean, visitOnServer: Boolean?): SyncRejection = when {
        !firstVisit -> SyncRejection.DUPLICATE_RECEIPT
        visitOnServer == null -> SyncRejection.RETRY_LATER
        visitOnServer -> SyncRejection.ALREADY_SYNCED
        else -> SyncRejection.DUPLICATE_RECEIPT
    }
}
