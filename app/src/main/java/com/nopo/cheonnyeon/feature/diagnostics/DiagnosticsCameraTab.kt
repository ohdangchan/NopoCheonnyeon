package com.nopo.cheonnyeon.feature.diagnostics

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonSize
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.platform.camera.CameraPreview
import com.nopo.cheonnyeon.platform.camera.rememberCameraController
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.time.TimeSource

/** SPK-2: 미리보기 지연, 메모리 촬영(파일 없음), 파일 촬영(EXIF 반영 썸네일). */
@Composable
fun DiagnosticsCameraTab(modifier: Modifier = Modifier) {
    val permission = rememberPermission(Manifest.permission.CAMERA)
    if (permission.status != PermissionStatus.GRANTED) {
        PermissionGate(permission, stringResource(R.string.perm_camera_needed), modifier)
        return
    }
    val context = LocalContext.current
    val controller = rememberCameraController()
    val scope = rememberCoroutineScope()
    val shown = remember { TimeSource.Monotonic.markNow() }
    var previewMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var resultLine by rememberSaveable { mutableStateOf<String?>(null) }
    var thumb by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }

    val memoryFmt = stringResource(R.string.diag_cam_memory_result)
    val fileFmt = stringResource(R.string.diag_cam_file_result)
    val errorFmt = stringResource(R.string.diag_cam_error)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        CameraPreview(
            controller = controller,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
            onStreaming = { if (previewMs == null) previewMs = shown.elapsedNow().inWholeMilliseconds },
        )
        previewMs?.let {
            Text(stringResource(R.string.diag_cam_preview_ms, it.toInt()), style = NopoTheme.extraTypography.metric, color = NopoTheme.colors.ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            NopoButton(
                stringResource(R.string.diag_cam_memory),
                onClick = {
                    if (busy) return@NopoButton
                    busy = true
                    scope.launch {
                        resultLine = runCatching {
                            withTimeout(10_000) {
                                controller.captureInMemory { img ->
                                    memoryFmt.format(img.width, img.height, img.imageInfo.rotationDegrees)
                                }
                            }
                        }.getOrElse { errorFmt.format(it.javaClass.simpleName) }
                        busy = false
                    }
                },
                size = NopoButtonSize.Medium,
                enabled = !busy,
            )
            NopoButton(
                stringResource(R.string.diag_cam_file),
                onClick = {
                    if (busy) return@NopoButton
                    busy = true
                    scope.launch {
                        val mark = TimeSource.Monotonic.markNow()
                        val file = File(context.filesDir, "diag/diag_${System.currentTimeMillis()}.jpg")
                        resultLine = runCatching {
                            withTimeout(10_000) { controller.captureToFile(file) }
                            val ms = mark.elapsedNow().inWholeMilliseconds
                            thumb = withContext(Dispatchers.IO) { decodeUprightThumb(file, 480) }
                            fileFmt.format(ms.toInt(), file.name)
                        }.getOrElse { errorFmt.format(it.javaClass.simpleName) }
                        busy = false
                    }
                },
                size = NopoButtonSize.Medium,
                style = NopoButtonStyle.Outline,
                enabled = !busy,
            )
        }
        resultLine?.let { Text(it, style = NopoTheme.extraTypography.metric, color = NopoTheme.colors.ink) }
        thumb?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_diag_cam_thumb),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )
        }
    }
}

/** EXIF 방향을 반영해 똑바로 선 작은 썸네일을 만든다. */
private fun decodeUprightThumb(file: File, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val degrees = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bmp
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
        .also { if (it !== bmp) bmp.recycle() }
}
