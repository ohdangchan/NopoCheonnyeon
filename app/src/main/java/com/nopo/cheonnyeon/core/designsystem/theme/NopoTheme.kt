package com.nopo.cheonnyeon.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalNopoColors = staticCompositionLocalOf { LightNopoColors }
private val LocalNopoExtraTypography = staticCompositionLocalOf { NopoExtraTypography() }

/** 브랜드 색이 정체성이라 다이내믹 컬러(배경화면 색)는 쓰지 않는다. */
@Composable
fun NopoTheme(
    darkTheme: Boolean = false, // 시안은 크림 종이 한 가지 모습이라 시스템 다크 모드를 따르지 않는다 (v2 결정)
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkNopoColors else LightNopoColors
    CompositionLocalProvider(
        LocalNopoColors provides colors,
        LocalNopoExtraTypography provides NopoExtraTypography(),
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = NopoMaterialTypography,
            shapes = NopoMaterialShapes,
            content = content,
        )
    }
}

object NopoTheme {
    val colors: NopoColors
        @Composable @ReadOnlyComposable get() = LocalNopoColors.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val extraTypography: NopoExtraTypography
        @Composable @ReadOnlyComposable get() = LocalNopoExtraTypography.current

    val spacing = NopoSpacing
    val shapes = NopoShapes
}
