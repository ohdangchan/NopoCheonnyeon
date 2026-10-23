package com.nopo.cheonnyeon.feature.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.mock.BookmarkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.StarIcon
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.TasteRecommender
import com.nopo.cheonnyeon.feature.common.StoreCards
import com.nopo.cheonnyeon.feature.store.StorePhoto
import kotlinx.coroutines.launch
import java.time.LocalDate

private val Hairline = Color(0xFFE2DDCB)
private val Gray = Color(0xFF4B5563)
private val Ink2 = Color(0xFF2D3B4F)
private val CardWhite = Color(0xFFFCFAF4)

private val RegionRes = mapOf(
    "jongno" to R.drawable.mk2_n_reg_jongno, "euljiro" to R.drawable.mk2_n_reg_euljiro, "mapo" to R.drawable.mk2_n_reg_mapo,
    "seongsu" to R.drawable.mk2_n_reg_seongsu, "busan" to R.drawable.mk2_n_reg_busan,
)
private val FoodRes = mapOf(
    "naengmyeon" to R.drawable.mk2_n_food_naengmyeon, "gukbap" to R.drawable.mk2_n_food_gukbap, "bunsik" to R.drawable.mk2_n_food_bunsik,
    "gogi" to R.drawable.mk2_n_food_gogi, "kalguksu" to R.drawable.mk2_n_food_kalguksu, "jeontongju" to R.drawable.mk2_n_food_jeontongju,
    "haemul" to R.drawable.mk2_n_food_haemul, "jeon" to R.drawable.mk2_n_food_jeon,
)

/**
 * 설치 후 첫 세 화면(시안 26·25·24): ① 시작 화면 → ② 좋아하는 지역·음식 고르기(2/3) → ③ 준비 완료와 취향 추천(3/3).
 * 어느 단계에서든 "나중에 하기/둘러보기"를 누르면 건너뛰고 로그인으로 간다.
 */
@Composable
fun OnboardingScreen(container: AppContainer, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var regions by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var foods by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val scope = rememberCoroutineScope()
    androidx.activity.compose.BackHandler(enabled = step > 0) { step -= 1 }
    val finish = { onDone() }
    when (step) {
        0 -> IntroPage(onStart = { step = 1 }, onBrowse = finish)
        1 -> PreferencePage(
            regions, foods,
            onToggleRegion = { k -> regions = if (k in regions) regions - k else regions + k },
            onToggleFood = { k -> foods = if (k in foods) foods - k else foods + k },
            onNext = { scope.launch { container.settings.setTaste(regions.toSet(), foods.toSet()) }; step = 2 },
            onLater = finish,
        )
        else -> ReadyPage(container, regions, foods, onStart = finish, onLater = finish)
    }
}

@Composable
private fun IntroPage(onStart: () -> Unit, onBrowse: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFBF6EB)).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1567) {
            MockPic(R.drawable.mk2_n_intro1, 760, 1567, Modifier.at(0, 84))
            Box(Modifier.at(30, 1400).size(px(706), px(96)).clickable(role = Role.Button, onClick = onStart).semantics { contentDescription = "시작하기" })
            Box(Modifier.at(235, 1510).size(px(290), px(76)).clickable(role = Role.Button, onClick = onBrowse).semantics { contentDescription = "둘러보기" })
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, label: String) {
    // 시안: 연두 막대 + 끝의 짙은 점 + "2 / 3"
    Box(Modifier.at(38, 238).size(px(194), px(14)).clip(CircleShape).background(Color(0xFFE6E2D2)))
    Box(Modifier.at(38, 238).size(px(194 * fraction), px(14)).clip(CircleShape).background(MockColors.Lime2))
    Box(Modifier.at((38 + 194 * fraction - 9).toInt(), 235).size(px(20)).clip(CircleShape).background(MockColors.Lime2))
    Box(Modifier.at((38 + 194 * fraction - 5).toInt(), 239).size(px(12)).clip(CircleShape).background(Color(0xFF2B3A18)))
    MockText(label, 24, Modifier.at((38 + 194 + 18).toInt(), 228), color = MockColors.Navy, weight = FontWeight.Medium)
}

@Composable
private fun Underline(x: Int, y: Int, w: Int) {
    Box(Modifier.at(x, y).size(px(w), px(14)).rotate(-1.2f).clip(RoundedCornerShape(50)).background(MockColors.Lime2))
}

