package com.nopo.cheonnyeon.feature.help

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronDown
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockInput
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.feature.my.SUPPORT_EMAIL
import com.nopo.cheonnyeon.R

private val Hairline = Color(0xFFE2DDCB)
private val Gray = Color(0xFF4B5563)
private val CardWhite = Color(0xFFFCFAF4)

private data class Cat(val title: String, val sub: String, val icon: Int)
private data class Faq(val cat: Int, val q: String, val a: String)

private val Cats = listOf(
    Cat("일반 문의하기", "서비스 이용, 제휴, 기타 문의", R.drawable.mk2_n_help_ic1),
    Cat("인증 문제", "방문 인증, GPS, 인증 오류", R.drawable.mk2_n_help_ic2),
    Cat("계정 / 로그인", "회원가입, 로그인, 계정 관리", R.drawable.mk2_n_help_ic3),
    Cat("영수증 검증", "영수증 촬영, 검증 실패", R.drawable.mk2_n_help_ic4),
    Cat("스탬프 / 컬렉션", "스탬프 적립, 여권, 컬렉션", R.drawable.mk2_n_help_ic5),
    Cat("지도 / 맛집 정보", "검색, 위치, 맛집 정보 오류", R.drawable.mk2_n_help_ic6),
)

private val Faqs = listOf(
    Faq(1, "방문 인증은 어떻게 하나요?", "노포 매장 상세 화면의 [방문 인증하기] 버튼을 눌러 진행하세요. 방문한 날 결제한 영수증을 앱 안 카메라로 찍으면 가게 이름·날짜·금액을 확인한 뒤 인증 정보를 확정할 수 있어요."),
    Faq(1, "GPS 인증이 되지 않아요.", "위치 권한이 허용돼 있는지, 기기 위치(GPS)가 켜져 있는지 확인해 주세요. 가게에서 너무 멀거나 위치 정확도가 낮으면 인증할 수 없어요. 창가나 야외에서 다시 시도해 보세요."),
    Faq(3, "영수증 인증은 어떤 방식으로 하나요?", "영수증 전체가 한 장에 보이도록 찍으면 글자를 읽어 식당 이름과 결제 시각을 확인해요. 영수증 사진은 저장하지 않고, 읽은 글자만 확인에 써요."),
    Faq(3, "영수증 검증이 계속 실패해요.", "밝은 곳에서 흔들리지 않게 찍고, 식당 이름과 날짜·시간이 모두 보이게 해 주세요. 24시간 안에 결제한 영수증만 인정돼요. 계속 실패하면 가게 간판 사진으로 인증할 수도 있어요."),
    Faq(4, "스탬프는 어떻게 적립되나요?", "방문 인증에 성공하면 그 가게의 업력만큼 세월이 쌓이고 도장이 바로 찍혀요. 세월이 쌓이면 칭호가 올라가고, 한 지역에서 5곳을 방문하면 지역 스탬프를 얻어요."),
    Faq(4, "한 가게를 또 방문해도 도장이 찍히나요?", "같은 가게는 도장이 한 번만 찍히고 방문 횟수만 늘어요. 하루에 인증할 수 있는 가게 수에도 한도가 있어요."),
    Faq(2, "계정 정보를 변경하고 싶어요.", "마이페이지 → 설정 → 프로필 관리에서 닉네임과 프로필 사진을 바꿀 수 있어요. 비밀번호는 로그인 화면의 [비밀번호 찾기]로 다시 설정해요."),
    Faq(2, "로그인이 되지 않아요.", "이메일과 비밀번호를 다시 확인해 주세요. 비밀번호가 기억나지 않으면 [비밀번호 찾기]를 눌러 재설정 메일을 받을 수 있어요."),
    Faq(5, "가게 정보(영업시간, 전화번호)가 달라요.", "가게 상세 화면 아래의 [정보가 달라요? 제보하기]를 눌러 알려 주세요. 확인 후 반영해요."),
    Faq(5, "내 주변 노포가 지도에 안 나와요.", "위치 권한이 허용돼 있는지 확인하고, 지도 화면의 내 위치 버튼을 눌러 보세요. 칩이나 검색어로 지역을 좁혀 둔 상태라면 [전체]로 바꿔 주세요."),
    Faq(0, "AI 추천과 룰렛은 어떻게 쓰나요?", "홈의 [오늘의 AI 추천]과 컬렉션의 [AI 룰렛]에서 쓸 수 있어요. 처음 쓸 때 AI 기능 사용 동의를 받아요."),
    Faq(0, "내 정보는 안전한가요?", "위치 좌표와 영수증 내용은 서버에 저장하지 않아요. 자세한 내용은 설정의 개인정보 안내에서 볼 수 있어요."),
)

