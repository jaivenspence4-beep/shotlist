package app.shotlist.ui.share

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
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import app.shotlist.actions.ActionKind
import app.shotlist.actions.ShotlistAction
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    fun findingIntent(context: Context, action: ShotlistAction): Intent {
        val accent = accentFor(action.kind)
        val uri = render(
            context = context,
            fileName = "finding-${action.findingId ?: action.id.hashCode()}.png",
            eyebrow = kindLabel(action.kind).uppercase(),
            headline = action.title,
            detail = action.detail,
            metric = "✦",
            accent = accent,
        )
        return chooser(context, uri, "Share this find")
    }

    fun weeklyIntent(
        context: Context,
        found: Int,
        acted: Int,
        streak: Int,
        topType: String,
    ): Intent {
        val uri = render(
            context = context,
            fileName = "weekly-wrapped.png",
            eyebrow = "MY SHOTLIST WEEK",
            headline = "$found useful finds",
            detail = "$acted handled  •  Top find: $topType",
            metric = "🔥 $streak day rhythm",
            accent = Color.rgb(255, 121, 201),
        )
        return chooser(context, uri, "Share your Shotlist week")
    }

    private fun render(
        context: Context,
        fileName: String,
        eyebrow: String,
        headline: String,
        detail: String,
        metric: String,
        accent: Int,
    ): Uri {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                WIDTH.toFloat(),
                HEIGHT.toFloat(),
                intArrayOf(Color.rgb(9, 11, 24), mix(accent, Color.BLACK, 0.68f), Color.rgb(16, 22, 43)),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), background)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                190f,
                210f,
                660f,
                intArrayOf(withAlpha(accent, 175), withAlpha(accent, 25), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(190f, 210f, 660f, glow)
        canvas.drawCircle(970f, 1050f, 510f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.rgb(112, 240, 208), 36)
        })

        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(94, 255, 255, 255) }
        canvas.drawRoundRect(68f, 64f, 1012f, 1286f, 76f, 76f, panel)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(105, 255, 255, 255)
        }
        canvas.drawRoundRect(68f, 64f, 1012f, 1286f, 76f, 76f, border)

        drawText(canvas, "✦  SHOTLIST", 126f, 134f, 43f, Color.WHITE, Typeface.BOLD, 820)
        drawText(canvas, eyebrow, 126f, 314f, 30f, accent, Typeface.BOLD, 820)
        drawText(canvas, headline.take(140), 126f, 392f, 82f, Color.WHITE, Typeface.BOLD, 820, 3)
        drawText(canvas, detail.take(260), 126f, 700f, 39f, Color.argb(205, 255, 255, 255), Typeface.NORMAL, 820, 4)

        val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(accent, 52) }
        canvas.drawRoundRect(126f, 1000f, 720f, 1112f, 56f, 56f, metricPaint)
        drawText(canvas, metric, 163f, 1028f, 40f, accent, Typeface.BOLD, 520, 1)

        drawText(canvas, "Screenshots in. Life out.", 126f, 1182f, 28f, Color.WHITE, Typeface.BOLD, 820)
        drawText(canvas, "Private by design • processed on your phone", 126f, 1230f, 23f, Color.argb(150, 255, 255, 255), Typeface.NORMAL, 820)

        val directory = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        directory.listFiles()?.filter { it.name != fileName }?.forEach { it.delete() }
        val file = File(directory, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 94, it) }
        bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun chooser(context: Context, uri: Uri, title: String): Intent {
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newUri(context.contentResolver, "Shotlist share card", uri)
        return Intent.createChooser(send, title)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        style: Int,
        width: Int,
        maxLines: Int = 1,
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.create("sans-serif", style), style)
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(2f, 1f)
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun accentFor(kind: ActionKind): Int = when (kind) {
        ActionKind.Event -> Color.rgb(142, 170, 255)
        ActionKind.Deadline -> Color.rgb(255, 190, 99)
        ActionKind.Product -> Color.rgb(255, 121, 201)
        ActionKind.Place -> Color.rgb(112, 240, 208)
        ActionKind.Code -> Color.rgb(88, 216, 255)
        ActionKind.Link -> Color.rgb(170, 184, 255)
        ActionKind.Contact -> Color.rgb(126, 245, 216)
        ActionKind.Recipe -> Color.rgb(255, 157, 114)
        ActionKind.Noise -> Color.rgb(181, 186, 208)
    }

    private fun kindLabel(kind: ActionKind): String = when (kind) {
        ActionKind.Event -> "Event"
        ActionKind.Deadline -> "Deadline"
        ActionKind.Product -> "Product"
        ActionKind.Place -> "Place"
        ActionKind.Code -> "Code"
        ActionKind.Link -> "Link"
        ActionKind.Contact -> "Contact"
        ActionKind.Recipe -> "Recipe"
        ActionKind.Noise -> "Saved"
    }

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
        alpha,
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )

    private fun mix(first: Int, second: Int, amount: Float): Int = Color.rgb(
        (Color.red(first) * (1f - amount) + Color.red(second) * amount).toInt(),
        (Color.green(first) * (1f - amount) + Color.green(second) * amount).toInt(),
        (Color.blue(first) * (1f - amount) + Color.blue(second) * amount).toInt(),
    )
}
