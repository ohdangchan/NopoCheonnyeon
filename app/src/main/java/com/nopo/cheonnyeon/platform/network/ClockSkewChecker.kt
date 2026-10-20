package com.nopo.cheonnyeon.platform.network

import com.nopo.cheonnyeon.core.util.ClockSkew
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

/** 서버(HTTP Date 헤더)와 기기 시계의 차이를 한 번 잰다. 연결이 없거나 실패하면 null. 아무 정보도 보내지 않는다. */
class ClockSkewChecker {
    suspend fun measureMillis(): Long? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                val conn = (URL(URL_204).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 4_000
                    readTimeout = 4_000
                }
                try {
                    conn.responseCode
                    ClockSkew.skewMillis(conn.getHeaderField("Date"), System.currentTimeMillis())
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private companion object {
        const val URL_204 = "https://www.google.com/generate_204"
        const val TIMEOUT_MS = 6_000L
    }
}
