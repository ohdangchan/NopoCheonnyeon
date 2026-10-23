package com.nopo.cheonnyeon.feature.verify

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.labelRes
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.component.StampView
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.platform.camera.CameraPreview
import com.nopo.cheonnyeon.platform.camera.rememberCameraController
import com.nopo.cheonnyeon.platform.permission.PermissionHandle
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission

private fun titleLabel(t: com.nopo.cheonnyeon.domain.model.Title): Int = t.labelRes()

internal fun animationsEnabled(context: android.content.Context): Boolean =
    android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

@Suppress("DEPRECATION")
internal fun vibrateOnce(context: android.content.Context) {
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
    } else {
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }
    vibrator.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
}
