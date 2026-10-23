package com.nopo.cheonnyeon.feature.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.MockCheck
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockInput
import com.nopo.cheonnyeon.core.designsystem.mock.MockLimeBar
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.domain.rule.AuthInvalid
import com.nopo.cheonnyeon.domain.rule.AuthValidation

private val Hairline = Color(0xFFE2DDCB)
private val HintGray = Color(0xFF5F6877)
private val ErrorRed = Color(0xFFB4332A)
private val NaverGreen = Color(0xFF03C75A)

private fun AuthInvalid.text(): String = when (this) {
    AuthInvalid.EMAIL -> "이메일 형식이 맞지 않아요."
    AuthInvalid.PASSWORD_SHORT -> "비밀번호는 8자 이상이어야 해요."
    AuthInvalid.PASSWORD_WEAK -> "영문과 숫자를 모두 넣어 주세요."
    AuthInvalid.PASSWORD_MISMATCH -> "비밀번호가 서로 달라요."
    AuthInvalid.NICK_LENGTH -> "닉네임은 2~12자로 써 주세요."
    AuthInvalid.NICK_CHARS -> "한글·영문·숫자만 쓸 수 있어요."
}

/** 구글·카카오·네이버 간편 로그인은 각 서비스의 앱 키가 있어야 한다. 키가 준비되기 전에는 안내만 띄운다. */
@Composable
private fun SocialNotReadyDialog(provider: String?, onDismiss: () -> Unit) {
    if (provider == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${provider}로 시작하기") },
        text = { Text("${provider} 간편 로그인은 준비 중이에요. 지금은 이메일로 가입하고 로그인해 주세요.") },
        confirmButton = { TextButton(onDismiss) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_02)) } },
    )
}

