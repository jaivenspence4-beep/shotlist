package app.shotlist.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.shotlist.MainActivity
import app.shotlist.actions.ShotlistActions
import app.shotlist.data.HabitTick
import app.shotlist.data.ShotlistDb
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class NextUpWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent { NextUpContent(context, snapshot) }
    }
}

class NextUpWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextUpWidget()
}

class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent { StreakContent(context, snapshot) }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

class VaultWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent { VaultContent(context, snapshot) }
    }
}

class VaultWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VaultWidget()
}

private data class WidgetSnapshot(
    val nextFindingId: Long?,
    val nextTitle: String,
    val nextWhen: String,
    val habitStreak: Int,
    val vaultCount: Int,
)

private suspend fun loadSnapshot(context: Context): WidgetSnapshot = withContext(Dispatchers.IO) {
    val db = ShotlistDb.get(context)
    val next = db.findings().upcoming().first().firstOrNull()
    val habits = db.habits().active().first()
    val today = LocalDate.now().toEpochDay()
    val ticks = db.habits().ticksSince(today - 90).first()
    WidgetSnapshot(
        nextFindingId = next?.id,
        nextTitle = next?.title ?: "Inbox is clear",
        nextWhen = next?.whenAt?.let(::formatWhen) ?: "Screenshot something worth keeping",
        habitStreak = habits.maxOfOrNull { habit -> streakFor(habit.id, ticks, today) } ?: 0,
        vaultCount = db.findings().vaultedCount().first(),
    )
}

@Composable
private fun NextUpContent(context: Context, snapshot: WidgetSnapshot) {
    val openNext = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_TARGET_TAB, "inbox")
        snapshot.nextFindingId?.let { putExtra(ShotlistActions.EXTRA_FINDING_ID, it) }
        data = Uri.parse("shotlist://widget/next/${snapshot.nextFindingId ?: 0}")
    }
    Row(
        modifier = widgetModifier.clickable(actionStartActivity(openNext)),
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text("NEXT UP", style = widgetText(10.sp, periwinkle, FontWeight.Bold))
            Spacer(GlanceModifier.height(3.dp))
            Text(snapshot.nextTitle, style = widgetText(17.sp, primary, FontWeight.Bold), maxLines = 1)
            Text(snapshot.nextWhen, style = widgetText(12.sp, muted, FontWeight.Normal), maxLines = 1)
        }
        Spacer(GlanceModifier.width(10.dp))
        Text("✦", style = widgetText(22.sp, aqua, FontWeight.Bold))
    }
}

@Composable
private fun StreakContent(context: Context, snapshot: WidgetSnapshot) {
    val openTrack = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_TARGET_TAB, "track")
        .setData(Uri.parse("shotlist://widget/track"))
    Column(
        modifier = widgetModifier.clickable(actionStartActivity(openTrack)),
    ) {
        Text("🔥", style = widgetText(18.sp, primary, FontWeight.Normal))
        Spacer(GlanceModifier.height(2.dp))
        Text(snapshot.habitStreak.toString(), style = widgetText(23.sp, amber, FontWeight.Bold))
        Text(
            if (snapshot.habitStreak == 1) "DAY" else "DAYS",
            style = widgetText(9.sp, muted, FontWeight.Bold),
        )
    }
}

@Composable
private fun VaultContent(context: Context, snapshot: WidgetSnapshot) {
    val openVault = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_TARGET_TAB, "you")
        .putExtra(MainActivity.EXTRA_OPEN_VAULT, true)
        .setData(Uri.parse("shotlist://widget/vault"))
    Column(
        modifier = widgetModifier.clickable(actionStartActivity(openVault)),
    ) {
        Text("🔐", style = widgetText(18.sp, primary, FontWeight.Normal))
        Spacer(GlanceModifier.height(2.dp))
        Text(snapshot.vaultCount.toString(), style = widgetText(23.sp, pink, FontWeight.Bold))
        Text("VAULTED", style = widgetText(9.sp, muted, FontWeight.Bold))
    }
}

private val widgetModifier = GlanceModifier
    .fillMaxSize()
    .background(widgetBackground)
    .cornerRadius(24.dp)
    .padding(14.dp)

private fun widgetText(
    size: androidx.compose.ui.unit.TextUnit,
    color: ColorProvider,
    weight: FontWeight,
): TextStyle = TextStyle(color = color, fontSize = size, fontWeight = weight)

private fun formatWhen(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a"))

private fun streakFor(habitId: Long, ticks: List<HabitTick>, today: Long): Int {
    val days = ticks.asSequence().filter { it.habitId == habitId }.map { it.day }.toSet()
    var day = if (today in days) today else today - 1
    var streak = 0
    while (day in days) {
        streak += 1
        day -= 1
    }
    return streak
}

private fun colors(day: Long, night: Long) = ColorProvider(Color(day), Color(night))

private val widgetBackground = colors(0xFFF1F3FF, 0xFF10162B)
private val primary = colors(0xFF11162A, 0xFFF7F8FF)
private val muted = colors(0xFF5E6579, 0xFFB8BED1)
private val aqua = colors(0xFF007F6D, 0xFF7EF5D8)
private val periwinkle = colors(0xFF485ED0, 0xFFAAB8FF)
private val amber = colors(0xFFA65D00, 0xFFFFBE63)
private val pink = colors(0xFFA62E70, 0xFFFF79C9)
