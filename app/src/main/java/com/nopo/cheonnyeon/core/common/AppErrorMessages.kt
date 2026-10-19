package com.nopo.cheonnyeon.core.common

import android.content.Context

/** 오류 코드 → 화면 문구. 거리·정확도(미터)가 필요한 코드는 [meters]를 받는다. */
fun AppError.message(context: Context, meters: Int = 0): String {
    val id = context.resources.getIdentifier("err_" + code.lowercase(), "string", context.packageName)
    return if (id == 0) code else context.getString(id, meters)
}
