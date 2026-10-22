package com.nopo.cheonnyeon.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** 디자인 토큰. 노포 패스포트 시안(design/redesign): 크림 종이, 남색 글자, 연두 강조. 화면 코드는 이 값만 쓴다. */
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
    /** 새 디자인(노포 패스포트)의 연두 강조색: 주 버튼·선택 칩·진행 바 */
    val accent: Color,
    val onAccent: Color,
    /** 영수증 스캔 연출: 형광펜 칠, 빨간 레이저 선 */
    val highlighter: Color,
    val laser: Color,
    val isDark: Boolean,
)

val LightNopoColors = NopoColors(
    sign = Color(0xFF1B2A4A),
    primary = Color(0xFF22365F),
    stamp = Color(0xFFC23B2A),
    brass = Color(0xFFC9962B),
    paper = Color(0xFFF8F3E6),
    surface = Color(0xFFFFFBF0),
    ink = Color(0xFF14233B),
    ink2 = Color(0xFF5A6577),
    line = Color(0xFFE3DAC3),
    tealSoft = Color(0xFFECE5D0),
    stampSoft = Color(0xFFF4DCD6),
    brassSoft = Color(0xFFF6E8C4),
    onSign = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onStamp = Color(0xFFFFFFFF),
    onBrass = Color(0xFF2A1F00),
    accent = Color(0xFFA8E32E),
    onAccent = Color(0xFF14233B),
    highlighter = Color(0xFFFFEB3B),
    laser = Color(0xFFFF2B2B),
    isDark = false,
)

val DarkNopoColors = NopoColors(
    sign = Color(0xFF0B1424),
    primary = Color(0xFFB9CCF0),
    stamp = Color(0xFFE8664F),
    brass = Color(0xFFE4B64A),
    paper = Color(0xFF101A2C),
    surface = Color(0xFF17233A),
    ink = Color(0xFFF2EEDF),
    ink2 = Color(0xFFA3AEC2),
    line = Color(0xFF2B3A57),
    tealSoft = Color(0xFF22304A),
    stampSoft = Color(0xFF3D2420),
    brassSoft = Color(0xFF3A3119),
    onSign = Color(0xFFF2EEDF),
    onPrimary = Color(0xFF101A2C),
    onStamp = Color(0xFF101A2C),
    onBrass = Color(0xFF2A1F00),
    accent = Color(0xFFA8E32E),
    onAccent = Color(0xFF14233B),
    highlighter = Color(0xFFFFEB3B),
    laser = Color(0xFFFF4D4D),
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
