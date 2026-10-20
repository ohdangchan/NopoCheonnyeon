package com.nopo.cheonnyeon.feature.store

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.platform.camera.CameraPreview
import com.nopo.cheonnyeon.platform.camera.ReviewPhotoCodec
import com.nopo.cheonnyeon.platform.camera.rememberCameraController
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.launch

const val ASSET_PREFIX = "asset:"

/** 리뷰 사진을 그린다. 시연용 예시는 "asset:" 경로, 실제 리뷰는 base64 WebP 문자열이다. */
@Composable
fun ReviewPhotoImage(photo: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val model: Any? = remember(photo) {
        if (photo.startsWith(ASSET_PREFIX)) "file:///android_asset/" + photo.removePrefix(ASSET_PREFIX) else ReviewPhotoCodec.decode(photo)
    }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = stringResource(R.string.cd_review_photo),
        contentScale = contentScale,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize().background(NopoTheme.colors.tealSoft)) },
        error = { Box(Modifier.fillMaxSize().background(NopoTheme.colors.tealSoft)) },
    )
}

/** 리뷰 카드의 사진: 눌러 크게 본다 */
@Composable
fun ReviewPhotoThumb(photo: String, modifier: Modifier = Modifier) {
    var big by remember { mutableStateOf(false) }
    ReviewPhotoImage(photo, modifier.clip(NopoTheme.shapes.md).clickable(role = Role.Image) { big = true })
    if (big) {
        Dialog(onDismissRequest = { big = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { big = false }, contentAlignment = Alignment.Center) {
                ReviewPhotoImage(photo, Modifier.fillMaxWidth(), ContentScale.FillWidth)
            }
        }
    }
}

/**
 * 앱 카메라로 찍는 화면(갤러리 선택 없음). 찍으면 줄여서 [onCaptured]로 넘기고 닫는다.
 * 사진은 파일로 저장하지 않는다.
 */
@Composable
fun ReviewPhotoCapture(onCaptured: (String) -> Unit, onDismiss: () -> Unit) {
    val c = NopoTheme.colors
    val camera = rememberCameraController()
    val permission = rememberPermission(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(
            Modifier.fillMaxSize().background(c.paper).statusBarsPadding().navigationBarsPadding().padding(NopoTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
        ) {
            Text(stringResource(R.string.reviews_photo_title), style = NopoTheme.typography.titleLarge, color = c.ink)
            Text(stringResource(R.string.reviews_photo_hint), style = NopoTheme.typography.bodySmall, color = c.ink2)
            if (permission.status == PermissionStatus.GRANTED) {
                CameraPreview(camera, Modifier.fillMaxWidth().weight(1f).clip(NopoTheme.shapes.md))
                if (failed) Text(stringResource(R.string.reviews_photo_failed), color = c.stamp)
                NopoButton(
                    stringResource(if (busy) R.string.auth_working else R.string.reviews_photo_shoot),
                    {
                        busy = true
                        failed = false
                        scope.launch {
                            val encoded = runCatching { camera.captureInMemory { ReviewPhotoCodec.encode(it) } }.getOrNull()
                            busy = false
                            if (encoded != null) onCaptured(encoded) else failed = true
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(stringResource(R.string.perm_camera_needed), color = c.ink)
                NopoButton(
                    stringResource(if (permission.status == PermissionStatus.PERMANENTLY_DENIED) R.string.perm_open_settings else R.string.perm_request),
                    { if (permission.status == PermissionStatus.PERMANENTLY_DENIED) permission.openSettings() else permission.request() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.weight(1f))
            }
            NopoButton(stringResource(R.string.detail_back), onDismiss, style = NopoButtonStyle.Outline, enabled = !busy, modifier = Modifier.fillMaxWidth())
        }
    }
}
