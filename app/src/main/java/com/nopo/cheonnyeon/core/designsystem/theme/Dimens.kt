package com.nopo.cheonnyeon.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** DESIGN.md 2.3: 8dp 격자 (보조 4dp). */
@Immutable
object NopoSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** 접근성 최소 터치 영역 */
    val minTouch = 48.dp
}

@Immutable
object NopoShapes {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(18.dp)
    val pill = RoundedCornerShape(percent = 50)
}

internal val NopoMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = NopoShapes.sm,
    medium = NopoShapes.md,
    large = NopoShapes.card,
    extraLarge = RoundedCornerShape(26.dp),
)
