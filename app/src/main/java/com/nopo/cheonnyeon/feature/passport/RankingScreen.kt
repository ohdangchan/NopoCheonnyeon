package com.nopo.cheonnyeon.feature.passport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.labelRes
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronDown
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.data.remote.DemoUser
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import com.nopo.cheonnyeon.feature.main.MockNavBar

private val Line = Color(0xFFE6E0CC)
private val Gray = Color(0xFF4B5563)
private val SortOptions = listOf("방문한 노포 많은 순", "세월 높은 순", "리뷰 많은 순")

private data class RankRow(val rank: Int, val user: DemoUser, val me: Boolean)

private fun titleCount(years: Int) = TitlePolicy.titleFor(years).ordinal + 1

/** 시안 "미식가 랭킹" 복제. 수집한 스탬프 대신 수집한 칭호. 하단 탭은 앱의 5탭 */
@Composable
fun RankingScreen(vm: HallOfFameViewModel, onBack: () -> Unit, onOpenStore: (String) -> Unit, onTab: (Int) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    val rows = remember(state, sort) {
        val me = DemoUser(state.nickname.ifBlank { "나" }, state.myYears, state.myStores, 0)
        val all = state.byYears.map { RankRow(0, it, false) } + RankRow(0, me, true)
        val sorted = when (sort) {
            1 -> all.sortedByDescending { it.user.years }
            2 -> all.sortedWith(compareByDescending<RankRow> { it.user.reviews }.thenByDescending { it.user.years })
            else -> all.sortedWith(compareByDescending<RankRow> { it.user.stores }.thenByDescending { it.user.years })
        }
        sorted.take(100).mapIndexed { i, r -> r.copy(rank = i + 1) }
    }
    Column(Modifier.fillMaxSize().background(MockColors.Cream)) {
        LazyColumn(Modifier.weight(1f).statusBarsPadding()) {
            item {
                MockStage(originY = 84, heightPx = 915) {
                    MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
                    Box(Modifier.at(652, 98).size(px(76)).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "닫기" }, contentAlignment = Alignment.Center) { Close() }
                    MockPic(R.drawable.mk2_k_title, 760, 200, Modifier.at(0, 205))
                    // 탭
                    val names = listOf("전국 랭킹", "서울 랭킹", "친구 랭킹")
                    val xs = listOf(20, 270, 510)
                    val ws = listOf(238, 232, 232)
                    names.forEachIndexed { i, n ->
                        val sel = tab == i
                        Box(
                            Modifier.at(xs[i], 410).size(px(ws[i]), px(74)).clip(RoundedCornerShape(50)).background(if (sel) MockColors.Navy else Color(0xFFF7F3E6))
                                .then(if (sel) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(50))).clickable(role = Role.Tab) { tab = i },
                            contentAlignment = Alignment.Center,
                        ) { MockText(n, 24, color = if (sel) Color.White else MockColors.Navy, weight = FontWeight.ExtraBold) }
                    }
                    if (tab == 0 && rows.size >= 3) {
                        val top = rows.take(3)
                        Podium(top[1], 18, 556, 237, 425, Brush.verticalGradient(listOf(Color(0xFFE6EAF2), Color(0xFFF6F4EC))), Color(0xFF7A8AA6), 2)
                        Podium(top[0], 250, 534, 262, 447, Brush.verticalGradient(listOf(Color(0xFFFBE9A6), Color(0xFFFCF3D0))), Color(0xFFD9A323), 1)
                        Podium(top[2], 505, 556, 237, 425, Brush.verticalGradient(listOf(Color(0xFFF8DCC4), Color(0xFFFBEFE3))), Color(0xFFB8692B), 3)
                    } else if (tab != 0) {
                        MockText(if (tab == 1) "서울 랭킹은 준비 중이에요." else "친구 랭킹은 준비 중이에요.\n친구를 초대하면 함께 겨룰 수 있어요.", 24, Modifier.at(40, 620).width(px(680)), color = Gray, align = TextAlign.Center, maxLines = 3, lineHeightPx = 36)
                    }
                }
            }
            if (tab == 0) {
                item {
                    Box(Modifier.fillMaxWidth().padding(horizontal = px(18))) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = px(26), topEnd = px(26))).background(Color(0xFFFCFAF4)).border(1.dp, Line, RoundedCornerShape(topStart = px(26), topEnd = px(26)))) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = px(22), vertical = px(22)), verticalAlignment = Alignment.CenterVertically) {
                                MockText("전체 랭킹", 28, font = MockSerif, weight = FontWeight.ExtraBold)
                                Box(Modifier.width(px(14)))
                                MockText("TOP 100", 21, color = Gray)
                                Box(Modifier.weight(1f))
                                Box {
                                    Row(Modifier.clickable(role = Role.Button) { sortMenu = true }, verticalAlignment = Alignment.CenterVertically) {
                                        MockText(SortOptions[sort], 21, color = Gray); Box(Modifier.width(px(8))); ChevronDown(22)
                                    }
                                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                        SortOptions.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { sort = i; sortMenu = false }) }
                                    }
                                }
                            }
                        }
                    }
                }
                itemsIndexed(rows.drop(3), key = { _, r -> r.rank }) { _, r ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = px(18)).background(Color(0xFFFCFAF4)).border(0.5.dp, Line)) { RankLine(r) }
                }
            }
        }
        MockNavBar(3, onTab)
    }
}

