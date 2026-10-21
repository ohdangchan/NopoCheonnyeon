package com.nopo.cheonnyeon.feature.verify

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.data.visit.AwardResult
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.VerifyMethod
import com.nopo.cheonnyeon.domain.rule.DailyLimitPolicy
import com.nopo.cheonnyeon.domain.rule.VerificationConfig
import com.nopo.cheonnyeon.domain.verify.PhotoMatcher
import com.nopo.cheonnyeon.domain.verify.VerifyReceiptUseCase
import com.nopo.cheonnyeon.platform.camera.CameraController
import com.nopo.cheonnyeon.platform.camera.toUprightBitmap
import com.nopo.cheonnyeon.platform.location.Fix
import com.nopo.cheonnyeon.platform.location.FusedLocationAdapter
import com.nopo.cheonnyeon.platform.ocr.MlKitKoreanRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class VerifyStep { RECEIPT, LOCATE, PHOTO, DONE }

data class VerifyUiState(
    val store: Store? = null,
    val step: VerifyStep = VerifyStep.RECEIPT,
    val busy: Boolean = false,
    val error: AppError? = null,
    /** E_FAR·E_ACCURACY 문구에 넣는 미터 값 */
    val errorMeters: Int = 0,
    val failedPhotoAttempts: Int = 0,
    val withinRange: Boolean = false,
    val locatingBest: Double? = null,
    val award: AwardResult? = null,
    /** 하루 한도 등으로 인증 자체를 시작할 수 없음 */
    val blocked: Boolean = false,
    /** 기기 위치가 꺼져 있어 시스템 '위치 켜기' 대화상자를 띄워야 함 (S-605) */
    val pendingResolution: com.google.android.gms.common.api.ResolvableApiException? = null,
    /** 한도 안내 화면에 보여 줄 오늘 인증한 가게 (이름, 얻은 세월) */
    val todayVisits: List<Pair<String, Int>> = emptyList(),
    /** 기기 시계가 서버와 10분 넘게 어긋남 (영수증 24시간 판정이 틀릴 수 있다). 어긋난 분 */
    val clockSkewMinutes: Long? = null,
    /** 영수증 스캔 연출: 레이저가 훑는 중 → 인식한 글자에 형광펜 */
    val scanPhase: ScanPhase = ScanPhase.NONE,
    /** 방금 찍은 영수증 장면(화면 표시용으로 줄인 것, 메모리에서만 쓰고 파일로 저장하지 않는다) */
    val scanFrame: android.graphics.Bitmap? = null,
    val scanBoxes: List<com.nopo.cheonnyeon.platform.ocr.OcrBox> = emptyList(),
    /** AI 촬영 도우미 안내(Gemini). 인식 실패 때 한두 문장 */
    val aiHint: String? = null,
    /** AI 안내를 받으려면 전송 동의가 필요함(처음 한 번) */
    val askAiConsent: Boolean = false,
)

enum class ScanPhase { NONE, LASER, HIGHLIGHT }

/** AI 촬영 도우미에 보낼 요청(동의를 기다리는 동안 들고 있는다) */
private class HintRequest(val storeName: String, val lines: List<String>, val reason: String, val imageBase64: String?)

