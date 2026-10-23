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
import kotlinx.coroutines.withTimeoutOrNull

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
    /** 영수증 글자 확인을 마쳤고 사용자가 "인증 정보 확정하기"를 누르기를 기다리는 중 */
    val confirm: ReceiptInfo? = null,
    /** 인증을 마친 뒤 그 가게가 속한 시·도의 지역 스탬프 진행 */
    val region: com.nopo.cheonnyeon.domain.rule.RegionProgress? = null,
    /** 마지막으로 잰 가게까지의 거리(미터). 아직 안 쟀으면 null */
    val distanceM: Int? = null,
    /** 마지막으로 잰 내 위치(지도에 표시만 하고 저장·전송하지 않는다) */
    val myLat: Double? = null,
    val myLng: Double? = null,
    /** 최근 인증한 노포(최신순, 최대 5곳) */
    val recent: List<RecentVisit> = emptyList(),
)

data class RecentVisit(val store: Store, val years: Int, val at: Long)

/** 확인 화면에 보여 줄 영수증에서 읽은 값(없으면 null) */
data class ReceiptInfo(val storeName: String, val paidAt: java.time.LocalDateTime?, val amount: Long?, val address: String, val hash: String?, val approval: String?)

enum class ScanPhase { NONE, LASER, HIGHLIGHT }

private val APPROVAL_REGEX = Regex("승\\s*인\\s*번\\s*호\\s*[:：]?\\s*([0-9]{4,})")

