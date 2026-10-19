package com.nopo.cheonnyeon.domain.verify

import java.security.MessageDigest
import java.time.format.DateTimeFormatter

/** 같은 영수증 재사용 방지용 지문 (FR-VER-08). 초는 빼고(OCR 오차 흡수), 금액은 1원 단위까지 넣는다. */
object ReceiptHasher {
    private val minuteFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    fun hash(r: ParsedReceipt, storeId: String): String {
        val shop = r.businessNo ?: r.phone ?: storeId
        val raw = listOf(shop, r.paidAt?.format(minuteFormat).orEmpty(), r.amount?.toString().orEmpty()).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
