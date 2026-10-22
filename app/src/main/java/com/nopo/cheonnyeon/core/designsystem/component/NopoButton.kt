package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme

enum class NopoButtonStyle { Primary, Stamp, Outline }

enum class NopoButtonSize(val height: Dp) { Large(52.dp), Medium(44.dp) }

/**
 * DESIGN.md 3.3. Medium(44dp)도 Material 최소 터치 영역 48dp가 적용된다.
 * 큰 글씨에서 잘리지 않도록 높이는 최소값으로만 건다. [leadingIcon]은 18dp 슬롯.
 */
@Composable
fun NopoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NopoButtonStyle = NopoButtonStyle.Primary,
    size: NopoButtonSize = NopoButtonSize.Large,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val c = NopoTheme.colors
    val (container, content) = when (style) {
        NopoButtonStyle.Primary -> c.accent to c.onAccent
        NopoButtonStyle.Stamp -> c.stamp to c.onStamp
        NopoButtonStyle.Outline -> c.paper to c.primary
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = size.height)
            .defaultMinSize(minWidth = 88.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = c.line,
            disabledContentColor = c.ink2,
        ),
        border = if (style == NopoButtonStyle.Outline && enabled) BorderStroke(2.dp, c.primary) else null,
        contentPadding = PaddingValues(horizontal = NopoTheme.spacing.xl, vertical = NopoTheme.spacing.sm),
    ) {
        if (leadingIcon != null) {
            Box(Modifier.size(18.dp)) { leadingIcon() }
            Spacer(Modifier.width(NopoTheme.spacing.sm))
        }
        Text(
            text = text,
            style = if (size == NopoButtonSize.Large) NopoTheme.typography.titleSmall else NopoTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@NopoPreviews
@Composable
private fun NopoButtonPreview() = PreviewSurface {
    NopoButton("식객 등록", {})
    NopoButton("방문 인증하기", {}, style = NopoButtonStyle.Stamp)
    NopoButton("길찾기", {}, style = NopoButtonStyle.Outline, size = NopoButtonSize.Medium)
    NopoButton("저장 중", {}, enabled = false)
}
