package com.nopo.cheonnyeon.feature.my

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.mock.BookmarkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.saveDescription
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.rule.AuthValidation
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import com.nopo.cheonnyeon.feature.common.StoreCards
import com.nopo.cheonnyeon.feature.explore.StoreCard
import com.nopo.cheonnyeon.feature.passport.PassportViewModel
import com.nopo.cheonnyeon.feature.store.StorePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate

/** 고객센터 문의 주소 (개발자 연락처). 운영 주소가 정해지면 이 값만 바꾼다. */
const val SUPPORT_EMAIL = "wow.sillyer@gmail.com"

class MyViewModel(private val container: AppContainer) : ViewModel() {
    private val catalog = kotlinx.coroutines.flow.MutableStateFlow<List<Store>>(emptyList())

    init {
        viewModelScope.launch { catalog.value = container.catalog.all().filter { !it.isTest } }
    }

    /** 저장한 노포 (저장한 순서는 기기에 남기지 않아 이름순) */
    val saved: StateFlow<List<Store>> = combine(container.settings.bookmarks, catalog) { ids, all ->
        all.filter { it.id in ids }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarks = container.settings.bookmarks
    val settings = container.settings
    fun toggleBookmark(id: String) { viewModelScope.launch { container.settings.toggleBookmark(id) } }

    /** 프로필 사진(이 기기에만 저장). 바뀔 때마다 올라가는 번호로 화면이 다시 그린다 */
    val photoFile = File(container.appContext.filesDir, "profile.jpg")
    private val _photoVersion = kotlinx.coroutines.flow.MutableStateFlow(if (photoFile.exists()) photoFile.lastModified() else 0L)
    val photoVersion: StateFlow<Long> = _photoVersion

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = container.appContext.contentResolver
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
                    val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                    if (bmp != null) photoFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                }
            }
            _photoVersion.value = if (photoFile.exists()) photoFile.lastModified() else 0L
        }
    }

    suspend fun changeNickname(nick: String) = container.auth.changeNickname(nick)
    val email: String? get() = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email }.getOrNull()
}

private enum class Sub { NONE, SAVED, NOTIFY, PREFS }

/**
 * 시안 "마이"(2.webp) 복제. 사진 자리는 이 기기에서 고른 프로필 사진, 방문 기록은 컬렉션 탭으로,
 * 알림 설정·앱 환경 설정·고객센터는 여기서 바로 열린다. 계정 관리는 기존 설정 화면.
 */
