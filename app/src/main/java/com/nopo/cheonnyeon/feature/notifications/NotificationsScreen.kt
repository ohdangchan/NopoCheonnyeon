package com.nopo.cheonnyeon.feature.notifications

import com.nopo.cheonnyeon.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import java.time.Instant
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class NoticeItem(
    val key: String,
    val title: String,
    val body: String,
    val at: Long,
    val storeId: String? = null,
    val lime: Boolean = false,
    val tab: Int? = null
)

/**
 * 알림 목록. 서버 푸시 없이, 이 기기의 방문 기록에서 도장·칭호 소식을 만들고
 * 알림 설정(새 소식·오늘의 추천)에 따라 안내를 함께 보여 준다.
 */
/** 알림 목록을 만드는 규칙 (순수 함수): 시험 가게 방문은 뺀다. 칭호가 오른 방문에만 칭호 알림이 붙고, 최신순으로 정렬한다. */
fun buildNotices(
    visits: List<MyVisitEntity>, storeNames: Map<String, String>, news: Boolean, reco: Boolean, nowMs: Long,
): List<NoticeItem> {
    val real = visits.filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sortedBy { it.firstVisitedAt }
    val out = ArrayList<NoticeItem>()
    if (news) {
        var total = 0
        real.forEach { v ->
            val name = storeNames[v.storeId] ?: v.storeId
            val before = total
            total += v.yearsAwarded
            out += NoticeItem("stamp-${v.storeId}", "도장을 찍었어요", "$name 방문 인증 완료 · 세월 +${v.yearsAwarded}년", v.firstVisitedAt, v.storeId, lime = true)
            TitlePolicy.titleUp(before, total)?.let { out += NoticeItem("title-${it.name}", "새 칭호를 얻었어요", "누적 세월 ${total}년 · 새 칭호를 확인해 보세요", v.firstVisitedAt + 1, tab = 3) }
        }
    }
    if (reco) out += NoticeItem("reco", "오늘의 추천", "홈 화면의 '오늘의 AI 추천'에서 오늘 가볼 노포를 확인해 보세요.", nowMs - 1, tab = 0)
    out += NoticeItem("welcome", "노포 패스포트에 오신 걸 환영해요", "방문할수록 더 특별해지는 나만의 노포 여행 기록을 시작해 보세요.", 0L, tab = 4)
    return out.sortedByDescending { it.at }
}

class NotificationsViewModel(private val container: AppContainer) : ViewModel() {
    val items: StateFlow<List<NoticeItem>> = combine(
        container.database.myVisitDao().observeAll(),
        container.catalog.changes,
        container.settings.notifyNews,
        container.settings.notifyReco,
    ) { visits, _, news, reco ->
        val names = container.catalog.all().associate { it.id to it.name }
        buildNotices(visits, names, news, reco, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

@Composable
fun NotificationsScreen(vm: NotificationsViewModel, onBack: () -> Unit, onOpenStore: (String) -> Unit, onOpenTab: (Int) -> Unit = {}) {
    val items by vm.items.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = px(20), vertical = px(18)), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(
                role = Role.Button,
                onClick = onBack
            ).padding(px(10))) { MockText(stringResource(com.nopo.cheonnyeon.R.string.notice_01), 34, font = MockSerif, weight = FontWeight.ExtraBold) }
        }
        LazyColumn(contentPadding = PaddingValues(px(20)), verticalArrangement = Arrangement.spacedBy(px(14))) {
            items(items, key = { it.key }) { n ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(px(18))).background(MockColors.Card).border(
                        1.dp,
                        MockColors.CardLine,
                        RoundedCornerShape(px(18))
                    )
                        .clickable(role = Role.Button) { if (n.storeId != null) onOpenStore(n.storeId) else if (n.tab != null) onOpenTab(n.tab) }.padding(px(22)),
                    horizontalArrangement = Arrangement.spacedBy(px(16)), verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.padding(top = px(8)).size(px(16)).clip(CircleShape).background(if (n.lime) MockColors.LimeDeep else Color_Gray))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(px(6))) {
                        MockText(n.title, 26, weight = FontWeight.Bold)
                        MockText(n.body, 22, color = MockColors.Ink2, maxLines = 3, lineHeightPx = 30)
                        if (n.at > 0) {
                            val d = Instant.ofEpochMilli(n.at).atZone(SeoulZone).toLocalDate()
                            MockText("%d. %02d. %02d".format(d.year, d.monthValue, d.dayOfMonth), 18, color = MockColors.Hint)
                        }
                    }
                }
            }
        }
    }
}

private val Color_Gray = Color(0xFFCFCCBE)
