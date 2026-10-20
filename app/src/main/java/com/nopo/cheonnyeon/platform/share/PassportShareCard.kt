package com.nopo.cheonnyeon.platform.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.nopo.cheonnyeon.R
import java.io.File

/** 공유 카드에 들어가는 값. 위치·영수증·이메일은 들어가지 않는다 (PAS-05). */
data class ShareCardData(
    val nickname: String,
    val titleName: String,
    val totalYears: Int,
    val stampCount: Int,
    /** 최근 도장(가게 이름, 세월) 최대 3개 */
    val recent: List<Pair<String, Int>>,
    val colors: ShareCardColors,
)

data class ShareCardColors(val background: Int, val surface: Int, val primary: Int, val stamp: Int, val brass: Int, val ink: Int, val ink2: Int)

/** 패스포트 공유 카드(1080×1350)를 Canvas로 그려 PNG로 저장하고 공유 시트를 띄운다 (PAS-05). */
object PassportShareCard {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    fun render(context: Context, d: ShareCardData): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val c = d.colors
        val title = ResourcesCompat.getFont(context, R.font.black_han_sans) ?: Typeface.DEFAULT_BOLD
        fun paint(size: Float, color: Int, face: Typeface = Typeface.DEFAULT_BOLD, align: Paint.Align = Paint.Align.CENTER) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = face; textAlign = align }

        cv.drawColor(c.background)
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.brass; style = Paint.Style.STROKE; strokeWidth = 8f }
        cv.drawRoundRect(40f, 40f, WIDTH - 40f, HEIGHT - 40f, 40f, 40f, frame)

        cv.drawText(context.getString(R.string.share_card_heading), WIDTH / 2f, 160f, paint(64f, c.primary, title))
        cv.drawText(d.nickname, WIDTH / 2f, 260f, paint(56f, c.ink))
        cv.drawText(d.titleName, WIDTH / 2f, 335f, paint(44f, c.stamp))

        // 큰 세월 도장
        val stamp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.stamp; style = Paint.Style.STROKE; strokeWidth = 14f }
        cv.drawCircle(WIDTH / 2f, 640f, 230f, stamp)
        stamp.strokeWidth = 4f
        cv.drawCircle(WIDTH / 2f, 640f, 205f, stamp)
        cv.drawText(context.getString(R.string.share_card_years_label), WIDTH / 2f, 560f, paint(44f, c.stamp))
        cv.drawText(context.getString(R.string.share_card_years, d.totalYears), WIDTH / 2f, 690f, paint(150f, c.stamp, title))
        cv.drawText(context.getString(R.string.share_card_count, d.stampCount), WIDTH / 2f, 770f, paint(40f, c.ink2))

        // 최근 도장
        val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.surface }
        cv.drawRoundRect(100f, 930f, WIDTH - 100f, 1190f, 32f, 32f, box)
        cv.drawText(context.getString(R.string.share_card_recent), 150f, 995f, paint(36f, c.primary, align = Paint.Align.LEFT))
        d.recent.take(3).forEachIndexed { i, (name, years) ->
            val y = 1060f + i * 52f
            cv.drawText(name.take(16), 150f, y, paint(38f, c.ink, align = Paint.Align.LEFT))
            cv.drawText(context.getString(R.string.share_card_years, years), WIDTH - 150f, y, paint(38f, c.stamp, align = Paint.Align.RIGHT))
        }
        cv.drawText(context.getString(R.string.app_name), WIDTH / 2f, 1270f, paint(40f, c.ink2, title))
        return bmp
    }

    /** 카드를 캐시 폴더에 저장하고 공유 시트를 연다. 실패하면 false. */
    fun share(context: Context, d: ShareCardData): Boolean = runCatching {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "passport.png")
        val bmp = render(context, d)
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_card_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
