package com.nopo.cheonnyeon.feature.store

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.common.message
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
import com.nopo.cheonnyeon.domain.rule.ReviewRules
import com.nopo.cheonnyeon.feature.common.StoreCards
import com.nopo.cheonnyeon.feature.main.MockNavBar
import com.nopo.cheonnyeon.platform.camera.ReviewPhotoCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Hairline = Color(0xFFE2DDCB)
private val Gray = Color(0xFF4B5563)
private val CardWhite = Color(0xFFFCFAF4)
private val Tags = listOf("분위기", "가성비", "친절함", "맛", "재방문 의사")
private val RatingWords = mapOf(1 to "아쉬워요", 2 to "별로예요", 3 to "보통이에요", 4 to "아주 좋아요!", 5 to "최고예요!")

/** 좋았던 점 칩을 리뷰 글 끝에 "#분위기 #맛" 한 줄로 붙인다 (서버 리뷰 문서 구조는 그대로 두기 위해) */
internal fun withTags(text: String, tags: List<String>): String {
    val t = text.trim()
    if (tags.isEmpty()) return t
    val line = tags.joinToString(" ") { "#" + it.replace(" ", "") }
    return if (t.isEmpty()) line else "$t\n$line"
}

/** 시안 22 "방문 후기 작성": 별점·좋았던 점·사진을 담아 후기를 등록한다. 하단 탭은 앱의 5탭. */
@Composable
fun ReviewWriteScreen(vm: ReviewsViewModel, store: Store, onBack: () -> Unit, onOpenStore: () -> Unit, onTab: (Int) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ratings by vm.ratingsFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var text by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var photos by remember { mutableStateOf(vm.draftPhotos.orEmpty()) }
    var capturing by rememberSaveable { mutableStateOf(false) }
    var pickMenu by rememberSaveable { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val rate = ratings[store.id]
    val room = ReviewRules.MAX_PHOTOS - photos.size
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(ReviewRules.MAX_PHOTOS)) { uris ->
        val chosen = uris.take(ReviewRules.MAX_PHOTOS - photos.size)
        if (chosen.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val added = withContext(Dispatchers.Default) { chosen.mapNotNull { ReviewPhotoCodec.fromUri(context, it) } }
            failed = added.size < chosen.size
            photos = (photos + added).take(ReviewRules.MAX_PHOTOS); vm.draftPhotos = photos
        }
    }
    val album = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val full = withTags(text, tags)
    val valid = rating in 1..5 && com.nopo.cheonnyeon.domain.rule.ReviewRules.validate(rating, full) is com.nopo.cheonnyeon.domain.rule.ReviewCheck.Valid
    androidx.activity.compose.BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(Color(0xFFFBF6EB))) {
        Column(Modifier.weight(1f).statusBarsPadding().verticalScroll(rememberScrollState())) {
            MockStage(originY = 84, heightPx = 1470) {
                MockPic(R.drawable.mk2_n_rev_head, 760, 346, Modifier.at(0, 84))
                Box(Modifier.at(15, 218).size(px(60), px(52)).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "뒤로" })
                MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_01), 50, Modifier.at(28, 275), font = MockSerif, weight = FontWeight.ExtraBold)
                // 가게 카드
                Box(Modifier.at(18, 430).size(px(724), px(228)).clip(RoundedCornerShape(px(22))).background(CardWhite).clickable(role = Role.Button, onClick = onOpenStore)) {
                    Box(Modifier.size(px(330), px(228)).clip(RoundedCornerShape(topStart = px(22), bottomStart = px(22)))) { StorePhoto(store, Modifier.fillMaxSize()) }
                    MockText(store.name, 31, Modifier.padding(start = px(358), top = px(14)).width(px(300)), font = MockSerif, weight = FontWeight.ExtraBold)
                    Box(Modifier.padding(start = px(690), top = px(22))) { ChevronRight(26) }
                    Row(Modifier.padding(start = px(358), top = px(70)), verticalAlignment = Alignment.CenterVertically) {
                        PinIcon(24); Box(Modifier.width(px(8))); MockText(store.address.ifBlank { listOf(store.sido, store.sigungu).joinToString(" ") }, 20, Modifier.width(px(330)), color = Gray)
                    }
                    Row(Modifier.padding(start = px(358), top = px(116)), horizontalArrangement = Arrangement.spacedBy(px(10))) {
                        listOfNotNull(com.nopo.cheonnyeon.domain.rule.RoulettePicker.cuisineOf(store), store.foundedYear?.let { "${java.time.LocalDate.now().year - it}년 전통" }, store.menuItems.firstOrNull { it.isNotBlank() }).forEach { t ->
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFEBDD)).padding(horizontal = px(18), vertical = px(5))) { MockText(t, 19, color = Gray) }
                        }
                    }
                    Row(Modifier.padding(start = px(358), top = px(170)), verticalAlignment = Alignment.CenterVertically) {
                        if (rate != null) {
                            StarIcon(26); Box(Modifier.width(px(8))); MockText("%.1f".format(rate.average), 22, weight = FontWeight.Bold); MockText(" (%,d)".format(rate.count), 19, color = Gray)
                            Box(Modifier.padding(horizontal = px(14)).width(1.dp).height(px(28)).background(Hairline))
                        }
                        MockPic(R.drawable.mk2_n_rev_ic_person, 32, 40)
                        Box(Modifier.width(px(8)))
                        // "몇 명 방문" 이 아니라 "몇 명 방문 인증"
                        MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_02).format(state.stats?.visitors ?: 0), 20, color = Gray)
                    }
                }

                // 별점
                MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_03), 28, Modifier.at(25, 678), weight = FontWeight.ExtraBold)
                Box(Modifier.at(540, 676).clip(RoundedCornerShape(50)).background(Color(0xFFEFEBDD)).padding(horizontal = px(20), vertical = px(6))) { MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_04), 19, color = Gray) }
                Row(Modifier.at(22, 722), horizontalArrangement = Arrangement.spacedBy(px(14))) {
                    (1..5).forEach { i ->
                        Box(Modifier.size(px(66)).clickable(role = Role.Button) { rating = i }.semantics { contentDescription = "${i}점" }, contentAlignment = Alignment.Center) {
                            FlatStar(60, if (i <= rating) MockColors.Star else Color(0xFFE6E4DC))
                        }
                    }
                }
                if (rating > 0) {
                    MockText("$rating.0", 46, Modifier.at(500, 724).width(px(110)), font = MockSerif, weight = FontWeight.ExtraBold, align = TextAlign.End)
                    MockText(RatingWords.getValue(rating), 20, Modifier.at(616, 750).width(px(130)), color = Gray)
                }

                // 글
                MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_05), 28, Modifier.at(25, 818), weight = FontWeight.ExtraBold)
                Box(Modifier.at(22, 862).size(px(716), px(138)).clip(RoundedCornerShape(px(18))).background(CardWhite).border(1.dp, Hairline, RoundedCornerShape(px(18)))) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = text, onValueChange = { text = it.take(ReviewRules.TEXT_MAX) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = com.nopo.cheonnyeon.core.designsystem.mock.pxSp(21), color = MockColors.Navy),
                        modifier = Modifier.fillMaxSize().padding(px(22)),
                    )
                    if (text.isEmpty()) MockText("예) 깊은 국물 맛이 정말 인상적이었어요.\n오랜 세월이 느껴지는 분위기와 친절한 사장님도 좋았습니다.\n다음에도 꼭 다시 방문하고 싶어요!", 20, Modifier.padding(px(22)).width(px(660)), color = Color(0xFF9AA0AB), maxLines = 3, lineHeightPx = 30)
                    MockText("${text.length}/500", 18, Modifier.align(Alignment.BottomEnd).padding(end = px(18), bottom = px(10)), color = Color(0xFF9AA0AB))
                }

                // 사진
                Row(Modifier.at(25, 1022), verticalAlignment = Alignment.Bottom) {
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_06), 28, weight = FontWeight.ExtraBold); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_07), 21, color = Gray)
                }
                MockText("최대 ${ReviewRules.MAX_PHOTOS}장까지 등록 가능", 19, Modifier.at(520, 1030).width(px(220)), color = Gray, align = TextAlign.End)
                Box(
                    Modifier.at(22, 1068).size(px(150), px(150)).clip(RoundedCornerShape(px(14))).background(Color(0xFFF3EFE3)).clickable(enabled = room > 0, role = Role.Button) { pickMenu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) { drawRoundRect(Color(0xFFCFC8B0), cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()), style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { PlusImageGlyph(); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_08), 20, color = Gray) }
                }
                photos.forEachIndexed { i, p ->
                    Box(Modifier.at(186 + i * 185, 1068).size(px(172), px(150)).clip(RoundedCornerShape(px(14)))) {
                        ReviewPhotoImage(p, Modifier.fillMaxSize())
                        Box(Modifier.align(Alignment.TopEnd).padding(px(8)).size(px(40)).clip(CircleShape).background(Color.White).clickable(role = Role.Button) { photos = photos.filterIndexed { k, _ -> k != i }; vm.draftPhotos = photos }.semantics { contentDescription = "사진 지우기" }, contentAlignment = Alignment.Center) {
                            MockText("✕", 20, color = MockColors.Navy, weight = FontWeight.Bold)
                        }
                    }
                }
                if (failed) MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_09), 18, Modifier.at(25, 1222), color = MockColors.Stamp)

                // 칩
                Row(Modifier.at(25, 1236), verticalAlignment = Alignment.Bottom) {
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_10), 28, weight = FontWeight.ExtraBold); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_11), 19, color = Gray)
                }
                Row(Modifier.at(18, 1286), horizontalArrangement = Arrangement.spacedBy(px(10))) {
                    Tags.forEach { t ->
                        val sel = t in tags
                        Box(
                            Modifier.height(px(58)).clip(RoundedCornerShape(50)).background(if (sel) MockColors.Lime2 else CardWhite).border(1.dp, if (sel) MockColors.Lime2 else Hairline, RoundedCornerShape(50))
                                .clickable(role = Role.Checkbox) { tags = if (sel) tags - t else tags + t }.padding(horizontal = px(18)),
                            contentAlignment = Alignment.Center,
                        ) { MockText((if (sel) "✔ " else "") + t, 21, weight = if (sel) FontWeight.Bold else FontWeight.Medium, maxLines = 1) }
                    }
                }
                MockPic(R.drawable.mk2_n_rev_banner, 720, 130, Modifier.at(20, 1362))
                state.error?.let { MockText(it.message(context), 19, Modifier.at(25, 1500).width(px(700)), color = MockColors.Stamp, maxLines = 2) }
            }
        }
        // 등록 버튼
        Box(
            Modifier.padding(horizontal = px(16), vertical = px(8)).height(px(84)).fillMaxWidth().clip(RoundedCornerShape(50)).background(if (valid && !state.saving) MockColors.Lime2 else Color(0xFFE2E8C4))
                .clickable(enabled = valid && !state.saving, role = Role.Button) {
                    vm.save(null, rating, full, photos) { vm.draftPhotos = null; onOpenStore() }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PencilGlyph(); Box(Modifier.width(px(18))); MockText(if (state.saving) "등록 중…" else "후기 등록하기", 31, weight = FontWeight.ExtraBold)
            }
        }
        MockNavBar(0, onTab)
    }
    if (capturing) ReviewPhotoCapture(onCaptured = { photos = (photos + it).take(ReviewRules.MAX_PHOTOS); vm.draftPhotos = photos; capturing = false }, onDismiss = { capturing = false })
    if (pickMenu) AlertDialog(
        onDismissRequest = { pickMenu = false }, title = { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_08)) },
        text = { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_12)) },
        confirmButton = {
            TextButton({
                pickMenu = false
                if (com.nopo.cheonnyeon.feature.permission.albumGranted(context)) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                else album.launch(com.nopo.cheonnyeon.feature.permission.AlbumPermission)
            }) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_13)) }
        },
        dismissButton = { TextButton({ pickMenu = false; capturing = true }) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.rev_14)) } },
    )
}

