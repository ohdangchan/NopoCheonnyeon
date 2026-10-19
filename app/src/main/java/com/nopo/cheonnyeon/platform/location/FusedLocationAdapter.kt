package com.nopo.cheonnyeon.platform.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.nopo.cheonnyeon.domain.rule.VerificationConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** 한 번 받은 위치값. 좌표는 기기 안에서만 쓰고 서버·로그로 보내지 않는다. */
data class Fix(val lat: Double, val lng: Double, val accuracyM: Double, val isMock: Boolean)

/** 기기 위치 설정 확인 결과 */
sealed interface LocationSettingsResult {
    data object Ok : LocationSettingsResult
    /** 시스템 '위치 켜기' 대화상자를 띄울 수 있다 */
    data class Resolvable(val exception: ResolvableApiException) : LocationSettingsResult
    data object Unavailable : LocationSettingsResult
}

/**
 * Fused Location 감싸기 (아키텍처 5.2, S-605).
 * [updates]는 수집을 멈추면 반드시 위치 업데이트를 해제한다.
 */
class FusedLocationAdapter(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val settings = LocationServices.getSettingsClient(context)

    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, VerificationConfig.LOCATION_INTERVAL_MS)
        .setMinUpdateIntervalMillis(VerificationConfig.LOCATION_INTERVAL_MS)
        .setWaitForAccurateLocation(false)
        .build()

    suspend fun checkSettings(): LocationSettingsResult = try {
        settings.checkLocationSettings(LocationSettingsRequest.Builder().addLocationRequest(request).build()).await()
        LocationSettingsResult.Ok
    } catch (e: ResolvableApiException) {
        LocationSettingsResult.Resolvable(e)
    } catch (e: Exception) {
        LocationSettingsResult.Unavailable
    }

    /** 권한 확인은 호출하는 쪽 책임 */
    @SuppressLint("MissingPermission")
    fun updates(): Flow<Fix> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toFix()) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }
}

private fun Location.toFix() = Fix(
    lat = latitude,
    lng = longitude,
    accuracyM = accuracy.toDouble(),
    isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else @Suppress("DEPRECATION") isFromMockProvider,
)

/** 두 지점 사이 거리(m). S-204 GeoDistance가 생기면 그쪽으로 옮긴다. */
fun distanceMeters(a: Fix, lat: Double, lng: Double): Double {
    val out = FloatArray(1)
    Location.distanceBetween(a.lat, a.lng, lat, lng, out)
    return out[0].toDouble()
}