/** 방문 인증 화면 흐름 (S-603·S-606~S-609). 진행 단계·시도 횟수는 SavedStateHandle에 저장해 회전해도 이어진다. */
class VerifyViewModel(
    private val container: AppContainer,
    private val storeId: String,
    private val saved: SavedStateHandle,
    /** 기기 시계 오차(ms)를 재는 함수. 시험에서는 바꿔 끼운다 */
    private val measureClockSkew: suspend () -> Long? = { com.nopo.cheonnyeon.platform.network.ClockSkewChecker().measureMillis() },
) : ViewModel() {
    // 글자 인식기·위치는 쓸 때 만든다 (ViewModel을 단위 시험에서 만들 수 있게)
    private val recognizerLazy = lazy { MlKitKoreanRecognizer() }
    private val recognizer get() = recognizerLazy.value
    private val location by lazy { FusedLocationAdapter(container.appContext) }
    private val _state = MutableStateFlow(
        VerifyUiState(
            step = saved.get<String>(KEY_STEP)?.let(VerifyStep::valueOf) ?: VerifyStep.RECEIPT,
            failedPhotoAttempts = saved.get<Int>(KEY_ATTEMPTS) ?: 0,
            withinRange = saved.get<Boolean>(KEY_IN_RANGE) ?: false,
        ),
    )
    val state: StateFlow<VerifyUiState> = _state.asStateFlow()

    /** 사용자 설정(FR-VER-11). 읽지 못하면 기본값 */
    @Volatile private var allowLocationOnly = VerificationConfig.ALLOW_LOCATION_ONLY_AFTER_OCR_FAIL

    /** 인증 결과 화면의 "평점·리뷰 남기기": 가게 상세로 돌아가 리뷰 편집기를 열게 한다 */
    fun requestReview() { container.pendingReviewStoreId.value = storeId }

    init {
        com.nopo.cheonnyeon.core.common.Analytics.verifyStart()
        viewModelScope.launch {
            runCatching { allowLocationOnly = container.settings.allowLocationOnly.first() }
        }
        viewModelScope.launch {
            val skew = measureClockSkew()
            if (skew != null && com.nopo.cheonnyeon.core.util.ClockSkew.exceedsWarning(skew)) {
                _state.update { it.copy(clockSkewMinutes = kotlin.math.abs(skew) / 60_000L) }
            }
        }
        viewModelScope.launch {
            val store = container.catalog.byId(storeId)
            _state.update { it.copy(store = store) }
            if (store != null) {
                val limit = DailyLimitPolicy.check(container.visits.visitRefs(), store.id, store.isTest, container.clock.now())
                if (limit != null) {
                    val today = container.clock.now().atZone(com.nopo.cheonnyeon.core.common.SeoulZone).toLocalDate()
                    val list = container.database.myVisitDao().observeAll().first()
                        .filter { !it.storeId.startsWith(Store.TEST_PREFIX) && java.time.Instant.ofEpochMilli(it.lastVisitedAt).atZone(com.nopo.cheonnyeon.core.common.SeoulZone).toLocalDate() == today }
                        .map { (container.catalog.byId(it.storeId)?.name ?: it.storeId) to it.yearsAwarded }
                    _state.update { it.copy(blocked = true, error = limit, todayVisits = list) }
                }
            }
        }
    }

    private fun setStep(step: VerifyStep) {
        saved[KEY_STEP] = step.name
        _state.update { it.copy(step = step, error = null) }
    }

    /** 위치 켜기 대화상자를 처리한 뒤: 켰으면 다시 측정, 아니면 안내 */
    fun onLocationResolution(turnedOn: Boolean, approximateOnly: Boolean) {
        _state.update { it.copy(pendingResolution = null, error = if (turnedOn) null else AppError.E_LOC_TIMEOUT) }
        if (turnedOn) measureLocation(permissionGranted = true, approximateOnly = approximateOnly)
    }

    fun goToPhoto() = setStep(VerifyStep.LOCATE)
    fun backToReceipt() = setStep(VerifyStep.RECEIPT)
    fun clearError() = _state.update { it.copy(error = null) }

    /** ① 영수증: 촬영 → (레이저 연출) 글자 인식 → (형광펜 연출) 판정 → 기록. 이미지는 메모리에서만 쓰고 파일로 저장하지 않는다. */
    fun captureReceipt(camera: CameraController) {
        val store = _state.value.store ?: return
        if (_state.value.busy) return
        clearScan()
        _state.update { it.copy(busy = true, error = null, scanPhase = ScanPhase.LASER, aiHint = null) }
        viewModelScope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
            val scan = scanReceipt(camera)
            // 레이저가 최소 한 번은 훑고 지나가게 한다
            val wait = LASER_MIN_MS - (android.os.SystemClock.elapsedRealtime() - t0)
            if (wait > 0) kotlinx.coroutines.delay(wait)
            if (scan == null) {
                _state.update { it.copy(busy = false, scanPhase = ScanPhase.NONE, error = AppError.E_RECEIPT_UNREADABLE) }
                requestHint(store.name, emptyList(), AppError.E_RECEIPT_UNREADABLE, null)
                return@launch
            }
            _state.update { it.copy(scanPhase = ScanPhase.HIGHLIGHT, scanBoxes = scan.boxes) }
            kotlinx.coroutines.delay(HIGHLIGHT_MS)
            val visits = container.visits.visitRefs()
            when (val r = VerifyReceiptUseCase.verify(scan.lines, store, visits, container.clock.now())) {
                is AppResult.Failure -> {
                    _state.update { it.copy(busy = false, error = r.error) }
                    if (r.error in HINT_ERRORS) requestHint(store.name, scan.lines, r.error, state.value.scanFrame?.let(::jpegBase64))
                }
                is AppResult.Success -> {
                    finish(store, VerifyMethod.RECEIPT, photoVerified = true, hash = r.value.hash)
                    clearScan()
                }
            }
        }
    }

    /** 촬영해서 글자와 위치를 읽는다. 화면에 보일 줄인 장면은 상태에 둔다. 실패하면 null */
    private suspend fun scanReceipt(camera: CameraController): com.nopo.cheonnyeon.platform.ocr.OcrScan? = try {
        val bitmap = withTimeout(OCR_TIMEOUT_MS) { camera.captureInMemory { it.toUprightBitmap(OCR_MAX_SIDE) } }
        withContext(Dispatchers.Default) {
            try {
                val scale = FRAME_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
                val frame = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                _state.update { it.copy(scanFrame = frame) }
                withTimeout(OCR_TIMEOUT_MS) { recognizer.readScan(bitmap) }
            } finally {
                bitmap.recycle()
            }
        }
    } catch (_: TimeoutCancellationException) {
        null
    } catch (_: Exception) {
        null
    }

    private fun jpegBase64(frame: android.graphics.Bitmap): String? = runCatching {
        val out = java.io.ByteArrayOutputStream()
        frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
        android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
    }.getOrNull()

    private var pendingHint: HintRequest? = null

    /** 인식에 실패했을 때 Gemini가 원인과 다시 찍는 방법을 알려 준다. 키가 없거나 동의 전이면 건너뛴다 */
    private fun requestHint(storeName: String, lines: List<String>, error: AppError, imageBase64: String?) {
        if (!container.ai.available) return
        val req = HintRequest(storeName, lines, hintReason(error), imageBase64)
        viewModelScope.launch {
            if (container.settings.aiConsent.first()) runHint(req) else { pendingHint = req; _state.update { it.copy(askAiConsent = true) } }
        }
    }

    /** AI 안내 동의 대화상자에서 동의/거절 */
    fun answerAiConsent(agree: Boolean) {
        val req = pendingHint
        pendingHint = null
        _state.update { it.copy(askAiConsent = false) }
        if (agree && req != null) viewModelScope.launch { container.settings.setAiConsent(true); runHint(req) }
    }

    private suspend fun runHint(req: HintRequest) {
        val r = container.ai.receiptHint(req.storeName, req.lines, req.reason, req.imageBase64)
        if (r is AppResult.Success) _state.update { it.copy(aiHint = r.value) }
    }

    private fun hintReason(error: AppError) = when (error) {
        AppError.E_RECEIPT_MISMATCH -> "영수증의 가게 이름이 이 가게와 맞지 않아 보임(이름이 잘렸거나 다른 가게 영수증일 수 있음)"
        AppError.E_RECEIPT_NO_DATE -> "영수증에서 날짜·시간을 찾지 못함(잘렸거나 흐릴 수 있음)"
        else -> "영수증 글자를 거의 읽지 못함(흐림·어두움·기울어짐·너무 멀거나 가까움 등)"
    }

    /** 안내 창을 닫거나 다시 찍을 때: 연출 화면을 지우고 카메라로 돌아간다 */
    fun clearScan() {
        val old = _state.value.scanFrame
        _state.update { it.copy(scanFrame = null, scanBoxes = emptyList(), scanPhase = ScanPhase.NONE, aiHint = null) }
        runCatching { old?.recycle() }
    }

    fun dismissHint() = _state.update { it.copy(aiHint = null) }

    /** ② 사진·위치: 먼저 위치를 잰다 (최대 15초, 가장 정확한 값). */
    fun measureLocation(permissionGranted: Boolean, approximateOnly: Boolean) {
        val store = _state.value.store ?: return
        if (_state.value.busy) return
        val lat = store.lat
        val lng = store.lng
        if (lat == null || lng == null) { _state.update { it.copy(error = AppError.E_NO_COORD) }; return }
        if (!permissionGranted) { _state.update { it.copy(error = AppError.E_LOC_DENIED) }; return }
        if (approximateOnly) { _state.update { it.copy(error = AppError.E_LOC_APPROX_ONLY) }; return }
        _state.update { it.copy(busy = true, error = null, locatingBest = null) }
        viewModelScope.launch {
            // 기기 위치 설정이 꺼져 있으면 시스템 켜기 대화상자를 먼저 띄운다 (아키텍처 5.2 3단계)
            val settings = try { location.checkSettings() } catch (_: Exception) { null }
            if (settings is com.nopo.cheonnyeon.platform.location.LocationSettingsResult.Resolvable) {
                _state.update { it.copy(busy = false, pendingResolution = settings.exception) }
                return@launch
            }
            val fix: Fix? = try {
                location.bestFix { best -> _state.update { it.copy(locatingBest = best.accuracyM) } }
            } catch (_: Exception) {
                null
            }
            when {
                fix == null -> _state.update { it.copy(busy = false, error = AppError.E_LOC_TIMEOUT) }
                fix.isMock -> _state.update { it.copy(busy = false, error = AppError.E_LOC_MOCK) }
                else -> {
                    val distance = GeoDistance.meters(fix.lat, fix.lng, lat, lng)
                    val err = PhotoMatcher.judgeLocation(distance, fix.accuracyM)
                    val inRange = distance <= VerificationConfig.PHOTO_MAX_DISTANCE_M && fix.accuracyM <= VerificationConfig.PHOTO_MAX_ACCURACY_M
                    saved[KEY_IN_RANGE] = inRange
                    _state.update {
                        it.copy(
                            busy = false, error = err, withinRange = inRange,
                            errorMeters = if (err == AppError.E_ACCURACY) fix.accuracyM.toInt() else distance.toInt(),
                        )
                    }
                    if (err == null) setStep(VerifyStep.PHOTO)
                }
            }
        }
    }

    /** 간판·메뉴판 촬영 → 글자 인식 → 가게 이름 또는 전화번호 확인. 3번 실패하면 위치 확인 도장을 고를 수 있다. */
    fun capturePhoto(camera: CameraController) {
        val store = _state.value.store ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val proof = java.io.File(container.appContext.filesDir, "proofs/" + store.id + ".jpg")
            val lines = readText(camera, proof) ?: emptyList()
            val visits = container.visits.visitRefs()
            val limit = DailyLimitPolicy.check(visits, store.id, store.isTest, container.clock.now())
            if (limit != null) {
                _state.update { it.copy(busy = false, error = limit, blocked = true) }
                return@launch
            }
            when (PhotoMatcher.matchText(lines, store)) {
                is AppResult.Success -> finish(store, VerifyMethod.PHOTO, photoVerified = true, hash = null)
                is AppResult.Failure -> {
                    proof.delete()
                    val attempts = _state.value.failedPhotoAttempts + 1
                    saved[KEY_ATTEMPTS] = attempts
                    _state.update { it.copy(busy = false, error = AppError.E_PHOTO_NO_MATCH, failedPhotoAttempts = attempts) }
                }
            }
        }
    }

    /** 글자 확인이 3번 실패해도 30m 안이었다면 선택할 수 있는 ‘위치 확인 도장’ (FR-VER-11) */
    fun acceptLocationOnly() {
        val s = _state.value
        val store = s.store ?: return
        if (!PhotoMatcher.canOfferLocationOnly(s.failedPhotoAttempts, s.withinRange, allowLocationOnly)) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch { finish(store, VerifyMethod.LOCATION_ONLY, photoVerified = false, hash = null) }
    }

    fun canOfferLocationOnly(): Boolean = _state.value.let {
        PhotoMatcher.canOfferLocationOnly(it.failedPhotoAttempts, it.withinRange, allowLocationOnly)
    }

    private suspend fun finish(store: Store, method: VerifyMethod, photoVerified: Boolean, hash: String?) {
        // 로그인 직후 서버 기록을 내려받는 중이면 끝난 뒤에 기록한다 (서버에 있는 가게를 첫 방문으로 세지 않게)
        kotlinx.coroutines.withTimeoutOrNull(RESTORE_WAIT_MS) { container.restoring.first { !it } }
        val award = container.visits.record(store, method, photoVerified, hash)
        // "리뷰 남기기"로 넘어온 인증이면 성공한 뒤 가게 상세에서 리뷰 편집기를 연다
        if (container.reviewIntentStoreId.value == store.id) {
            container.pendingReviewStoreId.value = store.id
            container.reviewIntentStoreId.value = null
        }
        container.syncScheduler.enqueue()
        saved[KEY_STEP] = VerifyStep.DONE.name
        com.nopo.cheonnyeon.core.common.Analytics.verifyResult(method.name, "success")
        com.nopo.cheonnyeon.core.common.Analytics.stampAwarded(award.years, award.grade.name)
        _state.update { it.copy(busy = false, error = null, award = award, step = VerifyStep.DONE) }
    }

    /** 촬영 → 글자 줄 목록. 10초 제한, 이미지는 변환 직후 버린다. 실패하면 null */
    private suspend fun readText(camera: CameraController, proofFile: java.io.File? = null): List<String>? = try {
        val bitmap = withTimeout(OCR_TIMEOUT_MS) { camera.captureInMemory { it.toUprightBitmap(OCR_MAX_SIDE) } }
        withContext(Dispatchers.Default) {
            try {
                // 가게 사진 인증의 인증샷은 기기 안에만 저장한다 (FR-VER-12). 영수증은 저장하지 않는다.
                proofFile?.let { f ->
                    f.parentFile?.mkdirs()
                    f.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }
                }
                withTimeout(OCR_TIMEOUT_MS) { recognizer.read(bitmap).lines }
            } finally {
                bitmap.recycle()
            }
        }
    } catch (_: TimeoutCancellationException) {
        null
    } catch (_: Exception) {
        null
    }

    override fun onCleared() {
        runCatching { _state.value.scanFrame?.recycle() }
        if (recognizerLazy.isInitialized()) recognizer.close()
    }

    private companion object {
        const val KEY_STEP = "step"
        const val KEY_ATTEMPTS = "attempts"
        const val KEY_IN_RANGE = "in_range"
        const val OCR_TIMEOUT_MS = 10_000L
        const val LASER_MIN_MS = 1_700L
        const val HIGHLIGHT_MS = 1_800L
        const val FRAME_SIDE = 900
        val HINT_ERRORS = setOf(AppError.E_RECEIPT_UNREADABLE, AppError.E_RECEIPT_MISMATCH, AppError.E_RECEIPT_NO_DATE)
        const val OCR_MAX_SIDE = 1_600
        const val RESTORE_WAIT_MS = 10_000L
    }
}
