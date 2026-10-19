package com.nopo.cheonnyeon.domain.verify

import com.nopo.cheonnyeon.core.util.BusinessNumber
import java.time.DateTimeException
import java.time.LocalDateTime

/** 영수증 OCR 글자 → [ParsedReceipt] (PRD 7.3). 실패한 항목은 null이며 예외를 던지지 않는다. */
object ReceiptParser {
    private const val DIGITISH = "[0-9OolI]"
    private val phoneRegex = Regex("(?<![0-9A-Za-z])([0O]$DIGITISH{1,2})[-.)\\s]?($DIGITISH{3,4})[-.\\s]?($DIGITISH{4})(?![0-9A-Za-z])")
    private val bizRegex = Regex("(?<!\\d)(\\d{3})-?(\\d{2})-?(\\d{5})(?!\\d)")
    private val dateRegex = Regex(
        "(?<!\\d)(\\d{4}|\\d{2})\\s*[-./]\\s*(\\d{1,2})\\s*[-./]\\s*(\\d{1,2})\\s*(?:\\([^)]*\\))?\\s*(\\d{1,2}):(\\d{2})(?::\\d{2})?",
    )
    private val amountRegex = Regex("(?:합\\s*계(?:\\s*금\\s*액)?|결\\s*제\\s*금\\s*액|받을\\s*금\\s*액|총\\s*액)\\s*[:：]?\\s*([0-9][0-9,]*)")
    private val nameLabelRegex = Regex("(?:가맹점명|가맹점|상호명|상호|매장명)\\s*[:：]?\\s*(.+)")
    private val addressToken = Regex("[가-힣]*(특별시|광역시|특별자치\\S*|도|시|군|구|로|길)(\\s|$)")

    fun parse(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ParsedReceipt(
            phone = findPhone(lines),
            businessNo = findBusinessNo(text),
            paidAt = findPaidAt(text),
            amount = findAmount(text),
            name = findName(lines),
        )
    }

    private fun findPhone(lines: List<String>): String? {
        for (line in lines) {
            if (line.contains("카드") || line.contains("승인")) continue // 카드·승인번호를 전화로 오인하지 않는다
            val m = phoneRegex.find(line) ?: continue
            // 숫자 맥락에서만 O→0, l·I→1 보정
            return m.value.map { c ->
                when (c) {
                    'O', 'o' -> '0'
                    'l', 'I' -> '1'
                    else -> c
                }
            }.filter { it in '0'..'9' }.joinToString("")
        }
        return null
    }

    private fun findBusinessNo(text: String): String? =
        bizRegex.findAll(text).map { it.groupValues.drop(1).joinToString("") }.firstOrNull(BusinessNumber::isValid)

    private fun findPaidAt(text: String): LocalDateTime? {
        for (m in dateRegex.findAll(text)) {
            val (y, mo, d, h, mi) = m.destructured
            val year = if (y.length == 2) 2000 + y.toInt() else y.toInt()
            try {
                return LocalDateTime.of(year, mo.toInt(), d.toInt(), h.toInt(), mi.toInt())
            } catch (_: DateTimeException) {
                continue
            }
        }
        return null
    }

    private fun findAmount(text: String): Long? =
        amountRegex.findAll(text).lastOrNull()?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()

    private fun findName(lines: List<String>): String? {
        lines.firstNotNullOfOrNull { nameLabelRegex.find(it)?.groupValues?.get(1)?.trim()?.takeIf { v -> v.isNotEmpty() } }
            ?.let { return it }
        return lines.take(6)
            .filter { line -> line.none { it.isDigit() || it == ':' || it == '：' } && !addressToken.containsMatchIn(line) }
            .map { it to it.count { c -> c in '가'..'힣' } }
            .filter { it.second >= 2 }
            .maxByOrNull { it.second }?.first
    }
}
