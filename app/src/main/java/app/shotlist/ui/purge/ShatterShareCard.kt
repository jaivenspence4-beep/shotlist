package app.shotlist.ui.purge

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

internal fun shatterShareIntent(context: Context, count: Int, bytes: Long): Intent {
    val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawRect(
        0f,
        0f,
        1080f,
        1350f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                1080f,
                1350f,
                intArrayOf(Color.rgb(7, 11, 27), Color.rgb(43, 20, 63), Color.rgb(10, 46, 57)),
                null,
                Shader.TileMode.CLAMP,
            )
        },
    )
    val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(74, 255, 255, 255) }
    canvas.drawRoundRect(68f, 64f, 1012f, 1286f, 78f, 78f, glass)
    val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(115, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRoundRect(68f, 64f, 1012f, 1286f, 78f, 78f, edge)

    drawCardText(canvas, "✦  SHOTLIST", 126f, 146f, 42f, Color.WHITE)
    drawCardText(canvas, "SHATTER MODE", 126f, 318f, 30f, Color.rgb(255, 121, 201))
    drawCardText(canvas, formatBytes(bytes), 126f, 410f, 118f, Color.WHITE)
    drawCardText(canvas, "freed", 132f, 555f, 46f, Color.argb(190, 255, 255, 255))
    drawCardText(canvas, "$count screenshots", 126f, 720f, 62f, Color.rgb(126, 245, 216))
    drawCardText(canvas, "shattered into the trash", 130f, 810f, 34f, Color.WHITE)

    val shard = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(155, 170, 184, 255)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    repeat(12) { index ->
        val x = 150f + index * 73f
        val y = 965f + (index % 3) * 34f
        canvas.drawLine(540f, 890f, x, y, shard)
    }
    drawCardText(canvas, "Trashed, not permanently deleted.", 126f, 1166f, 27f, Color.WHITE)
    drawCardText(canvas, "Screenshots in. Life out.", 126f, 1218f, 24f, Color.argb(155, 255, 255, 255))

    val directory = File(context.cacheDir, "shared_cards").apply { mkdirs() }
    val file = File(directory, "shatter-results.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 94, it) }
    bitmap.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newUri(context.contentResolver, "Shatter results", uri)
    return Intent.createChooser(send, "Share your cleanup")
}

internal fun formatBytes(bytes: Long): String {
    val mb = bytes.coerceAtLeast(0) / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        String.format(Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }
}

private fun drawCardText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Int,
) {
    canvas.drawText(
        text,
        x,
        y,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        },
    )
}
