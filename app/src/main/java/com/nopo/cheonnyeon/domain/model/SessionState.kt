package com.nopo.cheonnyeon.domain.model

/** 로그인 게이트 상태 (아키텍처 3장). 내비게이션은 이 값만 보고 그래프를 바꾼다. */
enum class SessionState {
    /** 아직 판단 전. 스플래시를 보여 준다 */
    Loading,
    SignedOut,

    /** 로그인은 됐지만 users/{uid} 프로필이 없음 → 닉네임 정하기로 복구 (FR-AUTH-07) */
    NeedsNickname,
    SignedIn,
}
