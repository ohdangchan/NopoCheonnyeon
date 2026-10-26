package com.nopo.cheonnyeon.domain.rule

/** AI가 추천한 메뉴 하나 */
data class MenuPick(
    val menu: String,
    val reason: String,
    /** 주재료·구성 성분(밀가루, 소고기, 회 등) */
    val ingredients: List<String> = emptyList(),
)

/**
 * 메뉴 추천 룰렛 규칙: 한 번 돌릴 때마다 메뉴 3개, 최대 10번(= 서로 다른 메뉴 30개).
 * 새로 돌릴 때는 앞서 나온 메뉴를 AI에게 "제외"로 알려 준다.
 * 이전 라운드는 최대 9번까지 기억한다(10번째 라운드가 앞의 9번을 기억).
 * AI가 제외한 메뉴를 또 내놓아도 여기서 한 번 더 걸러 중복이 없게 한다.
 */
object MenuRoulette {
    const val PER_ROUND = 3
    const val MAX_ROUNDS = 10
    const val MEMORY_ROUNDS = 9
    const val MAX_MENUS = PER_ROUND * MAX_ROUNDS

    private val ignoredInKey = Regex("\\(.*?\\)|\\s+|[^0-9a-z가-힣]")

    fun canSpin(rounds: List<List<MenuPick>>): Boolean = rounds.size < MAX_ROUNDS

    /** AI에게 알려 줄 제외 메뉴: 최근 [MEMORY_ROUNDS]라운드의 메뉴 */
    fun exclusions(rounds: List<List<MenuPick>>): List<String> =
        rounds.takeLast(MEMORY_ROUNDS).flatten().map { it.menu }

    /** 이미 나온 메뉴·한 라운드 안의 중복·빈 이름을 걸러 최대 [PER_ROUND]개만 */
    fun fresh(
        candidates: List<MenuPick>,
        rounds: List<List<MenuPick>>,
        alsoTaken: List<MenuPick> = emptyList(),
    ): List<MenuPick> {
        val seen = (rounds.flatten() + alsoTaken).map { key(it.menu) }.toMutableSet()
        val out = ArrayList<MenuPick>()
        for (c in candidates) {
            val k = key(c.menu)
            if (k.isEmpty() || !seen.add(k)) continue
            out += c
            if (out.size == PER_ROUND) break
        }
        return out
    }

    /** 공백·괄호·대소문자 차이를 무시하고 같은 메뉴인지 비교하는 열쇠 */
    fun key(menu: String): String = menu.lowercase().replace(ignoredInKey, "")
}