@Composable
private fun NaverMark(sizePx: Int) {
    Box(Modifier.size(px(sizePx)).clip(RoundedCornerShape(px(sizePx / 5))).background(NaverGreen), contentAlignment = Alignment.Center) {
        MockText("N", (sizePx * 0.68f).toInt(), color = Color.White, weight = FontWeight.ExtraBold)
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var c = this
    while (c is android.content.ContextWrapper) { if (c is android.app.Activity) return c; c = c.baseContext }
    return null
}

/** 구글 로그인 한 번: 계정을 골라 ID 토큰을 받고 Firebase 로 로그인한다. 결과 안내 문구는 [onNote] 로 돌려준다. */
private suspend fun doGoogle(context: android.content.Context, vm: AuthViewModel, onNote: (String?) -> Unit) {
    val activity = context.findActivity() ?: return
    when (val r = com.nopo.cheonnyeon.platform.auth.GoogleSignIn.getIdToken(activity)) {
        is com.nopo.cheonnyeon.platform.auth.GoogleResult.Token -> { onNote(null); vm.googleSignIn(r.idToken) }
        com.nopo.cheonnyeon.platform.auth.GoogleResult.Cancelled -> onNote(null)
        com.nopo.cheonnyeon.platform.auth.GoogleResult.NotConfigured -> onNote("구글 로그인은 아직 설정 중이에요. 지금은 이메일로 로그인해 주세요.")
        com.nopo.cheonnyeon.platform.auth.GoogleResult.NoAccount -> onNote("이 기기에 구글 계정이 없어요. 설정에서 구글 계정을 추가해 주세요.")
        is com.nopo.cheonnyeon.platform.auth.GoogleResult.Failed -> onNote("구글 로그인에 실패했어요. 잠시 뒤 다시 시도해 주세요. (${r.reason})")
    }
}

/** 시안 28 "로그인": 이메일 로그인 + 구글로 시작하기 + 회원가입하기 (게스트 둘러보기는 뺐다) */
@Composable
fun LoginScreen(vm: AuthViewModel, onSignUp: () -> Unit, onReset: () -> Unit, onDiagnostics: (() -> Unit)? = null) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPw by rememberSaveable { mutableStateOf(false) }
    var keep by rememberSaveable { mutableStateOf(true) }
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    val ready = AuthValidation.email(email.trim()) == null && password.isNotEmpty() && !state.busy
    val submit = { if (ready) vm.signIn(email.trim(), password) }

    Box(Modifier.fillMaxSize().background(Color(0xFFFBF6EB))) {
        Box(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            androidx.compose.foundation.layout.Column {
                MockStage(originY = 0, heightPx = 1651) {
                    MockPic(R.drawable.mk2_n_login_head, 760, 801, Modifier.at(0, 84))
                    if (onDiagnostics != null) Box(Modifier.at(40, 250).size(px(420), px(170)).combinedClickable(onClick = {}, onLongClick = onDiagnostics))
                    MockPic(R.drawable.mk2_n_login_in1, 760, 92, Modifier.at(0, 870))
                    MockInput(email, { email = it }, "이메일 또는 휴대폰 번호를 입력하세요", Modifier.at(132, 897).size(px(520), px(46)), keyboard = KeyboardType.Email)
                    MockPic(R.drawable.mk2_n_login_in2, 760, 94, Modifier.at(0, 968))
                    MockInput(password, { password = it }, "비밀번호를 입력하세요", Modifier.at(132, 994).size(px(500), px(46)), password = true, visible = showPw, ime = ImeAction.Done, onDone = submit)
                    Box(Modifier.at(632, 985).size(px(110), px(72)).clickable(role = Role.Button) { showPw = !showPw }.semantics { contentDescription = "비밀번호 보기" })

                    MockCheck(keep, 34, Modifier.at(42, 1074)) { keep = it; vm.setKeepLogin(it) }
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.auth2_01), 23, Modifier.at(90, 1076), color = MockColors.Navy)
                    Box(Modifier.at(500, 1066).size(px(222), px(52)).clickable(role = Role.Button, onClick = onReset), contentAlignment = Alignment.CenterEnd) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.auth2_02), 23, weight = FontWeight.Medium); Box(Modifier.width(px(8))); ChevronRight(26)
                        }
                    }
                    MockLimeBar(if (state.busy) "로그인 중…" else "로그인  ›", 96, Modifier.at(40, 1130).size(px(682), px(96)), textPx = 32, enabled = ready, onClick = submit)
                    val msg = state.error?.message(context) ?: note
                    msg?.let { MockText(it, 20, Modifier.at(44, 1232).width(px(680)), color = ErrorRed, maxLines = 2) }

                    Box(Modifier.at(45, 1270).size(px(290), 1.dp).background(Hairline))
                    Box(Modifier.at(425, 1270).size(px(292), 1.dp).background(Hairline))
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.auth2_03), 21, Modifier.at(340, 1258).width(px(80)), color = HintGray, align = TextAlign.Center)

                    // 구글로 시작하기 (시안의 "간편하게 시작하기 / 휴대폰으로 시작하기" 자리)
                    Box(
                        Modifier.at(40, 1300).size(px(682), px(86)).clip(RoundedCornerShape(px(26))).background(Color(0xFFFCFAF4)).border(1.dp, Hairline, RoundedCornerShape(px(26)))
                            .clickable(enabled = !state.busy, role = Role.Button) { scope.launch { doGoogle(context, vm) { note = it } } },
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            MockPic(R.drawable.mk2_lg_ic_google, 46, 46); Box(Modifier.width(px(22))); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.auth2_04), 27, weight = FontWeight.ExtraBold)
                        }
                    }
                    Box(
                        Modifier.at(40, 1402).size(px(682), px(86)).clip(RoundedCornerShape(px(26))).border(2.dp, MockColors.Navy, RoundedCornerShape(px(26))).clickable(role = Role.Button, onClick = onSignUp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            MockPic(R.drawable.mk2_n_login_user, 47, 47); Box(Modifier.width(px(20))); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.auth2_05), 27, weight = FontWeight.ExtraBold); Box(Modifier.width(px(12))); ChevronRight(28)
                        }
                    }
                    MockPic(R.drawable.mk2_n_login_foot, 760, 161, Modifier.at(0, 1490))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

private val TermsText = "노포 패스포트 이용약관(요약)\n\n1. 이 앱은 공개된 백년가게·오래가게 자료를 정리해 보여 주는 서비스예요.\n2. 방문 인증은 본인이 실제로 방문해 결제한 영수증으로만 할 수 있어요. 타인의 영수증·조작된 영수증을 쓰면 인증이 취소될 수 있어요.\n3. 리뷰와 닉네임은 다른 이용자에게 보여요. 욕설·광고 등 서비스 운영을 방해하는 글은 삭제될 수 있어요.\n4. 이 앱은 중소벤처기업부·서울특별시와 관련이 없어요."

