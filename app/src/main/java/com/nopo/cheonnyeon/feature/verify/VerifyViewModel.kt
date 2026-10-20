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
)

/** 방문 인증 화면 흐름 (S-603·S-606~S-609). 진행 단계·시도 횟수는 SavedStateHandle에 저장해 회전해도 이어진다. */
class VerifyViewModel(
    private val container: AppContainer,
    private val storeId: String,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val recognizer = MlKitKoreanRecognizer()
    private val location = FusedLocationAdapter(container.appContext)
    private val _state = MutableStateFlow(
        VerifyUiState(
            step = saved.get<String>(KEY_STEP)?.let(VerifyStep::valueOf) ?: VerifyStep.RECEIPT,
            failedPhotoAttempts = saved.get<Int>(KEY_ATTEMPTS) ?: 0,
            withinRange = saved.get<Boolean>(KEY_IN_RANGE) ?: false,
        ),
    )
    val state: StateFlow<VerifyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val store = container.catalog.byId(storeId)
            _state.update { it.copy(store = store) }
            if (store != null) {
                val limit = DailyLimitPolicy.check(container.visits.visitRefs(), store.id, store.isTest, container.clock.now())
                if (limit != null) _state.update { it.copy(blocked = true, error = limit) }
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

    /** ① 영수증: 촬영 → 글자 인식 → 판정 → 기록. 이미지는 메모리에서만 쓰고 바로 버린다. */
    fun captureReceipt(camera: CameraController) {
        val store = _state.value.store ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val lines = readText(camera)
            if (lines == null) {
                _state.update { it.copy(busy = false, error = AppError.E_RECEIPT_UNREADABLE) }
                return@launch
            }
            val visits = container.visits.visitRefs()
            when (val r = VerifyReceiptUseCase.verify(lines, store, visits, container.clock.now())) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.error) }
                is AppResult.Success -> finish(store, VerifyMethod.RECEIPT, photoVerified = true, hash = r.value.hash)
            }
        }
    }

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
        if (!PhotoMatcher.canOfferLocationOnly(s.failedPhotoAttempts, s.withinRange, VerificationConfig.ALLOW_LOCATION_ONLY_AFTER_OCR_FAIL)) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch { finish(store, VerifyMethod.LOCATION_ONLY, photoVerified = false, hash = null) }
    }

    fun canOfferLocationOnly(): Boolean = _state.value.let {
        PhotoMatcher.canOfferLocationOnly(it.failedPhotoAttempts, it.withinRange, VerificationConfig.ALLOW_LOCATION_ONLY_AFTER_OCR_FAIL)
    }

    private suspend fun finish(store: Store, method: VerifyMethod, photoVerified: Boolean, hash: String?) {
        val award = container.visits.record(store, method, photoVerified, hash)
        container.syncScheduler.enqueue()
        saved[KEY_STEP] = VerifyStep.DONE.name
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

    override fun onCleared() = recognizer.close()

    private companion object {
        const val KEY_STEP = "step"
        const val KEY_ATTEMPTS = "attempts"
        const val KEY_IN_RANGE = "in_range"
        const val OCR_TIMEOUT_MS = 10_000L
        const val OCR_MAX_SIDE = 1_600
    }
}
