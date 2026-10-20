package com.nopo.cheonnyeon.platform.share

/**
 * 공유 카드 QR이 가리키는 앱 내려받기 주소. 배포 방식이 정해지면 이 한 곳만 바꾼다
 * (Firebase App Distribution 초대 링크, Google Play 링크, 또는 APK를 올려 둔 페이지 등).
 * 기본값은 저장소 릴리스 페이지라서, 저장소가 비공개인 동안은 권한 있는 사람만 열 수 있다.
 */
object ShareConfig {
    const val DOWNLOAD_URL = "https://github.com/ohdangchan/NopoCheonnyeon/releases/latest"
}
