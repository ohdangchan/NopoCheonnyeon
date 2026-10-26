package com.nopo.cheonnyeon.feature.diagnostics

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.verify.PhotoMatcher
import com.nopo.cheonnyeon.domain.verify.ReceiptMatcher
import com.nopo.cheonnyeon.domain.verify.ReceiptParser
import com.nopo.cheonnyeon.platform.ocr.MlKitKoreanRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * 디버그 전용: 앱에 넣어 둔 샘플 영수증·간판 이미지를 실제 ML Kit로 읽고 파서·판정까지 돌려 본다.
 * 카메라 없이도 영수증 글자 인식 경로(OCR → 파싱 → 판정)를 기기에서 확인하는 용도.
 */
@Composable
fun DiagnosticsSampleOcr(modifier: Modifier = Modifier) {
    if (!BuildConfig.DEBUG) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    val store = remember { Store(id = "TEST-SAMPLE", name = "노포천년테스트식당", phone = "020000000", foundedYear = 1980, isTest = true) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
        NopoButton("샘플 영수증 이미지로 글자 인식 시험 (디버그)", {
            result = "읽는 중…"
            scope.launch {
                result = withContext(Dispatchers.Default) { runReceipt(context, store) }
            }
        }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        NopoButton("샘플 간판 이미지로 가게 판정 시험 (디버그)", {
            result = "읽는 중…"
            scope.launch {
                result = withContext(Dispatchers.Default) { runSign(context, store) }
            }
        }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        result?.let { Text(it, style = NopoTheme.typography.bodySmall, color = NopoTheme.colors.ink) }
    }
}

private suspend fun readAsset(context: Context, name: String): List<String> {
    val bitmap = context.assets.open(name).use { BitmapFactory.decodeStream(it) }
    return MlKitKoreanRecognizer().use { it.read(bitmap).lines }.also { bitmap.recycle() }
}

private suspend fun runReceipt(context: Context, store: Store): String = try {
    val lines = readAsset(context, "test_receipt.png")
    val parsed = ReceiptParser.parse(lines.joinToString("\n"))
    // 샘플 영수증의 결제 시각은 고정이라, 판정은 결제 1시간 뒤를 현재로 삼아 24시간 규칙을 건너뛴다
    val now = parsed.paidAt?.plusHours(1)?.atZone(SeoulZone)?.toInstant() ?: Instant.now()
    val match = ReceiptMatcher.match(parsed, store, now)
    "줄 ${lines.size}개\n상호 ${parsed.name}\n전화 ${parsed.phone}\n사업자 ${parsed.businessNo}\n결제 ${parsed.paidAt}\n금액 ${parsed.amount}\n판정 " +
        if (match is AppResult.Success) "통과(전화일치=${match.value.byPhone}, 상호유사도=${"%.2f".format(match.value.nameSimilarity)})" else "실패 ${match.errorOrNull?.code}"
} catch (e: Exception) {
    "오류 ${e.javaClass.simpleName}"
}

private suspend fun runSign(context: Context, store: Store): String = try {
    val lines = readAsset(context, "test_sign.png")
    val r = PhotoMatcher.matchText(lines, store)
    "줄 ${lines.size}개: ${lines.take(6)}\n판정 " + if (r is AppResult.Success) "통과(유사도 ${"%.2f".format(r.value)})" else "실패 ${r.errorOrNull?.code}"
} catch (e: Exception) {
    "오류 ${e.javaClass.simpleName}"
}
