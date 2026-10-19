package com.nopo.cheonnyeon.core.util

/**
 * 상호 비교용 정규화 (PRD 7.3): 법인 표기·"본점"·"원조"·공백·특수문자를 없애고 영문은 소문자로.
 * 한글·한자·숫자·영문만 남기므로 이모지나 빈 입력에서도 예외가 나지 않는다.
 */
object TextNormalizer {
    // "주식회사"를 "(주)"보다 먼저 지워야 "주"가 남지 않는다
    private val removeWords = listOf("주식회사", "유한회사", "농업회사법인", "영농조합법인", "(주)", "㈜", "본점", "원조")

    fun normalize(raw: String): String {
        var text = raw
        removeWords.forEach { text = text.replace(it, "") }
        return buildString { text.lowercase().forEach { if (it.isLetterOrDigit()) append(it) } }
    }
}
