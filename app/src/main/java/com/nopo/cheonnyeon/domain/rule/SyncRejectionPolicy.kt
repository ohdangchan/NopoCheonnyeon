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
 * 서버의 내 방문 문서 횟수가 이 기록이 만든 횟수 이상이면,
 * 이전 전송이 성공했는데 응답만 못 받은 재전송이다.
 * 첫 방문은 횟수 1, 재방문은 기기가 센 방문 횟수([localVisitCount])가 기준이다.
 * @param serverVisitCount 서버 방문 문서의 횟수(문서가 없으면 0). 확인하지 못하면 null
 */
object SyncRejectionPolicy {
    fun decide(firstVisit: Boolean, localVisitCount: Int, serverVisitCount: Int?): SyncRejection = when {
        serverVisitCount == null -> SyncRejection.RETRY_LATER
        serverVisitCount >= (if (firstVisit) 1 else localVisitCount) -> SyncRejection.ALREADY_SYNCED
        else -> SyncRejection.DUPLICATE_RECEIPT
    }
}