@Composable
private fun PlusImageGlyph() {
    Canvas(Modifier.size(px(46))) {
        val w = size.width
        val st = Stroke(w * 0.07f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        drawRoundRect(Color(0xFF4B5563), Offset(w * 0.06f, w * 0.18f), androidx.compose.ui.geometry.Size(w * 0.76f, w * 0.68f), androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = st)
        drawLine(Color(0xFF4B5563), Offset(w * 0.8f, w * 0.04f), Offset(w * 0.8f, w * 0.26f), w * 0.07f, StrokeCap.Round)
        drawLine(Color(0xFF4B5563), Offset(w * 0.69f, w * 0.15f), Offset(w * 0.91f, w * 0.15f), w * 0.07f, StrokeCap.Round)
    }
}

@Composable
private fun PencilGlyph() {
    Canvas(Modifier.size(px(40))) {
        val w = size.width
        val st = Stroke(w * 0.09f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.15f, w * 0.85f); lineTo(w * 0.22f, w * 0.62f); lineTo(w * 0.68f, w * 0.16f); lineTo(w * 0.84f, w * 0.32f); lineTo(w * 0.38f, w * 0.78f); close() }
        drawPath(p, MockColors.Navy, style = st)
    }
}

/** 외곽선 없는 납작한 별 (후기 별점) */
@Composable
private fun FlatStar(sizePx: Int, color: Color) {
    Canvas(Modifier.size(px(sizePx))) {
        val r = size.minDimension / 2
        val c = Offset(size.width / 2, size.height / 2 + r * 0.06f)
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.5f
            val a = -Math.PI / 2 + i * Math.PI / 5
            val p = Offset(c.x + (rad * Math.cos(a)).toFloat(), c.y + (rad * Math.sin(a)).toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, color)
        drawPath(path, color, style = Stroke(r * 0.18f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
