package com.nopo.cheonnyeon.feature.diagnostics

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.platform.camera.CameraController
import com.nopo.cheonnyeon.platform.camera.CameraPreview
import com.nopo.cheonnyeon.platform.camera.rememberCameraController
import com.nopo.cheonnyeon.platform.camera.toUprightBitmap
import com.nopo.cheonnyeon.platform.ocr.MlKitKoreanRecognizer
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.TimeSource

data class DiagOcrState(
    val running: Boolean = false,
    val text: String? = null,
    val lineCount: Int = 0,
    val millis: Long = 0,
    val longSide: Int = 0,
    val phone: String? = null,
    val paidAt: String? = null,
    val error: String? = null,
)

/** SPK-3. 영수증 경로와 같게 메모리 촬영 → 1,600px 축소 → 인식, 이미지는 저장하지 않는다. */
class DiagOcrViewModel : ViewModel() {
    private val recognizer = MlKitKoreanRecognizer()
    private val _state = MutableStateFlow(DiagOcrState())
    val state = _state.asStateFlow()

    fun captureAndRead(camera: CameraController) {
        if (_state.value.running) return
        _state.update { it.copy(running = true, error = null) }
        viewModelScope.launch {
            val next = runCatching {
                val bitmap = withTimeout(10_000) { camera.captureInMemory { it.toUprightBitmap(OCR_MAX_SIDE) } }
                val mark = TimeSource.Monotonic.markNow()
                val result = withContext(Dispatchers.Default) {
                    try {
                        withTimeout(10_000) { recognizer.read(bitmap) }
                    } finally {
                        bitmap.recycle()
                    }
                }
                val text = result.fullText
                DiagOcrState(
                    text = text,
                    lineCount = result.lines.size,
                    millis = mark.elapsedNow().inWholeMilliseconds,
                    longSide = OCR_MAX_SIDE,
                    phone = PHONE.find(text)?.value,
                    paidAt = PAID_AT.find(text)?.value,
                )
            }.getOrElse { DiagOcrState(error = it.javaClass.simpleName) }
            _state.value = next
        }
    }

    override fun onCleared() = recognizer.close()

    private companion object {
        const val OCR_MAX_SIDE = 1_600
        // 진단용 간이 확인. 실제 규칙은 S-207 ReceiptParser(PRD 7.2)
        val PHONE = Regex("""\(?0\d{1,3}\)?[-. ]?\d{3,4}[-. ]?\d{4}""")
        val PAID_AT = Regex("""\d{2,4}[-./]\d{1,2}[-./]\d{1,2}\s*(\([월화수목금토일]\))?\s*\d{1,2}:\d{2}""")
    }
}

@Composable
fun DiagnosticsOcrTab(modifier: Modifier = Modifier, viewModel: DiagOcrViewModel = viewModel()) {
    val permission = rememberPermission(Manifest.permission.CAMERA)
    if (permission.status != PermissionStatus.GRANTED) {
        PermissionGate(permission, stringResource(R.string.perm_camera_needed), modifier)
        return
    }
    val camera = rememberCameraController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    val none = stringResource(R.string.diag_ocr_none)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        CameraPreview(camera, Modifier.fillMaxWidth().aspectRatio(3f / 4f))
        Text(stringResource(R.string.diag_ocr_hint), style = NopoTheme.typography.bodySmall, color = c.ink2)
        NopoButton(
            text = stringResource(if (state.running) R.string.diag_ocr_running else R.string.diag_ocr_capture),
            onClick = { viewModel.captureAndRead(camera) },
            enabled = !state.running,
            modifier = Modifier.fillMaxWidth(),
        )
        DiagnosticsSampleOcr()
        state.error?.let { Text(stringResource(R.string.diag_cam_error, it), color = c.stamp, style = NopoTheme.extraTypography.metric) }
        state.text?.let { text ->
            Text(
                stringResource(R.string.diag_ocr_summary, state.lineCount, state.millis.toInt(), state.longSide),
                style = NopoTheme.extraTypography.metric,
                color = c.ink,
            )
            Text(stringResource(R.string.diag_ocr_phone, state.phone ?: none), style = NopoTheme.typography.bodyMedium, color = c.ink)
            Text(stringResource(R.string.diag_ocr_paid_at, state.paidAt ?: none), style = NopoTheme.typography.bodyMedium, color = c.ink)
            Text(text, style = NopoTheme.typography.bodySmall, color = c.ink2)
        }
    }
}