@Composable
fun MyScreen(
    vm: MyViewModel, passportVm: PassportViewModel,
    onOpenStore: (String) -> Unit, onOpenProfile: (String) -> Unit, onOpenCollection: () -> Unit, onOpenSettings: () -> Unit,
    onBell: () -> Unit = {}, onSearch: () -> Unit = {},
) {
    val state by passportVm.state.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle(initialValue = emptySet())
    val photoVersion by vm.photoVersion.collectAsStateWithLifecycle()
    var nickRefresh by remember { mutableIntStateOf(0) }
    val nickname by produceState("", nickRefresh) { value = passportVm.nickname() }
    var sub by rememberSaveable { mutableStateOf(Sub.NONE) }
    var editOpen by rememberSaveable { mutableStateOf(false) }
    var supportOpen by rememberSaveable { mutableStateOf(false) }
    val real = remember(state.stamps) { state.stamps.filter { !it.isTest && it.store != null } }
    val visited = remember(real) { real.sortedByDescending { it.visit.lastVisitedAt } }
    val title = TitlePolicy.titleFor(state.totalYears)
    val goal = com.nopo.cheonnyeon.domain.rule.CollectionRules.goal(real.size)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.setPhoto(uri) }
    BackHandler(enabled = sub != Sub.NONE) { sub = Sub.NONE }

    when (sub) {
        Sub.SAVED -> { SavedScreen(vm, saved, bookmarks, onBack = { sub = Sub.NONE }, onOpenStore = onOpenStore); return }
        Sub.NOTIFY -> { NotifySettingsScreen(vm, onBack = { sub = Sub.NONE }); return }
        Sub.PREFS -> { AppPrefsScreen(vm, onBack = { sub = Sub.NONE }); return }
        Sub.NONE -> Unit
    }

    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1560) {
            MockPic(R.drawable.mk_deco_my, 330, 160, Modifier.at(430, 150))
            MockPic(R.drawable.mk_hdr_logo, 432, 122, Modifier.at(20, 84))
            MockPic(R.drawable.mk_ic_search, 54, 52, Modifier.at(604, 90), onClick = onSearch)
            MockPic(R.drawable.mk_ic_bell, 54, 56, Modifier.at(682, 88), onClick = onBell)

            // 프로필
            Box(Modifier.at(28, 238).size(px(176)).clip(CircleShape).background(Color(0xFFECE7D4)).border(2.dp, Color(0xFFE3DCC4), CircleShape).clickable(role = Role.Button) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                if (photoVersion != 0L) {
                    coil3.compose.AsyncImage(
                        model = coil3.request.ImageRequest.Builder(LocalContext.current).data(vm.photoFile).memoryCacheKey("profile-$photoVersion").build(),
                        contentDescription = "프로필 사진", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    )
                } else DefaultAvatar()
            }
            Box(Modifier.at(150, 360).size(px(54)).clip(CircleShape).background(Color(0xFF3A3F47)).clickable(role = Role.Button) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) { CameraIcon() }
            Row(Modifier.at(222, 268).clickable(role = Role.Button) { editOpen = true }, verticalAlignment = Alignment.CenterVertically) {
                MockText(nickname.ifBlank { "노포여행자" }, 46, Modifier.widthIn(max = px(300)), font = MockSerif, weight = FontWeight.ExtraBold)
                ChevronRight(30)
            }
            MockPic(R.drawable.mk_my_tagline, 248, 64, Modifier.at(226, 338))
            MockPic(R.drawable.mk_my_edit, 190, 62, Modifier.at(556, 348), onClick = { editOpen = true })

            // 나의 노포 여정
            MockPic(R.drawable.mk_my_journey, 736, 206, Modifier.at(12, 436))
            Box(Modifier.at(190, 436).size(px(330), px(206)).clickable(role = Role.Button, onClick = onOpenCollection))
            Row(Modifier.at(197, 517), verticalAlignment = Alignment.Bottom) {
                MockText("${real.size}", 52, font = MockSerif, weight = FontWeight.ExtraBold)
                MockText(" / $goal", 28, color = MockColors.Ink2, font = MockSerif)
            }
            MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.col_06), 22, Modifier.at(197, 580), color = MockColors.Ink2, weight = FontWeight.Medium)
            Row(Modifier.at(398, 517), verticalAlignment = Alignment.Bottom) {
                MockText("${title.ordinal + 1}", 52, font = MockSerif, weight = FontWeight.ExtraBold)
                MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.book_02), 26, color = MockColors.Ink2, font = MockSerif)
            }
            MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.book_01), 22, Modifier.at(398, 580), color = MockColors.Ink2, weight = FontWeight.Medium)

            // 메뉴 카드 6장
            Card(R.drawable.mk_my_card_saved, 12, 655, 362, 207) { sub = Sub.SAVED }
            RealPhoto(saved.firstOrNull(), 224, 662)
            Card(R.drawable.mk_my_card_history, 385, 655, 363, 207, onOpenCollection)
            RealPhoto(visited.firstOrNull()?.store, 596, 662)
            Card(R.drawable.mk_my_card_alarm, 12, 870, 362, 208) { sub = Sub.NOTIFY }
            Card(R.drawable.mk_my_card_prefs, 385, 870, 363, 208) { sub = Sub.PREFS }
            Card(R.drawable.mk_my_card_account, 12, 1086, 362, 206, onOpenSettings)
            Card(R.drawable.mk_my_card_support, 385, 1086, 363, 206) { supportOpen = true }

            // 최근 방문한 노포
            MockPic(R.drawable.mk_my_title_recent, 246, 50, Modifier.at(14, 1298))
            MockPic(R.drawable.mk_my_all, 108, 46, Modifier.at(640, 1300), onClick = onOpenCollection)
            if (visited.isEmpty()) {
                Box(Modifier.at(15, 1360).size(px(730), px(150)).clip(RoundedCornerShape(px(18))).background(MockColors.Card).border(1.dp, MockColors.CardLine, RoundedCornerShape(px(18))), contentAlignment = Alignment.Center) {
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_01), 22, color = MockColors.Hint)
                }
            } else {
                LazyRow(Modifier.at(0, 1352).fillMaxWidth().height(px(172)), contentPadding = PaddingValues(horizontal = px(15)), horizontalArrangement = Arrangement.spacedBy(px(12))) {
                    items(visited.take(12), key = { it.visit.storeId }) { s ->
                        val store = s.store ?: return@items
                        val date = Instant.ofEpochMilli(s.visit.lastVisitedAt).atZone(SeoulZone).toLocalDate()
                        Box(Modifier.width(px(245)).height(px(170)).clip(RoundedCornerShape(px(14))).clickable(role = Role.Button) { onOpenStore(store.id) }) {
                            StorePhoto(store, Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000)), startY = 100f)))
                            Box(Modifier.align(Alignment.TopEnd).padding(top = px(10), end = px(10)).size(px(36)).saveDescription().clickable(role = Role.Button) { vm.toggleBookmark(store.id) }, contentAlignment = Alignment.Center) {
                                BookmarkIcon(28, filled = store.id in bookmarks)
                            }
                            Column(Modifier.align(Alignment.BottomStart).padding(start = px(14), bottom = px(10)), verticalArrangement = Arrangement.spacedBy(px(2))) {
                                Box(Modifier.clip(RoundedCornerShape(50)).background(MockColors.Lime).padding(horizontal = px(12), vertical = px(3))) {
                                    MockText("%d. %02d. %02d".format(date.year, date.monthValue, date.dayOfMonth), 17, weight = FontWeight.Medium)
                                }
                                MockText(store.name, 25, Modifier.width(px(220)), color = Color.White, font = MockSerif, weight = FontWeight.ExtraBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PinIcon(17, color = Color.White); Box(Modifier.width(px(5)))
                                    MockText(listOf(store.sido, store.sigungu).filter { it.isNotBlank() }.joinToString(" "), 17, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editOpen) EditProfileDialog(vm, nickname, onDismiss = { editOpen = false }, onSaved = { nickRefresh++ }, onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
    if (supportOpen) SupportDialog(vm.email) { supportOpen = false }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Card(res: Int, x: Int, y: Int, w: Int, h: Int, onClick: () -> Unit) {
    MockPic(res, w, h, Modifier.at(x, y), onClick = onClick)
}

/** 메뉴 카드 안의 사진 자리에 실제 가게 사진을 올린다 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.RealPhoto(store: Store?, x: Int, y: Int) {
    Box(Modifier.at(x, y).size(px(139), px(186)).clip(RoundedCornerShape(px(12))).background(Color(0xFFEFEAD8))) {
        if (store != null) StorePhoto(store, Modifier.fillMaxSize())
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BookmarkIcon(60, color = Color(0xFFCFC8B0)) }
    }
}

@Composable
private fun DefaultAvatar() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        drawCircle(Color(0xFF061A31), radius = w * 0.17f, center = Offset(w * 0.5f, w * 0.38f))
        drawCircle(Color(0xFF061A31), radius = w * 0.34f, center = Offset(w * 0.5f, w * 1.0f))
    }
}

@Composable
private fun CameraIcon() {
    Canvas(Modifier.size(px(30))) {
        val w = size.width
        drawRoundRect(Color.White, topLeft = Offset(0f, w * 0.22f), size = androidx.compose.ui.geometry.Size(w, w * 0.7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f))
        drawCircle(Color(0xFF3A3F47), radius = w * 0.2f, center = Offset(w * 0.5f, w * 0.57f))
        drawCircle(Color.White, radius = w * 0.11f, center = Offset(w * 0.5f, w * 0.57f))
    }
}

@Composable
private fun EditProfileDialog(vm: MyViewModel, current: String, onDismiss: () -> Unit, onSaved: () -> Unit, onPickPhoto: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nick by rememberSaveable { mutableStateOf(current) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalid = AuthValidation.nickname(nick)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_02)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nick, onValueChange = { nick = it; error = null }, singleLine = true, label = { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_03)) },
                    supportingText = { Text(error ?: stringResourceSafe(R.string.auth_nickname_hint)) }, isError = error != null,
                )
                TextButton(onClick = { onPickPhoto() }) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_04)) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && invalid == null,
                onClick = {
                    if (nick == current) { onDismiss(); return@TextButton }
                    busy = true
                    scope.launch {
                        val r = vm.changeNickname(nick)
                        busy = false
                        if (r is AppResult.Success) { onSaved(); onDismiss() } else error = r.errorOrNull?.message(context)
                    }
                },
            ) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_05)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_06)) } },
    )
}

@Composable
private fun stringResourceSafe(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
internal fun SupportDialog(email: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_07)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_08))
                Text(SUPPORT_EMAIL, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                if (!email.isNullOrBlank()) Text("내 계정: $email", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
                    .putExtra(Intent.EXTRA_SUBJECT, "[노포 패스포트] 문의").putExtra(Intent.EXTRA_TEXT, "\n\n---\n앱 버전 ${BuildConfig.VERSION_NAME}" + (if (!email.isNullOrBlank()) "\n계정 $email" else ""))
                try { context.startActivity(send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: android.content.ActivityNotFoundException) { }
                onDismiss()
            }) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_09)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_06)) } },
    )
}

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = px(20), vertical = px(18)), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable(role = Role.Button, onClick = onBack).padding(px(10))) { MockText("‹ $title", 32, font = MockSerif, weight = FontWeight.ExtraBold) }
    }
}

/** 저장한 노포: 저장하기로 담은 가게 목록 */
@Composable
private fun SavedScreen(vm: MyViewModel, saved: List<Store>, bookmarks: Set<String>, onBack: () -> Unit, onOpenStore: (String) -> Unit) {
    val year = LocalDate.now(SeoulZone).year
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding()) {
        SubHeader("저장한 노포 ${saved.size}", onBack)
        if (saved.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(px(30)), contentAlignment = Alignment.Center) {
                MockText("아직 저장한 노포가 없어요.\n가게 화면에서 '저장하기'를 눌러 보세요.", 24, color = MockColors.Hint, maxLines = 3, lineHeightPx = 36)
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2), contentPadding = PaddingValues(px(19)),
                horizontalArrangement = Arrangement.spacedBy(px(14)), verticalArrangement = Arrangement.spacedBy(px(14)),
            ) {
                gridItems(saved, key = { it.id }) { s ->
                    com.nopo.cheonnyeon.feature.explore.ExploreCard(StoreCards.of(s, year, emptyMap(), null, bookmarks), onOpen = { onOpenStore(s.id) }, onBookmark = { vm.toggleBookmark(s.id) })
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(px(18))).background(MockColors.Card).border(1.dp, MockColors.CardLine, RoundedCornerShape(px(18))).padding(px(22)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(px(6))) {
            MockText(title, 27, weight = FontWeight.Bold)
            MockText(body, 21, color = MockColors.Hint, maxLines = 3, lineHeightPx = 29)
        }
        Switch(checked, onChange)
    }
}

/** 알림 설정 */
@Composable
internal fun NotifySettingsScreen(vm: MyViewModel, onBack: () -> Unit) {
    val news by vm.settings.notifyNews.collectAsStateWithLifecycle(initialValue = true)
    val reco by vm.settings.notifyReco.collectAsStateWithLifecycle(initialValue = true)
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding()) {
        SubHeader(stringResource(R.string.my_notify_settings), onBack)
        Column(Modifier.padding(horizontal = px(20)), verticalArrangement = Arrangement.spacedBy(px(16))) {
            ToggleRow(stringResource(R.string.my_t11), stringResource(R.string.my_b12), news) { v -> vm.viewModelScope.launch { vm.settings.setNotifyNews(v) } }
            ToggleRow(stringResource(R.string.my_t13), stringResource(R.string.my_b14), reco) { v -> vm.viewModelScope.launch { vm.settings.setNotifyRecoSafe(v) } }
            MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_10), 21, color = MockColors.Hint, maxLines = 2, lineHeightPx = 29)
        }
    }
}

