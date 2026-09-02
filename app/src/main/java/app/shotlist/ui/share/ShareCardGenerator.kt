package app.shotlist.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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

/** Stable choices shared by the picker and bitmap renderer. */
enum class ShareTemplate(
    val key: String,
    val label: String,
    val description: String,
) {
    AURORA("aurora", "Aurora", "Glowing glass and color"),
    POSTER("poster", "Poster", "Big, bright and playful"),
    PAPER("paper", "Paper", "Warm editorial keepsake"),
    SIGNAL("signal", "Signal", "Sharp monochrome impact"),
    ;

    companion object {
        fun fromKey(key: String?): ShareTemplate = entries.firstOrNull { it.key == key } ?: AURORA
    }
}

object ShareCardGenerator {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val PREFS = "shotlist_share"
    private const val TEMPLATE_KEY = "last_template"

    fun selectedTemplate(context: Context): ShareTemplate = ShareTemplate.fromKey(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TEMPLATE_KEY, null),
    )

    fun rememberTemplate(context: Context, template: ShareTemplate) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TEMPLATE_KEY, template.key)
            .apply()
    }

    /** Compatibility entry point: shares with the person's last chosen style. */
    fun findingIntent(context: Context, action: ShotlistAction): Intent =
        findingIntent(context, action, selectedTemplate(context))

    fun findingIntent(
        context: Context,
        action: ShotlistAction,
        template: ShareTemplate,
    ): Intent {
        rememberTemplate(context, template)
        val uri = render(
            context = context,
            fileName = "finding-${action.findingId ?: action.id.hashCode()}-${template.key}.png",
            card = ShareCard(
                eyebrow = kindLabel(action.kind).uppercase(),
                headline = action.title,
                detail = action.detail,
                metric = "A FIND WORTH KEEPING",
                accent = accentFor(action.kind),
            ),
            template = template,
        )
        return chooser(context, uri, "Share this find")
    }

    /** Compatibility entry point: shares with the person's last chosen style. */
    fun weeklyIntent(
        context: Context,
        found: Int,
        acted: Int,
        streak: Int,
        topType: String,
    ): Intent = weeklyIntent(context, found, acted, streak, topType, selectedTemplate(context))

    fun weeklyIntent(
        context: Context,
        found: Int,
        acted: Int,
        streak: Int,
        topType: String,
        template: ShareTemplate,
    ): Intent {
        rememberTemplate(context, template)
        val uri = render(
            context = context,
            fileName = "weekly-wrapped-${template.key}.png",
            card = ShareCard(
                eyebrow = "MY SHOTLIST WEEK",
                headline = "$found useful finds",
                detail = "$acted handled  •  Top find: $topType",
                metric = "🔥 $streak DAY RHYTHM",
                accent = Color.rgb(255, 121, 201),
            ),
            template = template,
        )
        return chooser(context, uri, "Share your Shotlist week")
    }

    /** Ready for Collections (t72) without coupling this renderer to its data model. */
    fun collectionIntent(
        context: Context,
        boardName: String,
        itemCount: Int,
        highlight: String,
        template: ShareTemplate = selectedTemplate(context),
    ): Intent {
        rememberTemplate(context, template)
        val uri = render(
            context = context,
            fileName = "collection-${boardName.hashCode()}-${template.key}.png",
            card = ShareCard(
                eyebrow = "MY COLLECTION",
                headline = boardName,
                detail = highlight,
                metric = if (itemCount == 1) "1 SAVED FIND" else "$itemCount SAVED FINDS",
                accent = Color.rgb(112, 240, 208),
            ),
            template = template,
        )
        return chooser(context, uri, "Share your collection")
    }

    private data class ShareCard(
        val eyebrow: String,
        val headline: String,
        val detail: String,
        val metric: String,
        val accent: Int,
    )

    private data class CardInk(
        val headline: Int,
        val body: Int,
        val muted: Int,
        val eyebrow: Int,
    )

    private fun render(
        context: Context,
        fileName: String,
        card: ShareCard,
        template: ShareTemplate,
    ): Uri {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val ink = drawTemplate(canvas, template, card.accent)

        when (template) {
            ShareTemplate.AURORA -> drawAuroraCopy(canvas, card, ink)
            ShareTemplate.POSTER -> drawPosterCopy(canvas, card, ink)
            ShareTemplate.PAPER -> drawPaperCopy(canvas, card, ink)
            ShareTemplate.SIGNAL -> drawSignalCopy(canvas, card, ink)
        }

        val directory = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        // A share URI must stay valid while the chooser is open. Keep the four most
        // recent variants and only prune genuinely old cards.
        directory.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(4)
            ?.forEach { it.delete() }
        val file = File(directory, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 94, it) }
        bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun drawTemplate(canvas: Canvas, template: ShareTemplate, accent: Int): CardInk =
        when (template) {
            ShareTemplate.AURORA -> drawAurora(canvas, accent)
            ShareTemplate.POSTER -> drawPoster(canvas, accent)
            ShareTemplate.PAPER -> drawPaper(canvas, accent)
            ShareTemplate.SIGNAL -> drawSignal(canvas, accent)
        }

    private fun drawAurora(canvas: Canvas, accent: Int): CardInk {
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
        canvas.drawCircle(
            970f,
            1050f,
            510f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Color.rgb(112, 240, 208), 36) },
        )
        canvas.drawRoundRect(
            68f,
            64f,
            1012f,
            1286f,
            76f,
            76f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(94, 255, 255, 255) },
        )
        canvas.drawRoundRect(
            68f,
            64f,
            1012f,
            1286f,
            76f,
            76f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.argb(105, 255, 255, 255)
            },
        )
        return CardInk(Color.WHITE, Color.argb(210, 255, 255, 255), Color.argb(155, 255, 255, 255), accent)
    }

    private fun drawPoster(canvas: Canvas, accent: Int): CardInk {
        val hot = brighten(accent, 0.23f)
        canvas.drawColor(hot)
        canvas.drawCircle(935f, 155f, 250f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 225, 91) })
        canvas.drawCircle(965f, 1120f, 330f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(104, 246, 211) })
        canvas.drawRoundRect(
            54f,
            52f,
            1026f,
            1298f,
            18f,
            18f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(19, 17, 25)
                style = Paint.Style.STROKE
                strokeWidth = 12f
            },
        )
        val slash = Path().apply {
            moveTo(-80f, 970f)
            lineTo(1080f, 690f)
            lineTo(1080f, 805f)
            lineTo(-80f, 1085f)
            close()
        }
        canvas.drawPath(slash, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(52, 255, 255, 255) })
        return CardInk(
            Color.rgb(19, 17, 25),
            Color.rgb(35, 27, 41),
            Color.argb(180, 19, 17, 25),
            Color.rgb(19, 17, 25),
        )
    }

    private fun drawPaper(canvas: Canvas, accent: Int): CardInk {
        canvas.drawColor(Color.rgb(246, 238, 219))
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(28, 25, 30, 38)
            strokeWidth = 2f
        }
        var y = 170f
        while (y < HEIGHT) {
            canvas.drawLine(0f, y, WIDTH.toFloat(), y, rule)
            y += 72f
        }
        canvas.save()
        canvas.rotate(-2.2f, 540f, 675f)
        canvas.drawRoundRect(
            78f,
            72f,
            1002f,
            1278f,
            8f,
            8f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 252, 242) },
        )
        canvas.restore()
        canvas.drawRect(100f, 86f, 360f, 124f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(accent, 125) })
        canvas.drawCircle(
            902f,
            1120f,
            120f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(accent, 90)
                style = Paint.Style.STROKE
                strokeWidth = 18f
            },
        )
        return CardInk(
            Color.rgb(25, 30, 38),
            Color.rgb(54, 56, 59),
            Color.rgb(103, 98, 89),
            mix(accent, Color.BLACK, 0.18f),
        )
    }

    private fun drawSignal(canvas: Canvas, accent: Int): CardInk {
        canvas.drawColor(Color.rgb(5, 7, 9))
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(32, 255, 255, 255)
            strokeWidth = 2f
        }
        for (x in 0..WIDTH step 90) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), HEIGHT.toFloat(), grid)
        for (y in 0..HEIGHT step 90) canvas.drawLine(0f, y.toFloat(), WIDTH.toFloat(), y.toFloat(), grid)
        canvas.drawRect(0f, 0f, 28f, HEIGHT.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        canvas.drawRect(72f, 1050f, 1008f, 1062f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        canvas.drawRoundRect(
            72f,
            56f,
            1008f,
            1294f,
            2f,
            2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(130, 0, 0, 0) },
        )
        return CardInk(Color.WHITE, Color.rgb(215, 220, 225), Color.rgb(143, 151, 158), accent)
    }

    private fun drawAuroraCopy(canvas: Canvas, card: ShareCard, ink: CardInk) {
        drawText(canvas, "✦  SHOTLIST", 126f, 134f, 43f, ink.headline, Typeface.BOLD, 820)
        drawText(canvas, card.eyebrow, 126f, 314f, 30f, ink.eyebrow, Typeface.BOLD, 820)
        drawText(canvas, card.headline.take(140), 126f, 392f, 82f, ink.headline, Typeface.BOLD, 820, 3)
        drawText(canvas, card.detail.take(260), 126f, 700f, 39f, ink.body, Typeface.NORMAL, 820, 4)
        canvas.drawRoundRect(126f, 1000f, 760f, 1112f, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(card.accent, 52)
        })
        drawText(canvas, card.metric, 163f, 1030f, 32f, card.accent, Typeface.BOLD, 560, 1)
        drawFooter(canvas, 126f, 1182f, ink)
    }

    private fun drawPosterCopy(canvas: Canvas, card: ShareCard, ink: CardInk) {
        drawText(canvas, "SHOTLIST / ${card.eyebrow}", 92f, 94f, 31f, ink.eyebrow, Typeface.BOLD, 820)
        drawText(canvas, card.headline.take(140), 86f, 250f, 104f, ink.headline, Typeface.BOLD, 880, 4)
        drawText(canvas, card.detail.take(220), 92f, 760f, 42f, ink.body, Typeface.BOLD, 790, 3)
        canvas.drawRoundRect(
            88f,
            1030f,
            820f,
            1140f,
            10f,
            10f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink.headline },
        )
        drawText(canvas, card.metric, 124f, 1064f, 31f, Color.WHITE, Typeface.BOLD, 660, 1)
        drawFooter(canvas, 92f, 1192f, ink)
    }

    private fun drawPaperCopy(canvas: Canvas, card: ShareCard, ink: CardInk) {
        drawText(canvas, "SHOTLIST", 128f, 140f, 29f, ink.muted, Typeface.BOLD, 780)
        drawText(canvas, card.eyebrow, 128f, 282f, 28f, ink.eyebrow, Typeface.BOLD, 780)
        drawText(canvas, card.headline.take(140), 128f, 360f, 78f, ink.headline, Typeface.BOLD, 790, 4)
        drawText(canvas, card.detail.take(260), 128f, 730f, 38f, ink.body, Typeface.NORMAL, 790, 4)
        drawText(canvas, card.metric, 128f, 1035f, 31f, ink.eyebrow, Typeface.BOLD, 690, 1)
        canvas.drawLine(128f, 1120f, 920f, 1120f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 25, 30, 38)
            strokeWidth = 3f
        })
        drawFooter(canvas, 128f, 1160f, ink)
    }

    private fun drawSignalCopy(canvas: Canvas, card: ShareCard, ink: CardInk) {
        drawText(canvas, "✦ SHOTLIST", 106f, 104f, 31f, ink.headline, Typeface.BOLD, 800)
        drawText(canvas, "[ ${card.eyebrow} ]", 106f, 266f, 28f, ink.eyebrow, Typeface.BOLD, 800)
        drawText(canvas, card.headline.take(140), 106f, 350f, 88f, ink.headline, Typeface.BOLD, 830, 4)
        drawText(canvas, card.detail.take(240), 106f, 754f, 37f, ink.body, Typeface.NORMAL, 820, 4)
        drawText(canvas, "// ${card.metric}", 106f, 1095f, 29f, ink.eyebrow, Typeface.BOLD, 780, 1)
        drawFooter(canvas, 106f, 1190f, ink)
    }

    private fun drawFooter(canvas: Canvas, x: Float, y: Float, ink: CardInk) {
        drawText(canvas, "Screenshots in. Life out.", x, y, 28f, ink.headline, Typeface.BOLD, 820)
        drawText(canvas, "Private by design • processed on your phone", x, y + 48f, 23f, ink.muted, Typeface.NORMAL, 820)
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

    private fun brighten(color: Int, amount: Float): Int = mix(color, Color.WHITE, amount)

    private fun mix(first: Int, second: Int, amount: Float): Int = Color.rgb(
        (Color.red(first) * (1f - amount) + Color.red(second) * amount).toInt(),
        (Color.green(first) * (1f - amount) + Color.green(second) * amount).toInt(),
        (Color.blue(first) * (1f - amount) + Color.blue(second) * amount).toInt(),
    )
}