private fun mail(context: android.content.Context, email: String?) {
    val body = "\n\n---\n앱 버전 ${BuildConfig.VERSION_NAME}" + (if (!email.isNullOrBlank()) "\n계정 $email" else "")
    val send = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")).putExtra(Intent.EXTRA_SUBJECT, "[노포 패스포트] 문의").putExtra(Intent.EXTRA_TEXT, body)
    runCatching { context.startActivity(send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** 시안 23 "도움말 센터": 검색, 문의 분류, 자주 묻는 질문. 아래는 이메일 문의만 둔다(채팅 문의는 뺐다). */
@Composable
fun HelpCenterScreen(email: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableIntStateOf(-1) }
    var open by rememberSaveable { mutableIntStateOf(0) }
    var all by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    val filtered = Faqs.filter { f -> (cat < 0 || f.cat == cat) && (query.isBlank() || f.q.contains(query.trim()) || f.a.contains(query.trim())) }
    val shown = if (all || query.isNotBlank() || cat >= 0) filtered else filtered.take(5)
    val extra = if (open in shown.indices) 90 else 0
    val listH = 70 * shown.size + 40 + extra
    Column(Modifier.fillMaxSize().background(Color(0xFFFBF6EB)).statusBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 940 + listH + 170) {
            MockPic(R.drawable.mk2_n_help_head, 760, 440, Modifier.at(0, 84))
            Box(Modifier.at(670, 98).size(px(70), px(70)).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "닫기" })
            MockInput(query, { query = it }, "궁금한 내용을 검색해보세요.", Modifier.at(100, 452).size(px(430), px(44)), sizePx = 22)
            Cats.forEachIndexed { i, c ->
                val x = if (i % 2 == 0) 22 else 388
                val y = 520 + (i / 2) * 118
                val sel = cat == i
                Row(
                    Modifier.at(x, y).size(px(356), px(106)).clip(RoundedCornerShape(px(18))).background(if (sel) Color(0xFFF0F5D0) else CardWhite)
                        .border(if (sel) 2.dp else 1.dp, if (sel) MockColors.Lime2 else Hairline, RoundedCornerShape(px(18)))
                        .clickable(role = Role.Button) { if (i == 0) mail(context, email) else { cat = if (sel) -1 else i; open = 0 } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.padding(start = px(14)).size(px(78))) { MockPic(c.icon, 78, 78) }
                    Column(Modifier.weight(1f).padding(start = px(14))) {
                        MockText(c.title, 24, weight = FontWeight.ExtraBold)
                        MockText(c.sub, 16, color = Gray, maxLines = 1)
                    }
                    ChevronRight(24); Box(Modifier.width(px(12)))
                }
            }
            MockText(if (cat >= 0) "${Cats[cat].title} 질문" else "자주 묻는 질문", 31, Modifier.at(25, 884), weight = FontWeight.ExtraBold, font = MockSerif)
            Box(Modifier.at(560, 880).size(px(180), px(50)).clickable(role = Role.Button) { all = !all }, contentAlignment = Alignment.CenterEnd) {
                Row(verticalAlignment = Alignment.CenterVertically) { MockText(if (all) "접기" else "전체보기", 22); Box(Modifier.width(px(8))); ChevronRight(24) }
            }
            Column(
                Modifier.at(22, 940).size(px(716), px(listH)).clip(RoundedCornerShape(px(20))).background(CardWhite).border(1.dp, Hairline, RoundedCornerShape(px(20))).padding(horizontal = px(18), vertical = px(10)),
            ) {
                if (shown.isEmpty()) MockText(stringResource(com.nopo.cheonnyeon.R.string.help_01), 22, Modifier.padding(px(20)), color = Gray)
                shown.forEachIndexed { i, f ->
                    val isOpen = open == i
                    Row(Modifier.fillMaxWidth().height(px(66)).clickable(role = Role.Button) { open = if (isOpen) -1 else i }, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(px(38)).clip(CircleShape).background(MockColors.Lime2), contentAlignment = Alignment.Center) { MockText("Q", 22, weight = FontWeight.ExtraBold) }
                        MockText(f.q, 24, Modifier.weight(1f).padding(horizontal = px(16)), weight = FontWeight.ExtraBold)
                        Box(Modifier.rotate(if (isOpen) 180f else 0f)) { ChevronDown(24) }
                    }
                    if (isOpen) MockText(f.a, 20, Modifier.padding(start = px(54), bottom = px(10), end = px(10)).height(px(90)), color = Gray, maxLines = 3, lineHeightPx = 29)
                    if (i < shown.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                }
            }
            Row(
                Modifier.at(22, 940 + listH + 24).size(px(716), px(112)).clip(RoundedCornerShape(px(22))).background(Color(0xFFF3EDDA)).border(1.dp, Hairline, RoundedCornerShape(px(22)))
                    .clickable(role = Role.Button) { mail(context, email) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.padding(start = px(26)).size(px(60))) { EnvelopeGlyph() }
                Column(Modifier.weight(1f).padding(start = px(22))) {
                    MockText(stringResource(com.nopo.cheonnyeon.R.string.help_02), 27, weight = FontWeight.ExtraBold)
                    MockText(stringResource(com.nopo.cheonnyeon.R.string.help_03), 20, color = Gray)
                }
                ChevronRight(28); Box(Modifier.width(px(24)))
            }
        }
    }
}

@Composable
private fun EnvelopeGlyph() {
    Canvas(Modifier.size(px(60))) {
        val w = size.width
        val st = Stroke(w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(MockColors.Navy, Offset(w * 0.04f, w * 0.16f), Size(w * 0.92f, w * 0.68f), CornerRadius(w * 0.08f), style = st)
        val p = Path().apply { moveTo(w * 0.08f, w * 0.24f); lineTo(w * 0.5f, w * 0.56f); lineTo(w * 0.92f, w * 0.24f) }
        drawPath(p, MockColors.Navy, style = st)
    }
}
