package com.nopo.cheonnyeon.platform.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** 좌표 → 주소 결과. 도로명 주소가 없는 곳(산·논밭 등)은 [road]가 null이다. */
data class AddressResult(
    /** 예: 서울특별시 중구 세종대로 110 */
    val road: String?,
    /** 예: 서울특별시청 (건물 이름, 없으면 null) */
    val building: String?,
    /** 예: 서울특별시 중구 태평로1가 31 */
    val jibun: String?,
)

sealed interface ReverseGeocodeError {
    /** 키 오류(401)·이용 신청 안 됨 등 */
    data class Http(val code: Int) : ReverseGeocodeError
    data object Network : ReverseGeocodeError
    data object Timeout : ReverseGeocodeError
}

/**
 * 네이버 클라우드 Maps Reverse Geocoding (map-reversegeocode/v2/gc).
 * 좌표는 주소 변환 요청에만 쓰고 저장·로그하지 않는다(PRD 8.2 원칙).
 * 키는 BuildConfig(local.properties)에서 받는다.
 */
class ReverseGeocoder(private val clientId: String, private val clientSecret: String) {

    suspend fun lookup(lat: Double, lng: Double): Result<AddressResult> = withContext(Dispatchers.IO) {
        runCatching {
            withTimeout(TIMEOUT_MS) {
                val coords = String.format(Locale.US, "%.7f,%.7f", lng, lat) // 경도,위도 순서
                val url = URL("$ENDPOINT?coords=$coords&output=json&orders=roadaddr,addr")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7_000
                    readTimeout = 7_000
                    setRequestProperty("x-ncp-apigw-api-key-id", clientId)
                    setRequestProperty("x-ncp-apigw-api-key", clientSecret)
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = conn.responseCode
                    if (code != HttpURLConnection.HTTP_OK) throw ReverseGeocodeException(ReverseGeocodeError.Http(code))
                    parse(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                } finally {
                    conn.disconnect()
                }
            }
        }.recoverCatching { e ->
            throw when (e) {
                is ReverseGeocodeException -> e
                is kotlinx.coroutines.TimeoutCancellationException -> ReverseGeocodeException(ReverseGeocodeError.Timeout)
                else -> ReverseGeocodeException(ReverseGeocodeError.Network)
            }
        }
    }

    private companion object {
        const val ENDPOINT = "https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc"
        const val TIMEOUT_MS = 10_000L
    }
}

class ReverseGeocodeException(val error: ReverseGeocodeError) : Exception(error.toString())

/** 응답 JSON → 주소. 단위 테스트를 위해 공개한다. */
internal fun parse(json: String): AddressResult {
    val results = JSONObject(json).optJSONArray("results") ?: return AddressResult(null, null, null)
    var road: String? = null
    var building: String? = null
    var jibun: String? = null
    for (i in 0 until results.length()) {
        val r = results.getJSONObject(i)
        val region = r.optJSONObject("region") ?: continue
        val area = (1..4).mapNotNull { n ->
            region.optJSONObject("area$n")?.optString("name")?.takeIf { it.isNotBlank() }
        }
        val land = r.optJSONObject("land")
        when (r.optString("name")) {
            "roadaddr" -> if (land != null) {
                // 도로명 주소는 시·도 + 시·군·구 + 도로명 + 건물번호 (읍·면·동은 넣지 않음)
                val number = listOf(land.optString("number1"), land.optString("number2"))
                    .filter { it.isNotBlank() }.joinToString("-")
                road = (area.take(2) + listOf(land.optString("name"), number))
                    .filter { it.isNotBlank() }.joinToString(" ")
                building = land.optJSONObject("addition0")?.optString("value")?.takeIf { it.isNotBlank() }
            }
            "addr" -> if (land != null) {
                val number = listOf(land.optString("number1"), land.optString("number2"))
                    .filter { it.isNotBlank() }.joinToString("-")
                val mountain = if (land.optString("type") == "2") "산 " else ""
                jibun = (area + listOf(mountain + number)).filter { it.isNotBlank() }.joinToString(" ")
            }
        }
    }
    return AddressResult(road, building, jibun)
}
