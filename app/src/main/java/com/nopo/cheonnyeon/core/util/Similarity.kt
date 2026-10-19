package com.nopo.cheonnyeon.core.util

/** 글자 유사도 0.0~1.0. 정규화한 뒤 비교하며, 어느 한쪽이 비면 일치로 보지 않는다(0.0). */
object Similarity {
    /** 이름 전체를 비교: (긴 쪽 길이 − 편집 거리) / 긴 쪽 길이. 영수증 상호 대조(FR-VER-05)에 쓴다. */
    fun names(a: String, b: String): Double {
        val x = TextNormalizer.normalize(a)
        val y = TextNormalizer.normalize(b)
        if (x.isEmpty() || y.isEmpty()) return 0.0
        val longest = maxOf(x.length, y.length)
        return (longest - editDistance(x, y)).toDouble() / longest
    }

    /**
     * [name]이 [text] 안 어딘가에 (오타를 허용하며) 들어 있는 정도. 간판 글자처럼 가게 이름 앞뒤에
     * 다른 글자가 붙는 경우를 위한 것으로, (이름 길이 − text 속 가장 비슷한 부분과의 편집 거리) / 이름 길이.
     */
    fun partial(name: String, text: String): Double {
        val p = TextNormalizer.normalize(name)
        val t = TextNormalizer.normalize(text)
        if (p.isEmpty() || t.isEmpty()) return 0.0
        var prev = IntArray(t.length + 1) // 첫 줄은 0: text의 어디서 시작해도 비용 없음
        for (i in 1..p.length) {
            val cur = IntArray(t.length + 1)
            cur[0] = i
            for (j in 1..t.length) {
                val cost = if (p[i - 1] == t[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return (p.length - prev.min()).coerceAtLeast(0).toDouble() / p.length
    }

    /** 삽입·삭제·교체·인접 두 글자 맞바꿈을 각각 1로 센다 (Damerau-Levenshtein, 최적 문자열 정렬). */
    internal fun editDistance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }
}