@Composable
private fun Badge(selected: Boolean, centerX: Int, centerY: Int) {
    val d = 46
    Box(
        Modifier.padding(start = px(centerX - d / 2), top = px(centerY - d / 2)).size(px(d)).clip(CircleShape)
            .background(if (selected) MockColors.Lime2 else Color(0xB3808896)).border(3.dp, if (selected) MockColors.Lime2 else Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Canvas(Modifier.size(px(24))) {
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(size.width * 0.12f, size.height * 0.55f); lineTo(size.width * 0.4f, size.height * 0.82f); lineTo(size.width * 0.9f, size.height * 0.2f) }
            drawPath(p, MockColors.Navy, style = Stroke(size.width * 0.16f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

@Composable
private fun SectionHead(icon: @Composable () -> Unit, y: Int, title: String, sub: String) {
    Box(Modifier.at(30, y).size(px(70)).clip(CircleShape).background(Color(0xFFF1EDE0)), contentAlignment = Alignment.Center) { icon() }
    MockText(title, 27, Modifier.at(135, y + 4), color = MockColors.Navy, weight = FontWeight.ExtraBold)
    MockText(sub, 21, Modifier.at(135, y + 44), color = Gray)
}

@Composable
private fun PinGlyph() = Canvas(Modifier.size(px(40))) {
    val w = size.width
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.5f, w * 0.96f); cubicTo(w * 0.1f, w * 0.55f, w * 0.14f, w * 0.3f, w * 0.14f, w * 0.3f)
        cubicTo(w * 0.14f, w * 0.1f, w * 0.3f, 0f, w * 0.5f, 0f); cubicTo(w * 0.7f, 0f, w * 0.86f, w * 0.1f, w * 0.86f, w * 0.3f)
        cubicTo(w * 0.86f, w * 0.3f, w * 0.9f, w * 0.55f, w * 0.5f, w * 0.96f); close()
    }
    drawPath(p, Color(0xFFA6D921)); drawPath(p, MockColors.Navy, style = Stroke(w * 0.06f))
    drawCircle(MockColors.Navy, w * 0.13f, Offset(w * 0.5f, w * 0.33f))
}

@Composable
private fun ForkGlyph() = Canvas(Modifier.size(px(40))) {
    val w = size.width
    val st = Stroke(w * 0.09f, cap = StrokeCap.Round)
    for (x in listOf(0.2f, 0.33f, 0.46f)) drawLine(MockColors.Navy, Offset(w * x, w * 0.1f), Offset(w * x, w * 0.4f), w * 0.07f, StrokeCap.Round)
    drawLine(MockColors.Navy, Offset(w * 0.2f, w * 0.38f), Offset(w * 0.46f, w * 0.38f), w * 0.07f, StrokeCap.Round)
    drawLine(MockColors.Navy, Offset(w * 0.33f, w * 0.38f), Offset(w * 0.33f, w * 0.92f), w * 0.1f, StrokeCap.Round)
    drawOval(MockColors.Navy, Offset(w * 0.64f, w * 0.08f), androidx.compose.ui.geometry.Size(w * 0.24f, w * 0.42f))
    drawLine(MockColors.Navy, Offset(w * 0.76f, w * 0.45f), Offset(w * 0.76f, w * 0.92f), w * 0.1f, StrokeCap.Round)
}

@Composable
private fun CompassGlyph() = Canvas(Modifier.size(px(46))) {
    val w = size.width
    drawCircle(MockColors.Navy, w * 0.44f, Offset(w / 2, w / 2), style = Stroke(w * 0.07f))
    val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.68f, w * 0.32f); lineTo(w * 0.56f, w * 0.56f); lineTo(w * 0.32f, w * 0.68f); lineTo(w * 0.44f, w * 0.44f); close() }
    drawPath(p, MockColors.Navy)
}

@Composable
private fun BigLimeButton(text: String, y: Int, onClick: () -> Unit) {
    Box(Modifier.at(28, y).size(px(704), px(86)).clip(RoundedCornerShape(50)).background(MockColors.Lime2).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompassGlyph(); Box(Modifier.width(px(20))); MockText(text, 32, weight = FontWeight.ExtraBold); Box(Modifier.width(px(14))); ChevronRight(30)
        }
    }
}

