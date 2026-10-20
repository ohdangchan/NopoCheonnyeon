package com.nopo.cheonnyeon.feature.diagnostics

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.rule.VerificationConfig
import com.nopo.cheonnyeon.platform.location.Fix
import com.nopo.cheonnyeon.platform.location.FusedLocationAdapter
import com.nopo.cheonnyeon.platform.location.LocationSettingsResult
import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

data class DiagLocationState(
    val measuring: Boolean = false,
    val secondsLeft: Int = 0,
    val best: Fix? = null,
    val firstFixMs: Long? = null,
    val fixCount: Int = 0,
    val timedOut: Boolean = false,
    val settingsOff: Boolean = false,
    val pendingResolution: ResolvableApiException? = null,
)

/** SPK-4: 최대 15초 측정해 가장 정확한 값을 고르고, 끝나면 업데이트를 해제한다. */
class DiagLocationViewModel(private val location: FusedLocationAdapter) : ViewModel() {
    private val _state = MutableStateFlow(DiagLocationState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    fun measure() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            when (val s = location.checkSettings()) {
                is LocationSettingsResult.Resolvable -> {
                    _state.update { it.copy(pendingResolution = s.exception, settingsOff = true) }
                    return@launch
                }
                LocationSettingsResult.Unavailable -> {
                    _state.update { it.copy(settingsOff = true) }
                    return@launch
                }
                LocationSettingsResult.Ok -> Unit
            }
            val timeout = VerificationConfig.LOCATION_TIMEOUT_MS
            val mark = TimeSource.Monotonic.markNow()
            _state.value = DiagLocationState(measuring = true, secondsLeft = (timeout / 1000).toInt())
            val ticker = launch {
                while (true) {
                    delay(1_000)
                    val left = ((timeout - mark.elapsedNow().inWholeMilliseconds) / 1000).toInt().coerceAtLeast(0)
                    _state.update { it.copy(secondsLeft = left) }
                }
            }
            withTimeoutOrNull(timeout) {
                location.updates()
                    .takeWhile { (_state.value.best?.accuracyM ?: Double.MAX_VALUE) > VerificationConfig.LOCATION_EARLY_STOP_ACCURACY_M }
                    .collect { fix ->
                        DiagLast.locationAccuracyM = fix.accuracyM
                        _state.update {
                            val better = it.best == null || fix.accuracyM < it.best.accuracyM
                            it.copy(
                                best = if (better) fix else it.best,
                                fixCount = it.fixCount + 1,
                                firstFixMs = it.firstFixMs ?: mark.elapsedNow().inWholeMilliseconds,
                            )
                        }
                    }
            }
            ticker.cancel()
            _state.update { it.copy(measuring = false, secondsLeft = 0, timedOut = it.best == null) }
        }
    }

    fun onResolutionHandled() = _state.update { it.copy(pendingResolution = null, settingsOff = false) }
}

@Composable
fun DiagnosticsLocationTab(modifier: Modifier = Modifier) {
    val permission = rememberPermission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (permission.status != PermissionStatus.GRANTED) {
        val msg = stringResource(if (permission.approximateOnly) R.string.perm_location_precise else R.string.perm_location_needed)
        PermissionGate(permission, msg, modifier)
        return
    }
    val appContext = LocalContext.current.applicationContext
    val viewModel: DiagLocationViewModel = viewModel { DiagLocationViewModel(FusedLocationAdapter(appContext)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    var targetLat by rememberSaveable { mutableStateOf("") }
    var targetLng by rememberSaveable { mutableStateOf("") }

    val resolver = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onResolutionHandled()
    }
    LaunchedEffect(state.pendingResolution) {
        state.pendingResolution?.let { resolver.launch(IntentSenderRequest.Builder(it.resolution).build()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        NopoButton(
            text = if (state.measuring) {
                stringResource(R.string.diag_loc_measuring, state.secondsLeft, state.best?.accuracyM?.let { "%.1f".format(it) } ?: "-")
            } else {
                stringResource(R.string.diag_loc_measure)
            },
            onClick = viewModel::measure,
            enabled = !state.measuring,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.settingsOff) Text(stringResource(R.string.diag_loc_settings_off), color = c.stamp, style = NopoTheme.typography.bodyMedium)
        if (state.timedOut) Text(stringResource(R.string.diag_loc_timeout), color = c.stamp, style = NopoTheme.typography.bodyMedium)

        val best = state.best
        if (best == null && !state.measuring) {
            Text(stringResource(R.string.diag_loc_none), color = c.ink2, style = NopoTheme.typography.bodyMedium)
        }
        best?.let { fix ->
            Text(
                stringResource(R.string.diag_loc_result, fix.accuracyM, (state.firstFixMs ?: 0).toInt(), state.fixCount),
                style = NopoTheme.extraTypography.metric,
                color = c.ink,
            )
            Text(stringResource(R.string.diag_loc_coords, fix.lat, fix.lng), style = NopoTheme.extraTypography.metric, color = c.ink2)
            Text(
                stringResource(if (fix.isMock) R.string.diag_loc_mock else R.string.diag_loc_not_mock),
                style = NopoTheme.typography.labelLarge,
                color = if (fix.isMock) c.stamp else c.primary,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            OutlinedTextField(
                value = targetLat,
                onValueChange = { targetLat = it },
                label = { Text(stringResource(R.string.diag_loc_target_lat)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = targetLng,
                onValueChange = { targetLng = it },
                label = { Text(stringResource(R.string.diag_loc_target_lng)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        val lat = targetLat.toDoubleOrNull()
        val lng = targetLng.toDoubleOrNull()
        if (best != null && lat != null && lng != null) {
            val d = GeoDistance.meters(best.lat, best.lng, lat, lng)
            val pass = d <= VerificationConfig.PHOTO_MAX_DISTANCE_M && best.accuracyM <= VerificationConfig.PHOTO_MAX_ACCURACY_M && !best.isMock
            Text(stringResource(R.string.diag_loc_distance, d), style = NopoTheme.extraTypography.metric, color = c.ink)
            Text(
                stringResource(if (pass) R.string.diag_loc_pass else R.string.diag_loc_fail),
                style = NopoTheme.typography.titleSmall,
                color = if (pass) c.primary else c.stamp,
            )
        }
    }
}
