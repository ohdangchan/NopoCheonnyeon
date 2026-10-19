package com.nopo.cheonnyeon.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** 디자인 토큰 (PRD 10.1, design/stitch DESIGN.md 2.1). 화면 코드는 이 값만 쓴다. */
@Immutable
data class NopoColors(
    val sign: Color,
    val primary: Color,
    val stamp: Color,
    val brass: Color,
    val paper: Color,
    val surface: Color,
    val ink: Color,
    val ink2: Color,
    val line: Color,
    val tealSoft: Color,
    val stampSoft: Color,
    val brassSoft: Color,
    val onSign: Color,
    val onPrimary: Color,
    val onStamp: Color,
    val onBrass: Color,
    val isDark: Boolean,
)

val LightNopoColors = NopoColors(
    sign = Color(0xFF17504A),
    primary = Color(0xFF1F6B63),
    stamp = Color(0xFFC23B2A),
    brass = Color(0xFFD9A22C),
    paper = Color(0xFFEDF0EA),
    surface = Color(0xFFF8F9F5),
    ink = Color(0xFF14201E),
    ink2 = Color(0xFF52625E),
    line = Color(0xFFCCD4CC),
    tealSoft = Color(0xFFD3E5E0),
    stampSoft = Color(0xFFF4DCD6),
    brassSoft = Color(0xFFF6E8C4),
    onSign = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onStamp = Color(0xFFFFFFFF),
    onBrass = Color(0xFF2A1F00),
    isDark = false,
)

val DarkNopoColors = NopoColors(
    sign = Color(0xFF0F2E2A),
    primary = Color(0xFF5DB3A6),
    stamp = Color(0xFFE8664F),
    brass = Color(0xFFE4B64A),
    paper = Color(0xFF0D1614),
    surface = Color(0xFF15221F),
    ink = Color(0xFFE7EEEA),
    ink2 = Color(0xFF9CB0AA),
    line = Color(0xFF2A3C37),
    tealSoft = Color(0xFF1B3833),
    stampSoft = Color(0xFF3D2420),
    brassSoft = Color(0xFF3A3119),
    onSign = Color(0xFFE7EEEA),
    onPrimary = Color(0xFF0D1614),
    onStamp = Color(0xFF0D1614),
    onBrass = Color(0xFF2A1F00),
    isDark = true,
)

/** Material3 컴포넌트(TextField, Dialog 등)가 토큰 색을 따르도록 연결. */
internal fun NopoColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = tealSoft,
        onPrimaryContainer = ink,
        secondary = stamp,
        onSecondary = onStamp,
        secondaryContainer = stampSoft,
        onSecondaryContainer = ink,
        tertiary = brass,
        onTertiary = onBrass,
        tertiaryContainer = brassSoft,
        onTertiaryContainer = ink,
        background = paper,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = tealSoft,
        onSurfaceVariant = ink2,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = tealSoft,
        outline = ink2,
        outlineVariant = line,
        error = stamp,
        onError = onStamp,
        errorContainer = stampSoft,
        onErrorContainer = ink,
    )
}