/** AI 촬영 도우미에 보낼 요청(동의를 기다리는 동안 들고 있는다) */
private class HintRequest(val storeName: String, val lines: List<String>, val reason: String, val imageBase64: String?, val subject: String = "영수증")

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
            // 인증 완료(DONE)는 결과값이 저장되지 않으므로 복원하면 처음 단계로 돌아간다
            step = saved.get<String>(KEY_STEP)?.let(VerifyStep::valueOf)?.takeIf { it != VerifyStep.DONE } ?: VerifyStep.RECEIPT,
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
        loadRecent()
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

    /** 첫 화면과 완료 화면에 보여 줄 최근 인증 노포 */
    private fun loadRecent() {
        viewModelScope.launch {
            runCatching {
                val list = container.database.myVisitDao().observeAll().first()
                    .filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sortedByDescending { it.lastVisitedAt }.take(5)
                val rows = list.mapNotNull { v -> container.catalog.byId(v.storeId)?.let { RecentVisit(it, v.yearsAwarded, v.lastVisitedAt) } }
                _state.update { it.copy(recent = rows) }
            }
        }
    }

    private fun setStep(step: VerifyStep) {
        clearScan()
        saved[KEY_STEP] = step.name
        _state.update { it.copy(step = step, error = null) }
    }

    /** 위치 켜기 대화상자를 처리한 뒤: 켰으면 다시 측정, 아니면 안내 */
    fun onLocationResolution(turnedOn: Boolean, approximateOnly: Boolean) {
        _state.update { it.copy(pendingResolution = null, error = if (turnedOn) null else AppError.E_LOC_TIMEOUT) }
        if (turnedOn) measureLocation(permissionGranted = true, approximateOnly = approximateOnly)
    }

    fun goToPhoto() = setStep(VerifyStep.LOCATE)

    /** 위치 확인을 마친 뒤 "방문 인증 시작" */
    fun startPhotoStep() { if (_state.value.withinRange) setStep(VerifyStep.PHOTO) }

    val ratings by lazy { container.ratings }
    fun backToReceipt() = setStep(VerifyStep.RECEIPT)
    fun clearError() = _state.update { it.copy(error = null) }

    /** 확인 화면에서 "인증 정보 확정하기" */
    fun confirmReceipt() {
        val s = _state.value
        val info = s.confirm ?: return
        val store = s.store ?: return
        if (s.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            finish(store, VerifyMethod.RECEIPT, photoVerified = true, hash = info.hash)
            clearScan()
            _state.update { it.copy(confirm = null) }
        }
    }

    /** 확인 화면에서 "다시 촬영" */
    fun retakeReceipt() { clearScan(); _state.update { it.copy(confirm = null, error = null) } }

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
            // 판정은 바로 해 두고, 실패면 형광펜 연출이 도는 동안 AI 안내를 미리 요청한다(화면에 늦게 뜨지 않게)
            val visits = container.visits.visitRefs()
            val verdict = VerifyReceiptUseCase.verify(scan.lines, store, visits, container.clock.now())
            _state.update { it.copy(scanPhase = ScanPhase.HIGHLIGHT, scanBoxes = scan.boxes) }
            if (verdict is AppResult.Failure && verdict.error in HINT_ERRORS) requestHint(store.name, scan.lines, verdict.error, null)
            kotlinx.coroutines.delay(if (verdict is AppResult.Failure) FAIL_HIGHLIGHT_MS else HIGHLIGHT_MS)
            when (val r = verdict) {
                is AppResult.Failure -> {
                    _state.update { it.copy(busy = false, error = r.error) }
                }
                is AppResult.Success -> {
                    // 바로 도장을 찍지 않고, 읽은 정보를 보여 주고 사용자가 확정할 때 기록한다
                    val joined = scan.lines.joinToString("\n")
                    val parsed = com.nopo.cheonnyeon.domain.verify.ReceiptParser.parse(joined)
                    val approval = APPROVAL_REGEX.find(joined)?.groupValues?.get(1)
                    _state.update {
                        it.copy(
                            busy = false, scanPhase = ScanPhase.NONE,
                            confirm = ReceiptInfo(store.name, parsed.paidAt, parsed.amount, store.address, r.value.hash, approval),
                        )
                    }
                }
            }
        }
    }

    /** 촬영해서 글자와 위치를 읽는다. 화면에 보일 줄인 장면은 상태에 둔다. 실패하면 null */
    private suspend fun scanReceipt(camera: CameraController, proofFile: java.io.File? = null): com.nopo.cheonnyeon.platform.ocr.OcrScan? = try {
        val bitmap = withTimeout(OCR_TIMEOUT_MS) { camera.captureInMemory { it.toUprightBitmap(OCR_MAX_SIDE) } }
        withContext(Dispatchers.Default) {
            try {
                val scale = FRAME_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
                val frame = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                _state.update { it.copy(scanFrame = frame) }
                // 가게 사진 인증의 인증샷은 기기 안에만 저장한다 (FR-VER-12). 영수증은 저장하지 않는다.
                proofFile?.let { f ->
                    f.parentFile?.mkdirs()
                    f.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }
                }
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
    private fun requestHint(storeName: String, lines: List<String>, error: AppError, imageBase64: String?, subject: String = "영수증") {
        if (!container.ai.available) return
        val req = HintRequest(storeName, lines, hintReason(error), imageBase64, subject)
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

    /**
     * AI 안내는 2~3초 안에 띄운다. 사진을 같이 보내면 오래 걸려 글자 줄만 보내고, 그래도 늦으면 앱이 가진 안내문으로 대신한다.
     */
    private suspend fun runHint(req: HintRequest) {
        val r = withTimeoutOrNull(HINT_TIMEOUT_MS) { container.ai.receiptHint(req.storeName, req.lines, req.reason, null, req.subject) }
        val tip = (r as? AppResult.Success)?.value ?: localHint(req.subject)
        // 판정 연출이 끝나(실패 화면으로 바뀌)기 전에는 띄우지 않는다
        withTimeoutOrNull(5_000L) { _state.first { !it.busy } }
        if (_state.value.error != null || _state.value.confirm == null) _state.update { it.copy(aiHint = tip) }
    }

    private fun localHint(subject: String) =
        if (subject == "영수증") "영수증 전체가 화면 틀 안에 들어오게, 가게 이름·날짜·금액이 선명하게 보이도록 밝은 곳에서 평평하게 다시 찍어 주세요."
        else "간판이나 메뉴판의 가게 이름이 크게 보이도록 가까이서, 흔들리지 않게 다시 찍어 주세요."

    private fun hintReason(error: AppError) = when (error) {
        AppError.E_RECEIPT_MISMATCH -> "영수증의 가게 이름이 이 가게와 맞지 않아 보임(이름이 잘렸거나 다른 가게 영수증일 수 있음)"
        AppError.E_RECEIPT_NO_DATE -> "영수증에서 날짜·시간을 찾지 못함(잘렸거나 흐릴 수 있음)"
        AppError.E_PHOTO_NO_MATCH -> "간판·메뉴판 글자에서 가게 이름이나 전화번호를 찾지 못함(글자가 잘렸거나 흐림, 너무 멀리 찍음, 다른 곳을 찍었을 수 있음)"
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
                            busy = false, error = err, withinRange = inRange, distanceM = distance.toInt(), myLat = fix.lat, myLng = fix.lng,
                            errorMeters = if (err == AppError.E_ACCURACY) fix.accuracyM.toInt() else distance.toInt(),
                        )
                    }
                    // 위치가 확인되면 "방문 인증 시작"을 눌러 다음 단계(가게 사진)로 간다
                }
            }
        }
    }

    /** 간판·메뉴판 촬영 → (레이저 연출) 글자 인식 → (형광펜 연출) 가게 이름 또는 전화번호 확인. 3번 실패하면 위치 확인 도장을 고를 수 있다. */
    fun capturePhoto(camera: CameraController) {
        val store = _state.value.store ?: return
        if (_state.value.busy) return
        clearScan()
        _state.update { it.copy(busy = true, error = null, scanPhase = ScanPhase.LASER, aiHint = null) }
        viewModelScope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
            val proof = java.io.File(container.appContext.filesDir, "proofs/" + store.id + ".jpg")
            val scan = scanReceipt(camera, proof)
            val wait = LASER_MIN_MS - (android.os.SystemClock.elapsedRealtime() - t0)
            if (wait > 0) kotlinx.coroutines.delay(wait)
            val lines = scan?.lines ?: emptyList()
            if (scan != null) {
                _state.update { it.copy(scanPhase = ScanPhase.HIGHLIGHT, scanBoxes = scan.boxes) }
                kotlinx.coroutines.delay(HIGHLIGHT_MS)
            } else {
                _state.update { it.copy(scanPhase = ScanPhase.NONE) }
            }
            val visits = container.visits.visitRefs()
            val limit = DailyLimitPolicy.check(visits, store.id, store.isTest, container.clock.now())
            if (limit != null) {
                _state.update { it.copy(busy = false, error = limit, blocked = true) }
                return@launch
            }
            when (PhotoMatcher.matchText(lines, store)) {
                is AppResult.Success -> {
                    finish(store, VerifyMethod.PHOTO, photoVerified = true, hash = null)
                    clearScan()
                }
                is AppResult.Failure -> {
                    proof.delete()
                    val attempts = _state.value.failedPhotoAttempts + 1
                    saved[KEY_ATTEMPTS] = attempts
                    _state.update { it.copy(busy = false, error = AppError.E_PHOTO_NO_MATCH, failedPhotoAttempts = attempts) }
                    requestHint(store.name, lines, AppError.E_PHOTO_NO_MATCH, _state.value.scanFrame?.let(::jpegBase64), "간판·메뉴판")
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
        val region = runCatching {
            val visited = container.database.myVisitDao().observeAll().first().map { it.storeId }.toSet()
            com.nopo.cheonnyeon.domain.rule.RegionProgressRule.compute(container.catalog.all(), visited).firstOrNull { it.sido == store.sido }
        }.getOrNull()
        _state.update { it.copy(busy = false, error = null, award = award, step = VerifyStep.DONE, region = region) }
        loadRecent()
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
        const val HINT_TIMEOUT_MS = 2_500L
        const val LASER_MIN_MS = 1_700L
        const val HIGHLIGHT_MS = 1_800L
        const val FAIL_HIGHLIGHT_MS = 700L
        const val FRAME_SIDE = 900
        val HINT_ERRORS = setOf(AppError.E_RECEIPT_UNREADABLE, AppError.E_RECEIPT_MISMATCH, AppError.E_RECEIPT_NO_DATE, AppError.E_PHOTO_NO_MATCH)
        const val OCR_MAX_SIDE = 1_600
        const val RESTORE_WAIT_MS = 10_000L
    }
}
