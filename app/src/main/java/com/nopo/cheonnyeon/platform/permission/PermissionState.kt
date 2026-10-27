package com.nopo.cheonnyeon.platform.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

enum class PermissionStatus { GRANTED, NOT_ASKED, DENIED, PERMANENTLY_DENIED }

class PermissionHandle internal constructor(
    val status: PermissionStatus,
    /** 위치에서만 의미 있음: 대략적 위치만 허용된 상태 */
    val approximateOnly: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

/**
 * 권한 상태와 요청 함수. 화면 복귀(ON_RESUME) 때마다 다시 확인한다 (아키텍처 9장).
 * [permissions]의 첫 항목이 꼭 필요한 권한이다(위치는 FINE을 먼저 둔다).
 */
@Composable
fun rememberPermission(vararg permissions: String): PermissionHandle {
    val context = LocalContext.current
    val primary = permissions.first()
    var asked by rememberSaveable { mutableStateOf(false) }
    var tick by rememberSaveable { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        tick++
    }
    // ON_RESUME·결과 수신 때(tick 변경) 다시 확인한다
    val granted = androidx.compose.runtime.remember(tick) { context.isGranted(primary) }
    val anyGranted = androidx.compose.runtime.remember(tick) { permissions.any { context.isGranted(it) } }
    val activity = context.findActivity()
    val status = when {
        granted -> PermissionStatus.GRANTED
        !asked -> PermissionStatus.NOT_ASKED
        activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, primary) ->
            PermissionStatus.PERMANENTLY_DENIED
        else -> PermissionStatus.DENIED
    }
    return PermissionHandle(
        status = status,
        approximateOnly = !granted && anyGranted,
        request = { launcher.launch(arrayOf(*permissions)) },
        openSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
    )
}

private fun Context.isGranted(p: String) =
    ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
