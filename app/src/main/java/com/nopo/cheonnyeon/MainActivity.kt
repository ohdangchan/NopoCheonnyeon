package com.nopo.cheonnyeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.feature.diagnostics.DiagnosticsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NopoTheme {
                // 1일차 기능 검증 동안은 진단 화면만. S-105에서 NopoNavHost로 교체
                DiagnosticsScreen()
            }
        }
    }
}
