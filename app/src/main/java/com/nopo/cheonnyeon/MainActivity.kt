package com.nopo.cheonnyeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.enableEdgeToEdge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.core.navigation.NopoNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 시안은 크림 종이 한 가지 모습이라 시스템 다크 모드와 무관하게 상태·내비게이션 바 아이콘을 어둡게 둔다
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val container = (application as NopoApp).container
        setContent {
            NopoTheme {
                // 시안 좌표(폭 760)를 쓰는 모든 화면이 같은 배율을 쓰도록 맨 위에서 한 번 준다
                com.nopo.cheonnyeon.core.designsystem.mock.ProvideMockScale {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize().background(com.nopo.cheonnyeon.core.designsystem.mock.MockColors.Cream),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(Modifier.widthIn(max = com.nopo.cheonnyeon.core.designsystem.mock.MOCK_MAX_WIDTH_DP.dp).fillMaxHeight()) {
                            NopoNavHost(container)
                        }
                    }
                }
            }
        }
    }
}