@Composable
private fun PreferencePage(
    regions: List<String>, foods: List<String>, onToggleRegion: (String) -> Unit, onToggleFood: (String) -> Unit, onNext: () -> Unit, onLater: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFBF6EB)).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1575) {
            MockPic(R.drawable.mk2_n_pref_foot, 760, 151, Modifier.at(0, 1500))
            MockPic(R.drawable.mk2_n_pref2_head, 760, 506, Modifier.at(0, 84))
            ProgressBar(2f / 3f, "2 / 3")
            MockText("어떤 노포 여행을", 54, Modifier.at(38, 288), font = MockSerif, weight = FontWeight.ExtraBold)
            MockText("좋아하세요?", 54, Modifier.at(38, 358), font = MockSerif, weight = FontWeight.ExtraBold)
            Underline(38, 436, 285)
            MockText("좋아하는 지역과 음식 취향을 선택하면", 24, Modifier.at(40, 478), color = Ink2)
            MockText("당신만의 노포를 더 잘 추천해드려요.", 24, Modifier.at(40, 516), color = Ink2)

            SectionHead({ PinGlyph() }, 572, "어떤 지역을 좋아하세요?", "여러 지역을 선택할 수 있어요.")
            Box(Modifier.at(580, 596).size(px(152), px(52)).clip(RoundedCornerShape(50)).border(1.dp, Hairline, RoundedCornerShape(50)).background(CardWhite), contentAlignment = Alignment.Center) {
                MockText("인기 지역 순 ⌄", 21, color = MockColors.Navy)
            }
            TasteRecommender.REGIONS.forEachIndexed { i, r ->
                val sel = r.key in regions
                val x = 22 + i * 145
                Box(
                    Modifier.at(x, 664).size(px(140), px(268)).clip(RoundedCornerShape(px(20))).background(CardWhite)
                        .border(if (sel) 3.dp else 1.dp, if (sel) MockColors.Lime2 else Hairline, RoundedCornerShape(px(20)))
                        .clickable(role = Role.Checkbox) { onToggleRegion(r.key) }.semantics { contentDescription = r.label },
                ) {
                    Box(Modifier.padding(start = px(6), top = px(8)).size(px(128), px(140)).clip(RoundedCornerShape(px(14)))) { MockPic(RegionRes.getValue(r.key), 128, 140) }
                    Badge(sel, 6 + 103, 8 + 25)
                    MockText(r.label, 26, Modifier.padding(start = px(12), top = px(160)), color = MockColors.Navy, weight = FontWeight.ExtraBold)
                    MockText(r.desc, 18, Modifier.padding(start = px(12), top = px(200)).width(px(124)), color = Gray, maxLines = 2, lineHeightPx = 25)
                }
            }

            SectionHead({ ForkGlyph() }, 950, "어떤 음식을 좋아하세요?", "여러 가지를 선택할 수 있어요.")
            TasteRecommender.FOODS.forEachIndexed { i, f ->
                val sel = f.key in foods
                val x = 24 + (i % 4) * 181
                val y = if (i < 4) 1040 else 1228
                Box(
                    Modifier.at(x, y).size(px(176), px(180)).clip(RoundedCornerShape(px(20))).background(CardWhite)
                        .border(if (sel) 3.dp else 1.dp, if (sel) MockColors.Lime2 else Hairline, RoundedCornerShape(px(20)))
                        .clickable(role = Role.Checkbox) { onToggleFood(f.key) }.semantics { contentDescription = f.label },
                ) {
                    Box(Modifier.padding(start = px(6), top = px(8)).size(px(164), px(98)).clip(RoundedCornerShape(px(14)))) { MockPic(FoodRes.getValue(f.key), 164, 98) }
                    Badge(sel, 6 + 137, 8 + 25)
                    MockText(f.label, 25, Modifier.padding(start = px(12), top = px(112)), color = MockColors.Navy, weight = FontWeight.ExtraBold)
                    MockText(f.desc, 17, Modifier.padding(start = px(12), top = px(146)).width(px(160)), color = Gray)
                }
            }
            BigLimeButton("이 취향으로 추천받기", 1440, onNext)
            Box(Modifier.at(250, 1548).size(px(260), px(60)).clickable(role = Role.Button, onClick = onLater), contentAlignment = Alignment.Center) {
                MockText("나중에 하기", 27, color = Gray, weight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ReadyPage(container: AppContainer, regions: List<String>, foods: List<String>, onStart: () -> Unit, onLater: () -> Unit) {
    val year = remember { LocalDate.now(SeoulZone).year }
    val pick by produceState<Store?>(null, regions, foods) {
        val all = container.catalog.all()
        value = TasteRecommender.recommend(all, regions.toSet(), foods.toSet(), container.ratings.value.mapValues { it.value.average })
    }
    val picked = TasteRecommender.REGIONS.filter { it.key in regions }.take(2)
    val pickedFoods = TasteRecommender.FOODS.filter { it.key in foods }.take(4)
    Column(Modifier.fillMaxSize().background(Color(0xFFFBF6EB)).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1575) {
            MockPic(R.drawable.mk2_n_pref_foot, 760, 151, Modifier.at(0, 1500))
            MockPic(R.drawable.mk2_n_pref3_head, 760, 506, Modifier.at(0, 84))
            ProgressBar(1f, "3 / 3")
            MockText("준비가 끝났어요!", 58, Modifier.at(38, 296), font = MockSerif, weight = FontWeight.ExtraBold)
            Underline(38, 380, 330)
            MockText("선택한 지역과 음식 취향을 바탕으로", 25, Modifier.at(40, 426), color = Ink2)
            MockText("당신만을 위한 노포를 추천해드릴게요.", 25, Modifier.at(40, 466), color = Ink2)
            MockText("이제, 특별한 노포 여행이 시작됩니다.", 25, Modifier.at(40, 506), color = Ink2)

            SectionHead({ PinGlyph() }, 572, "선택한 지역", "이런 지역의 노포를 찾아드릴게요.")
            if (picked.isEmpty()) MockText("고른 지역이 없어요. 전국에서 골라 드릴게요.", 22, Modifier.at(40, 700), color = Gray)
            picked.forEachIndexed { i, r ->
                Row(
                    Modifier.at(33 + i * 355, 664).size(px(342), px(154)).clip(RoundedCornerShape(px(22))).border(2.dp, MockColors.Lime2, RoundedCornerShape(px(22))).background(CardWhite).padding(px(8)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(px(138), px(138)).clip(RoundedCornerShape(px(14)))) { MockPic(RegionRes.getValue(r.key), 138, 138) }
                    Column(Modifier.padding(start = px(16)), verticalArrangement = Arrangement.spacedBy(px(4))) {
                        MockText(r.label, 27, color = MockColors.Navy, weight = FontWeight.ExtraBold)
                        MockText(r.desc, 18, Modifier.width(px(160)), color = Gray, maxLines = 2, lineHeightPx = 25)
                    }
                }
            }

            SectionHead({ ForkGlyph() }, 836, "선택한 음식 취향", "이런 음식을 중심으로 추천해드릴게요.")
            if (pickedFoods.isEmpty()) MockText("고른 음식이 없어요. 다양한 음식으로 골라 드릴게요.", 22, Modifier.at(40, 940), color = Gray)
            pickedFoods.forEachIndexed { i, f ->
                Box(Modifier.at(28 + i * 180, 930).size(px(172), px(176)).clip(RoundedCornerShape(px(20))).border(2.dp, MockColors.Lime2, RoundedCornerShape(px(20))).background(CardWhite)) {
                    Box(Modifier.padding(start = px(6), top = px(8)).size(px(160), px(96)).clip(RoundedCornerShape(px(14)))) { MockPic(FoodRes.getValue(f.key), 164, 98) }
                    MockText(f.label, 25, Modifier.padding(start = px(12), top = px(110)), color = MockColors.Navy, weight = FontWeight.ExtraBold)
                    MockText(f.desc, 17, Modifier.padding(start = px(12), top = px(144)).width(px(156)), color = Gray)
                }
            }

            SectionHead({ StarIcon(36, color = MockColors.Navy) }, 1128, "이런 노포는 어떠세요?", "당신의 취향에 딱 맞는 노포를 엄선했어요.")
            val st = pick
            if (st != null) {
                val ratingMap by container.ratings.collectAsStateWithLifecycle()
                val fix by container.lastFix.collectAsStateWithLifecycle()
                val card = StoreCards.of(st, year, ratingMap, fix, emptySet())
                Row(Modifier.at(20, 1218).size(px(722), px(206)).clip(RoundedCornerShape(px(22))).background(CardWhite).border(1.dp, Hairline, RoundedCornerShape(px(22)))) {
                    Box(Modifier.size(px(338), px(206)).clip(RoundedCornerShape(px(18)))) {
                        StorePhoto(st, Modifier.fillMaxSize())
                        Box(Modifier.padding(start = px(284), top = px(14)).size(px(44)).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { BookmarkIcon(26, color = MockColors.Navy) }
                    }
                    Column(Modifier.padding(start = px(18), top = px(10), end = px(10)), verticalArrangement = Arrangement.spacedBy(px(6))) {
                        Row(horizontalArrangement = Arrangement.spacedBy(px(10))) {
                            listOfNotNull(st.sigungu.takeIf { it.isNotBlank() }, pickedFoods.firstOrNull { TasteRecommender.matchesFood(st, it) }?.label).forEach { t ->
                                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFEBDD)).padding(horizontal = px(16), vertical = px(4))) { MockText(t, 18, color = Gray) }
                            }
                        }
                        MockText(st.name, 30, Modifier.width(px(330)), font = MockSerif, weight = FontWeight.ExtraBold)
                        com.nopo.cheonnyeon.core.designsystem.mock.RatingText(card.rating?.average, card.rating?.count, 20, showCount = true)
                        MockText(card.oneLiner, 19, Modifier.width(px(330)), color = Gray, maxLines = 2, lineHeightPx = 26)
                    }
                }
            }
            BigLimeButton("노포 추천 받기", 1440, onStart)
            Box(Modifier.at(250, 1548).size(px(260), px(60)).clickable(role = Role.Button, onClick = onLater), contentAlignment = Alignment.Center) {
                MockText("나중에 하기", 27, color = Gray, weight = FontWeight.Medium)
            }
        }
    }
}
