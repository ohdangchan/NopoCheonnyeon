package com.nopo.cheonnyeon.domain.rule

/** 한 가게의 단골 지표 */
data class LoyaltyInput(
    /** 방문자 수(인증한 사람) */
    val visitors: Int,
    /** 다시 방문한 사람 수 */
    val revisitors: Int,
    /** 리뷰에 적힌 단골 횟수의 평균 */
    val avgVisitCount: Double,
    val reviewCount: Int,
) {
    val rate: Double get() = if (visitors <= 0) 0.0 else revisitors.toDouble() / visitors
}

/** 비교 대상 가게들의 분포 요약(AI에게 근거로 건넨다) */
data class PeerSummary(
    val count: Int,
    val meanRate: Double,
    val medianRate: Double,
    val p75Rate: Double,
    val meanVisits: Double,
)

/**
 * 단골 충성도(0~100) 기준 점수. 재방문율과 평균 단골 횟수를 다른 가게들과 견준 백분위로 만들고,
 * 표본(방문자 수)이 적으면 50점 쪽으로 끌어당긴다. AI는 이 기준 점수를 근거로 ±[AI_ADJUST] 안에서 조정한다.
 */
object LoyaltyScore {
    const val AI_ADJUST = 12
    /** 표본이 이만큼 이상이면 점수를 그대로 믿는다 */
    const val FULL_CONFIDENCE_VISITORS = 20

    /** 기준 점수에서 재방문율 백분위와 평균 단골 횟수 백분위가 차지하는 비중 (합 1.0) */
    private const val RATE_WEIGHT = 0.65
    private const val VISITS_WEIGHT = 0.35

    /** [value]가 [sortedPeers] 중 얼마나 위인지(0~1). 같은 값은 절반씩 */
    fun percentile(value: Double, sortedPeers: List<Double>): Double {
        if (sortedPeers.isEmpty()) return 0.5
        val below = sortedPeers.count { it < value }
        val equal = sortedPeers.count { it == value }
        return (below + equal / 2.0) / sortedPeers.size
    }

    fun summary(peers: List<LoyaltyInput>): PeerSummary {
        if (peers.isEmpty()) return PeerSummary(0, 0.0, 0.0, 0.0, 0.0)
        val rates = peers.map { it.rate }.sorted()
        return PeerSummary(
            count = peers.size,
            meanRate = rates.average(),
            medianRate = rates[rates.size / 2],
            p75Rate = rates[(rates.size * 3 / 4).coerceAtMost(rates.size - 1)],
            meanVisits = peers.map { it.avgVisitCount }.average(),
        )
    }

    /** 기준 점수(0~100) */
    fun baseline(input: LoyaltyInput, peers: List<LoyaltyInput>): Int {
        val rateP = percentile(input.rate, peers.map { it.rate }.sorted())
        val visitsP = percentile(input.avgVisitCount, peers.map { it.avgVisitCount }.sorted())
        val raw = (rateP * RATE_WEIGHT + visitsP * VISITS_WEIGHT) * 100.0
        val confidence = (input.visitors.toDouble() / FULL_CONFIDENCE_VISITORS).coerceIn(0.0, 1.0)
        return (50.0 + (raw - 50.0) * confidence).toInt().coerceIn(0, 100)
    }

    /** 재방문율이 비교 대상 중 상위 몇 %인지(1~100, 작을수록 좋음) */
    fun topPercent(input: LoyaltyInput, peers: List<LoyaltyInput>): Int =
        (100 - (percentile(input.rate, peers.map { it.rate }.sorted()) * 100).toInt()).coerceIn(1, 100)

    /** AI 점수를 기준 점수 ±[AI_ADJUST]와 0~100 안으로 */
    fun clamp(aiScore: Int, baseline: Int): Int =
        aiScore.coerceIn(baseline - AI_ADJUST, baseline + AI_ADJUST).coerceIn(0, 100)

    /** 점수대별 이름표 */
    fun gradeKey(score: Int): Int = when {
        score >= 85 -> 4
        score >= 70 -> 3
        score >= 50 -> 2
        score >= 30 -> 1
        else -> 0
    }
}
