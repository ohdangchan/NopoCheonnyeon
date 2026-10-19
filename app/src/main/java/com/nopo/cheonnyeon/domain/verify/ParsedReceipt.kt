package com.nopo.cheonnyeon.domain.verify

import java.time.LocalDateTime

/** OCR 글자에서 뽑아낸 영수증 정보. 못 읽은 항목은 null. 전화·사업자번호는 숫자만. */
data class ParsedReceipt(
    val phone: String?,
    val businessNo: String?,
    val paidAt: LocalDateTime?,
    val amount: Long?,
    val name: String?,
)
