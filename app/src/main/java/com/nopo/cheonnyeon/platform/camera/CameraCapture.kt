package com.nopo.cheonnyeon.platform.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX 촬영 두 경로 (아키텍처 5.1·5.2).
 * - [captureInMemory]: 영수증용. 파일을 만들지 않고 ImageProxy를 넘긴 뒤 반드시 닫는다.
 * - [captureToFile]: 인증샷용. 앱 내부 저장소에만 쓴다.
 */
class CameraController internal constructor(private val context: Context) {
    internal val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    /** 프레임 변환은 메인 스레드 밖에서 한다 (아키텍처 9장) */
    private val frameExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    /** [block]이 끝나면 프레임을 닫는다. 프레임을 밖으로 내보내지 않는다. [block]은 백그라운드 스레드에서 돈다. */
    suspend fun <T> captureInMemory(block: (ImageProxy) -> T): T = suspendCancellableCoroutine { cont ->
        imageCapture.takePicture(
            frameExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val result = runCatching { image.use(block) }
                    result.fold(cont::resume, cont::resumeWithException)
                }

                override fun onError(exception: ImageCaptureException) = cont.resumeWithException(exception)
            },
        )
    }

    suspend fun captureToFile(file: File): File = suspendCancellableCoroutine { cont ->
        file.parentFile?.mkdirs()
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = cont.resume(file)
                override fun onError(exception: ImageCaptureException) = cont.resumeWithException(exception)
            },
        )
    }
}

@Composable
fun rememberCameraController(): CameraController {
    val context = LocalContext.current
    return remember { CameraController(context.applicationContext) }
}

/**
 * 뒤쪽 카메라 미리보기. 화면 생명주기에 바인딩하고, 화면을 떠나면 unbindAll 한다.
 * [onStreaming]은 첫 프레임이 화면에 나올 때 한 번 호출된다(미리보기 지연 측정용).
 */
@Composable
fun CameraPreview(
    controller: CameraController,
    modifier: Modifier = Modifier,
    onStreaming: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var reported = false
        val observer = androidx.lifecycle.Observer<PreviewView.StreamState> { state ->
            if (state == PreviewView.StreamState.STREAMING && !reported) {
                reported = true
                onStreaming()
            }
        }
        previewView.previewStreamState.observe(lifecycleOwner, observer)
        future.addListener({
            provider = future.get().also { p ->
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, controller.imageCapture)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            previewView.previewStreamState.removeObserver(observer)
            provider?.unbindAll()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

/** 프레임을 똑바로 세운 뒤 긴 변이 [maxSide] 이하가 되게 줄인 비트맵 (OCR 1,600px / 저장 1,080px). */
fun ImageProxy.toUprightBitmap(maxSide: Int): Bitmap {
    val src = toBitmap()
    val scale = (maxSide.toFloat() / maxOf(src.width, src.height)).coerceAtMost(1f)
    val m = Matrix().apply {
        postRotate(imageInfo.rotationDegrees.toFloat())
        postScale(scale, scale)
    }
    val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    if (out !== src) src.recycle()
    return out
}