private suspend fun com.nopo.cheonnyeon.data.local.SettingsStore.setNotifyRecoSafe(v: Boolean) = setNotifyReco(v)

/** 앱 환경 설정 */
@Composable
private fun AppPrefsScreen(vm: MyViewModel, onBack: () -> Unit) {
    val s = vm.settings
    val ai by s.aiConsent.collectAsStateWithLifecycle(initialValue = false)
    val food by s.foodOnly.collectAsStateWithLifecycle(initialValue = false)
    val loc by s.allowLocationOnly.collectAsStateWithLifecycle(initialValue = false)
    val demo by s.demoData.collectAsStateWithLifecycle(initialValue = true)
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding()) {
        SubHeader(stringResource(R.string.my_app_prefs), onBack)
        Column(Modifier.padding(horizontal = px(20)).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(px(16))) {
            ToggleRow(stringResource(R.string.my_t15), stringResource(R.string.my_b16), ai) { v -> vm.viewModelScope.launch { s.setAiConsent(v) } }
            ToggleRow(stringResource(R.string.my_t17), stringResource(R.string.my_b18), food) { v -> vm.viewModelScope.launch { s.setFoodOnly(v) } }
            ToggleRow(stringResource(R.string.my_t19), stringResource(R.string.my_b20), loc) { v -> vm.viewModelScope.launch { s.setAllowLocationOnly(v) } }
            ToggleRow(stringResource(R.string.my_t21), stringResource(R.string.my_b22), demo) { v -> vm.viewModelScope.launch { s.setDemoData(v) } }
            Column(Modifier.fillMaxWidth().padding(px(8)), verticalArrangement = Arrangement.spacedBy(px(6))) {
                MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.my_11), 21, color = MockColors.Hint)
                MockText("앱 버전 ${BuildConfig.VERSION_NAME}", 21, color = MockColors.Hint)
            }
        }
    }
}
