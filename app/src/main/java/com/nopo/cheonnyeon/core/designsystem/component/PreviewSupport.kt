package com.nopo.cheonnyeon.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme

/** 라이트·다크·큰 글씨(PRD 10.2) 세 가지로 한 번에 미리 본다. */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Font 1.5x", showBackground = true, fontScale = 1.5f)
annotation class NopoPreviews

@Composable
internal fun PreviewSurface(content: @Composable ColumnScope.() -> Unit) {
    NopoTheme {
        Surface(color = NopoTheme.colors.paper) {
            Column(
                modifier = Modifier.padding(NopoTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
                content = content,
            )
        }
    }
}