/** 시안 "회원가입"(1.webp) 복제. 간편 가입은 구글·카카오·네이버. */
@Composable
fun SignUpScreen(vm: AuthViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var nick by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var show1 by rememberSaveable { mutableStateOf(false) }
    var show2 by rememberSaveable { mutableStateOf(false) }
    var age by rememberSaveable { mutableStateOf(false) }
    var terms by rememberSaveable { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    var marketing by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var social by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    val nickInvalid = AuthValidation.nickname(nick).takeIf { nick.isNotEmpty() }
    val emailInvalid = AuthValidation.email(email.trim()).takeIf { email.isNotEmpty() }
    val pwInvalid = AuthValidation.password(password).takeIf { password.isNotEmpty() }
    val confirmInvalid = AuthValidation.passwordConfirm(password, confirm).takeIf { confirm.isNotEmpty() }
    val allAgreed = age && terms && privacy && marketing
    val ready = AuthValidation.nickname(nick) == null && AuthValidation.email(email.trim()) == null &&
        AuthValidation.password(password) == null && AuthValidation.passwordConfirm(password, confirm) == null &&
        state.nick != NickStatus.TAKEN && age && terms && privacy && !state.busy
    val submit = { if (ready) vm.signUp(email.trim(), password, nick) }

    Box(Modifier.fillMaxSize().background(MockColors.Cream)) {
        Box(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            androidx.compose.foundation.layout.Column {
                MockStage(originY = 0, heightPx = 1651) {
                    MockPic(R.drawable.mk2_su_hero, 760, 513, Modifier.at(0, 85))
                    Box(Modifier.at(650, 90).size(px(80), px(80)).clickable(role = Role.Button, onClick = onBack).semantics0("닫기"))

                    // 닉네임
                    MockPic(R.drawable.mk2_su_in_nick, 760, 83, Modifier.at(0, 605))
                    MockInput(nick, { nick = it.take(20); vm.onNicknameChanged(it) }, "닉네임을 입력해주세요", Modifier.at(112, 625).size(px(520), px(46)))
                    MockText("${nick.length}/12", 20, Modifier.at(600, 633).width(px(98)), color = Color(0xFF9AA0AB), align = TextAlign.End)
                    val nickHint = nickInvalid?.text() ?: when (state.nick) {
                        NickStatus.CHECKING -> "사용할 수 있는지 확인하는 중이에요."
                        NickStatus.AVAILABLE -> "사용할 수 있는 닉네임이에요."
                        NickStatus.TAKEN -> "이미 쓰고 있는 닉네임이에요."
                        else -> "다른 사용자에게 표시될 이름이에요."
                    }
                    MockText(nickHint, 20, Modifier.at(42, 688), color = if (nickInvalid != null || state.nick == NickStatus.TAKEN) ErrorRed else HintGray)

                    // 이메일
                    MockPic(R.drawable.mk2_su_in_mail, 760, 86, Modifier.at(0, 728))
                    MockInput(email, { email = it }, "이메일을 입력해주세요", Modifier.at(112, 750).size(px(360), px(46)), keyboard = KeyboardType.Email)
                    Box(Modifier.at(548, 748).size(px(164), px(50)).clickable(role = Role.Button) { dialog = "휴대폰 번호로 가입은 준비 중이에요. 지금은 이메일로 가입해 주세요." })
                    MockText(emailInvalid?.text() ?: "계정 인증 및 중요한 알림을 위해 사용돼요.", 20, Modifier.at(42, 817), color = if (emailInvalid != null) ErrorRed else HintGray)

                    // 비밀번호
                    MockPic(R.drawable.mk2_su_in_pw, 760, 83, Modifier.at(0, 855))
                    MockInput(password, { password = it }, "비밀번호를 입력해주세요", Modifier.at(112, 876).size(px(520), px(46)), password = true, visible = show1)
                    MockPic(R.drawable.mk2_su_eye, 46, 40, Modifier.at(660, 880), onClick = { show1 = !show1 }, description = "비밀번호 보기")
                    MockText(pwInvalid?.text() ?: "영문, 숫자, 특수문자 포함 8자 이상 입력해주세요.", 20, Modifier.at(42, 943), color = if (pwInvalid != null) ErrorRed else HintGray)
                    MockPic(R.drawable.mk2_su_in_pw2, 760, 83, Modifier.at(0, 978))
                    MockInput(confirm, { confirm = it }, "비밀번호를 다시 입력해주세요", Modifier.at(112, 999).size(px(520), px(46)), password = true, visible = show2, ime = ImeAction.Done, onDone = submit)
                    MockPic(R.drawable.mk2_su_eye, 46, 40, Modifier.at(660, 1003), onClick = { show2 = !show2 }, description = "비밀번호 보기")
                    MockText(confirmInvalid?.text() ?: "동일한 비밀번호를 한 번 더 입력해주세요.", 20, Modifier.at(42, 1066), color = if (confirmInvalid != null) ErrorRed else HintGray)

                    // 약관
                    MockCheck(allAgreed, 34, Modifier.at(46, 1119)) { v -> age = v; terms = v; privacy = v; marketing = v }
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.auth2_06), 25, Modifier.at(104, 1120), weight = FontWeight.ExtraBold)
                    Box(Modifier.at(40, 1166).size(px(680), 1.dp).background(Hairline))
                    data class Row(val y: Int, val label: String, val checked: Boolean, val onChange: (Boolean) -> Unit, val detail: String?)
                    listOf(
                        Row(1180, "[필수] 만 14세 이상입니다.", age, { age = it }, null),
                        Row(1221, "[필수] 이용약관에 동의합니다.", terms, { terms = it }, TermsText),
                        Row(1262, "[필수] 개인정보 수집 및 이용에 동의합니다.", privacy, { privacy = it }, "privacy"),
                        Row(1303, "[선택] 마케팅 알림 수신에 동의합니다.", marketing, { marketing = it }, "마케팅 알림은 새 소식과 이벤트를 알려 드리는 용도예요. 동의하지 않아도 앱을 모두 쓸 수 있고, 언제든 설정에서 바꿀 수 있어요."),
                    ).forEach { r ->
                        MockCheck(r.checked, 30, Modifier.at(47, r.y)) { r.onChange(it) }
                        MockText(r.label, 21, Modifier.at(104, r.y + 3), color = Color(0xFF4B5563))
                        if (r.detail != null) Box(Modifier.at(680, r.y - 6).size(px(50), px(44)).clickable(role = Role.Button) { dialog = r.detail }, contentAlignment = Alignment.Center) { ChevronRight(24) }
                    }
                    MockLimeBar(if (state.busy) "가입 중…" else "가입하고 시작하기", 82, Modifier.at(45, 1368).size(px(672), px(82)), textPx = 28, enabled = ready, onClick = submit)
                    (state.error?.message(context) ?: note)?.let { MockText(it, 20, Modifier.at(45, 1455).width(px(670)), color = ErrorRed, maxLines = 2) }

                    // 간편 가입
                    Box(Modifier.at(40, 1487).size(px(180), 1.dp).background(Hairline))
                    Box(Modifier.at(540, 1487).size(px(180), 1.dp).background(Hairline))
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.auth2_07), 20, Modifier.at(224, 1474).width(px(314)), color = HintGray, align = TextAlign.Center)
                    Box(Modifier.at(278, 1520).size(1.dp, px(80)).background(Hairline))
                    Box(Modifier.at(485, 1520).size(1.dp, px(80)).background(Hairline))
                    val cx = listOf(188, 381, 590)
                    val names = listOf("Google", "카카오", "네이버")
                    cx.forEachIndexed { i, x ->
                        Box(Modifier.at(x - 70, 1512).size(px(140), px(130)).clickable(role = Role.Button) { if (i == 0) scope.launch { doGoogle(context, vm) { note = it } } else social = names[i] })
                        MockText("${names[i]}로 시작하기", 20, Modifier.at(x - 70, 1606).width(px(140)), align = TextAlign.Center, color = Color(0xFF4B5563))
                    }
                    Box(Modifier.at(148, 1516).size(px(80)).clip(CircleShape).background(Color.White).border(1.dp, Hairline, CircleShape), contentAlignment = Alignment.Center) {
                        MockPic(R.drawable.mk2_lg_ic_google, 44, 44)
                    }
                    MockPic(R.drawable.mk2_su_ic_kakao, 80, 80, Modifier.at(341, 1516))
                    MockPic(R.drawable.mk2_su_ic_naver, 80, 80, Modifier.at(550, 1516))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
    SocialNotReadyDialog(social) { social = null }
    dialog?.let { d ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            text = { Text(if (d == "privacy") androidx.compose.ui.res.stringResource(R.string.privacy_policy_full) else d, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton({ dialog = null }) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_02)) } },
        )
    }
}

private fun Modifier.semantics0(label: String): Modifier = this.then(Modifier.semantics { contentDescription = label })
