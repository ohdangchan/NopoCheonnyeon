package com.nopo.cheonnyeon.platform.share

/**
 * 공유 카드 QR이 가리키는 앱 내려받기 주소. 배포 방식이 정해지면 이 한 곳만 바꾼다
 * (Firebase App Distribution 초대 링크, Google Play 링크, 또는 APK를 올려 둔 페이지 등).
 * 지금 값은 공개 저장소의 최신 릴리스에 올린 APK(`nopo-cheonnyeon.apk`) 바로 내려받기 주소다(QR을 찍으면 곧바로 내려받아져 설치 화면으로 이어진다).
 */
object ShareConfig {
    const val DOWNLOAD_URL = "https://github.com/ohdangchan/NopoCheonnyeon/releases/latest/download/nopo-cheonnyeon.apk"
}
