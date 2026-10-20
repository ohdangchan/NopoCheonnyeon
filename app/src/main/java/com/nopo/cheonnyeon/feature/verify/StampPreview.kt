package com.nopo.cheonnyeon.feature.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nopo.cheonnyeon.core.designsystem.component.CelebrationOverlay
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.component.StampCeremony
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.model.Title
import kotlinx.coroutines.delay

/**
 * 개발자 도구용: 인증 없이 도장 찍기 → 폭죽 → (선택) 칭호 승급 연출을 순서대로 미리 본다.
 * 발표 영상을 찍거나 연출을 다듬을 때 쓴다. 서버·기록에는 아무것도 남기지 않는다.
 */
@Composable
fun StampPreviewDialog(grade: StampGrade, years: Int, titleUp: Title?, onDismiss: () -> Unit) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    val animate = remember { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    var impacted by remember { mutableStateOf(!animate) }
    var showTitle by remember { mutableStateOf(false) }
    LaunchedEffect(impacted) {
        if (impacted && titleUp != null) {
            delay(if (animate) 2800 else 0)
            showTitle = true
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(c.paper)) {
            CelebrationOverlay(fire = impacted && animate, animate = animate, tier = titleUp?.ordinal ?: 2)
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(NopoTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                StampCeremony(years = years, grade = grade, animate = animate, onImpact = { impacted = true })
                Row(Modifier.fillMaxWidth().padding(top = NopoTheme.spacing.xl)) {
                    NopoButton(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.detail_back), onDismiss, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                }
            }
            if (showTitle && titleUp != null) TitleUpDialog(titleUp, titleUp.requiredYears, preview = true) { showTitle = false }
        }
    }
}
