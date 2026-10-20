package com.nopo.cheonnyeon.feature.diagnostics

/** 진단 화면 상단에 보여 줄 마지막 측정값 (FR-SPK-03). 메모리에만 두고 저장·전송하지 않는다. */
object DiagLast {
    @Volatile var locationAccuracyM: Double? = null
    @Volatile var ocrMs: Long? = null
    @Volatile var mapAuthError: String? = null
}
