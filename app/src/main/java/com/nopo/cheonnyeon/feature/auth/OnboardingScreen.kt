package com.nopo.cheonnyeon.feature.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.mock.AiSparkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.BookmarkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.LocalMockOriginY
import com.nopo.cheonnyeon.core.designsystem.mock.LocalMockU
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import com.nopo.cheonnyeon.core.designsystem.mock.MockLimeBar
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px

private data class Feature(val title: String, val body: String, val icon: Int?, val kind: Int, val ill: Int?)

private val Pages = listOf(
    listOf(
        Feature("진짜 노포를 발견하는 맛집 탐험", "세월이 만든 맛, 지금도 사랑받는\n진짜 노포를 찾아보세요.", R.drawable.mk2_ob_ic1, 0, R.drawable.mk2_ob_ill1),
        Feature("방문할 때마다 쌓이는 스탬프", "노포를 방문하고 나만의 패스포트에\n소중한 추억을 기록하세요.", R.drawable.mk2_ob_ic2, 0, R.drawable.mk2_ob_ill2),
        Feature("GPS & 영수증 인증으로 확실한 방문", "위치 확인과 영수증 인증으로\n다녀온 노포를 안전하게 기록할 수 있어요.", R.drawable.mk2_ob_ic3, 0, R.drawable.mk2_ob_ill3),
    ),
    listOf(
        Feature("AI가 골라 주는 오늘의 노포", "AI 추천과 룰렛으로 가볼 만한\n노포를 바로 찾아 드려요.", null, 1, R.drawable.mk2_ob_ill1),
        Feature("한 장씩 넘겨 보는 나만의 여권", "방문한 노포의 도장과 지역 스탬프를\n여권에 차곡차곡 모아 보세요.", R.drawable.mk2_ob_ic2, 0, R.drawable.mk2_ob_ill2),
        Feature("지도로 찾는 내 주변 노포", "현재 위치에서 가까운 노포를\n사진과 평점으로 한눈에 확인해요.", null, 2, null),
    ),
    listOf(
        Feature("칭호를 모아 미식가가 되세요", "세월이 쌓일수록 칭호가 올라가요.\n천 년을 모아 '천년 미식가'에 도전해요.", R.drawable.mk2_ob_ic2, 0, null),
        Feature("리뷰를 남기고 저장해 두세요", "다녀온 노포에 별점과 사진을 남기고\n가고 싶은 노포는 저장해 두세요.", null, 3, R.drawable.mk2_ob_ill3),
        Feature("내 정보는 안전하게", "위치 좌표와 영수증 내용은\n서버에 저장하지 않아요.", R.drawable.mk2_ob_ic3, 0, null),
    ),
)

/** 시안 "처음 설치 화면"(3.webp) 복제. 카드 영역을 좌우로 넘기면 앱 기능 설명이 이어진다. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { Pages.size }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().background(MockColors.Cream)) {
        // 화면이 시안보다 짧으면 그림(히어로)을 세로로 조금 눌러 한 화면에 들어오게 한다
        val bottomInset = WindowInsets.Companion.navigationBars.asPaddingValues().calculateBottomPadding()
        val total = (1651 - ((maxHeight - bottomInset).value / LocalMockU.current).toInt()).coerceIn(0, 260)
        val rest = minOf(total / 3, 50)   // 카드 줄 간격·버튼 간격에서 줄이는 몫
        val cut = total - rest            // 히어로 그림에서 줄이는 몫
        val rowGap = 150 - rest / 2
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
            MockStage(originY = 0, heightPx = 1651 - total) {
                MockPic(R.drawable.mk2_ob_hero, 760, 850 - cut, Modifier.at(0, 80))
                androidx.compose.runtime.CompositionLocalProvider(LocalMockOriginY provides cut) {
                // 카드
                Box(Modifier.at(30, 925).size(px(715), px(726)).clip(RoundedCornerShape(topStart = px(38), topEnd = px(38))).background(Color(0xFFFBF6EB)))
                HorizontalPager(pager, Modifier.at(30, 930).size(px(715), px(470))) { page ->
                    androidx.compose.runtime.CompositionLocalProvider(LocalMockOriginY provides 0) {
                    Box(Modifier.fillMaxSize()) {
                        Pages[page].forEachIndexed { i, f ->
                            val top = i * rowGap
                            if (f.icon != null) MockPic(f.icon, 102, 104, Modifier.at(20, 22 + top))
                            else Box(Modifier.at(20, 22 + top).size(px(100)).clip(CircleShape).background(Color(0xFFEFEBDD)), contentAlignment = Alignment.Center) {
                                when (f.kind) {
                                    1 -> AiSparkIcon(52)
                                    2 -> PinIcon(50)
                                    else -> BookmarkIcon(48, color = MockColors.Navy, filled = true)
                                }
                            }
                            MockText(f.title, 25, Modifier.at(153, 36 + top).width(px(400)), weight = FontWeight.ExtraBold)
                            MockText(f.body, 21, Modifier.at(153, 70 + top).width(px(420)), color = Color(0xFF4B5563), maxLines = 2, lineHeightPx = 28)
                            if (f.ill != null) MockPic(f.ill, if (f.ill == R.drawable.mk2_ob_ill1) 190 else if (f.ill == R.drawable.mk2_ob_ill2) 175 else 145, if (f.ill == R.drawable.mk2_ob_ill1) 145 else if (f.ill == R.drawable.mk2_ob_ill2) 135 else 145, Modifier.at(if (f.ill == R.drawable.mk2_ob_ill1) 505 else if (f.ill == R.drawable.mk2_ob_ill2) 520 else 550, 14 + top))
                            if (i < 2) Box(Modifier.at(22, rowGap + 5 + top).size(px(660), 1.dp).background(Color(0xFFE6E0CC)))
                        }
                    }
                    }
                }
                // 점
                repeat(Pages.size) { i ->
                    val on = pager.currentPage == i
                    Box(Modifier.at(341 + i * 31 + (if (on) 0 else 2), (if (on) 1405 else 1407) - rest).size(px(if (on) 18 else 14)).clip(CircleShape).background(if (on) MockColors.Lime else Color(0xFFCFCCBE)))
                }
                MockLimeBar("시작하기", 100, Modifier.at(40, 1450 - rest).size(px(682), px(100)), textPx = 32, onClick = onDone)
                Box(Modifier.at(250, 1566 - rest).size(px(260), px(56)).clickable(role = Role.Button, onClick = onDone), contentAlignment = Alignment.Center) {
                    MockText("둘러보기", 27, weight = FontWeight.Medium, align = TextAlign.Center)
                    Box(Modifier.align(Alignment.BottomCenter).width(px(104)).height(1.5.dp).background(MockColors.Navy))
                }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding())
        }
    }
}
