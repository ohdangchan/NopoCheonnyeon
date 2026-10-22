package com.nopo.cheonnyeon.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nopo.cheonnyeon.R

/** 업력·세월 숫자와 제목: 시안의 나눔명조 ExtraBold (옛 이름 BlackHanSans 유지) */
val BlackHanSans = FontFamily(Font(R.font.nanum_myeongjo_extrabold, FontWeight.ExtraBold), Font(R.font.nanum_myeongjo_extrabold, FontWeight.Bold), Font(R.font.nanum_myeongjo_extrabold, FontWeight.Normal))

/** 본문: Noto Sans KR TTF(10MB)는 APK 크기 때문에 넣지 않고 시스템 한글 글꼴을 쓴다 (S-102 허용 대체). */
val NopoSans = FontFamily.SansSerif

/** 거리·일시·금액·좌표. 가변 글꼴 하나에서 굵기를 고른다. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val JetBrainsMono = FontFamily(
    Font(
        R.font.jetbrains_mono,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.jetbrains_mono,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

/** DESIGN.md 2.2 */
internal val NopoMaterialTypography = Typography(
    displayLarge = TextStyle(fontFamily = BlackHanSans, fontSize = 54.sp, lineHeight = 1.05.em),
    displayMedium = TextStyle(fontFamily = BlackHanSans, fontSize = 40.sp, lineHeight = 1.1.em),
    displaySmall = TextStyle(fontFamily = BlackHanSans, fontSize = 28.sp, lineHeight = 1.15.em),
    headlineMedium = TextStyle(fontFamily = BlackHanSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 1.3.em),
    titleLarge = TextStyle(fontFamily = BlackHanSans, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 1.3.em),
    titleMedium = TextStyle(fontFamily = BlackHanSans, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 1.35.em),
    titleSmall = TextStyle(fontFamily = NopoSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 1.4.em),
    bodyLarge = TextStyle(fontFamily = NopoSans, fontSize = 16.sp, lineHeight = 1.5.em),
    bodyMedium = TextStyle(fontFamily = NopoSans, fontSize = 14.sp, lineHeight = 1.5.em),
    bodySmall = TextStyle(fontFamily = NopoSans, fontSize = 12.sp, lineHeight = 1.5.em),
    labelLarge = TextStyle(fontFamily = NopoSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 1.4.em),
    labelMedium = TextStyle(fontFamily = NopoSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 1.35.em),
    labelSmall = TextStyle(fontFamily = NopoSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 1.3.em),
)

/** Material 스케일에 없는 노포 전용 스타일. */
@Immutable
data class NopoExtraTypography(
    val metric: TextStyle = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    val metricSmall: TextStyle = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    val stampLabel: TextStyle = TextStyle(fontFamily = NopoSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 1.1.em),
)