@Composable
private fun Close() {
    Canvas(Modifier.size(px(38))) {
        val sw = size.width * 0.1f
        drawLine(MockColors.Navy, Offset(size.width * 0.1f, size.height * 0.1f), Offset(size.width * 0.9f, size.height * 0.9f), sw, StrokeCap.Round)
        drawLine(MockColors.Navy, Offset(size.width * 0.9f, size.height * 0.1f), Offset(size.width * 0.1f, size.height * 0.9f), sw, StrokeCap.Round)
    }
}

@Composable
private fun Avatar(name: String, sizePx: Int, tint: Color) {
    Box(Modifier.size(px(sizePx)).clip(CircleShape).background(tint.copy(alpha = 0.18f)).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            drawCircle(tint.copy(alpha = 0.55f), w * 0.2f, Offset(w * 0.5f, w * 0.38f))
            drawCircle(tint.copy(alpha = 0.55f), w * 0.36f, Offset(w * 0.5f, w * 1.0f))
        }
    }
}

private val Tints = listOf(Color(0xFF6B7B93), Color(0xFFB07A2B), Color(0xFF8A5A3B), Color(0xFF4A7A5A), Color(0xFF7A5A8A))

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Podium(row: RankRow, x: Int, y: Int, w: Int, h: Int, bg: Brush, medal: Color, place: Int) {
    val years = row.user.years
    Box(Modifier.at(x, y).size(px(w), px(h)).clip(RoundedCornerShape(topStart = px(w / 2), topEnd = px(w / 2), bottomStart = px(26), bottomEnd = px(26))).background(bg))
    // 메달
    Box(Modifier.at(x + w / 2 - 38, y - 22).size(px(76)).clip(CircleShape).background(medal), contentAlignment = Alignment.Center) { MockText("$place", 36, color = Color.White, weight = FontWeight.ExtraBold) }
    val av = if (place == 1) 176 else 160
    Box(Modifier.at(x + w / 2 - av / 2, y + 62)) { Avatar(row.user.nickname, av, Tints[place]) }
    val ny = y + 62 + av + 14
    MockText(row.user.nickname, 25, Modifier.at(x, ny).width(px(w)), weight = FontWeight.ExtraBold, align = TextAlign.Center)
    Box(Modifier.at(x + 18, ny + 44).size(px(w - 36), px(44)).clip(RoundedCornerShape(50)).background(medal.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
        MockText(androidx.compose.ui.res.stringResource(TitlePolicy.titleFor(years).labelRes()), 20, weight = FontWeight.ExtraBold, maxLines = 1)
    }
    val cy = ny + 106
    MockText("${row.user.stores}곳", 22, Modifier.at(x + 14, cy).width(px(w / 2 - 14)), weight = FontWeight.ExtraBold, align = TextAlign.Center)
    MockText("방문한 노포", 15, Modifier.at(x + 14, cy + 34).width(px(w / 2 - 14)), color = Gray, align = TextAlign.Center)
    MockText("${titleCount(years)}개", 22, Modifier.at(x + w / 2, cy).width(px(w / 2 - 14)), weight = FontWeight.ExtraBold, align = TextAlign.Center)
    MockText("수집한 칭호", 15, Modifier.at(x + w / 2, cy + 34).width(px(w / 2 - 14)), color = Gray, align = TextAlign.Center)
}

@Composable
private fun RankLine(r: RankRow) {
    val tint = Tints[r.rank % Tints.size]
    Row(Modifier.fillMaxWidth().height(px(104)).padding(horizontal = px(10)), verticalAlignment = Alignment.CenterVertically) {
        MockText("${r.rank}", 34, Modifier.width(px(70)), weight = FontWeight.ExtraBold, align = TextAlign.Center, font = MockSerif)
        Avatar(r.user.nickname, 82, tint)
        Box(Modifier.width(px(18)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(px(6))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MockText(r.user.nickname + if (r.me) " (나)" else "", 25, Modifier.weight(1f, fill = false), weight = FontWeight.ExtraBold)
                Box(Modifier.width(px(12)))
                Box(Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.18f)).padding(horizontal = px(14), vertical = px(3))) {
                    MockText(androidx.compose.ui.res.stringResource(TitlePolicy.titleFor(r.user.years).labelRes()), 17, weight = FontWeight.Medium)
                }
            }
            MockText("세월 ${r.user.years}년 · 리뷰 ${r.user.reviews}개", 18, color = Gray)
        }
        Column(Modifier.width(px(130)), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) { MockPic(R.drawable.mk2_k_ic_store, 34, 33); Box(Modifier.width(px(8))); MockText("${r.user.stores}곳", 22, weight = FontWeight.ExtraBold) }
            MockText("방문한 노포", 15, color = Gray)
        }
        Column(Modifier.width(px(130)), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) { MockPic(R.drawable.mk2_k_ic_ticket, 34, 35); Box(Modifier.width(px(8))); MockText("${titleCount(r.user.years)}개", 22, weight = FontWeight.ExtraBold) }
            MockText("수집한 칭호", 15, color = Gray)
        }
        ChevronRight(26)
    }
}
