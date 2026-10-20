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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.domain.rule.ReviewRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/** 리뷰 카드의 사진 줄: 최대 3장을 나란히, 누르면 크게 본다 */
@Composable
fun ReviewPhotoRow(photos: List<String>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
        photos.take(ReviewRules.MAX_PHOTOS).forEach { ReviewPhotoThumb(it, Modifier.weight(1f).height(120.dp)) }
        repeat(ReviewRules.MAX_PHOTOS - photos.size.coerceAtMost(ReviewRules.MAX_PHOTOS)) { Box(Modifier.weight(1f)) }
    }
}

/**
 * 리뷰 편집기의 사진 칸: 최대 3장, 카메라로 찍거나 앨범(시스템 사진 선택기, 별도 저장소 권한 없음)에서 고른다.
 * 고른 사진은 줄여서 문자열로만 들고 있고 파일로 남기지 않는다.
 */
@Composable
fun ReviewPhotoPicker(photos: List<String>, onChange: (List<String>) -> Unit, onCamera: () -> Unit, enabled: Boolean) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(photos)
    var failed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(ReviewRules.MAX_PHOTOS)) { uris ->
        val room = ReviewRules.MAX_PHOTOS - latest.size
        val chosen = uris.take(room)
        if (chosen.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val added = withContext(Dispatchers.Default) { chosen.mapNotNull { ReviewPhotoCodec.fromUri(context, it) } }
            failed = added.size < chosen.size
            onChange((latest + added).take(ReviewRules.MAX_PHOTOS))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
        if (photos.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                photos.forEachIndexed { i, p ->
                    val removeLabel = stringResource(R.string.reviews_photo_remove)
                    Box(Modifier.size(84.dp)) {
                        ReviewPhotoImage(p, Modifier.fillMaxSize().clip(NopoTheme.shapes.sm))
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(2.dp).size(26.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .clickable(enabled = enabled, role = Role.Button) { onChange(photos.filterIndexed { k, _ -> k != i }) }
                                .semantics { contentDescription = removeLabel },
                            contentAlignment = Alignment.Center,
                        ) { Text("✕", color = Color.White, style = NopoTheme.typography.labelSmall) }
                    }
                }
            }
        }
        Text(stringResource(R.string.reviews_photo_count, photos.size, ReviewRules.MAX_PHOTOS), style = NopoTheme.typography.bodySmall, color = c.ink2)
        val full = photos.size >= ReviewRules.MAX_PHOTOS
        Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            NopoButton(stringResource(R.string.reviews_photo_camera), onCamera, style = NopoButtonStyle.Outline, enabled = enabled && !full, modifier = Modifier.weight(1f))
            NopoButton(
                stringResource(R.string.reviews_photo_gallery),
                { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                style = NopoButtonStyle.Outline, enabled = enabled && !full, modifier = Modifier.weight(1f),
            )
        }
        if (failed) Text(stringResource(R.string.reviews_photo_failed), style = NopoTheme.typography.bodySmall, color = c.stamp)
    }
}
