package com.nopo.cheonnyeon

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 앱이 실제 폰에서 시작되고 첫 화면(로그인 상태에 따라 지도 탭 또는 로그인·온보딩)이 그려지는지 확인한다 */
@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test fun 첫_화면이_그려진다() {
        rule.waitUntil(timeoutMillis = 15_000) {
            listOf("지도 탐색", "로그인", "시작하기", "건너뛰기", "닉네임을 정해 주세요").any {
                rule.onAllNodesWithTextCount(it) > 0
            }
        }
        assertTrue(true)
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithTextCount(text: String): Int =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
}
