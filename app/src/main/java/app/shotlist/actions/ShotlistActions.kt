package app.shotlist.actions

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import app.shotlist.MainActivity
import app.shotlist.R
import app.shotlist.data.Finding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ActionKind {
    Event,
    Deadline,
    Product,
    Place,
    Code,
    Link,
    Contact,
    Recipe,
    Noise,
}

data class ShotlistAction(
    val id: String,
    val findingId: Long? = null,
    val kind: ActionKind,
    val title: String,
    val detail: String,
    val source: String,
    val confidence: Float,
    val startsAt: Instant? = null,
    val location: String? = null,
    val url: String? = null,
    val code: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

object ShotlistActions {
    const val REMINDER_CHANNEL_ID = "shotlist_reminders"
    const val ACTION_POST = "app.shotlist.action.POST_FINDING"
    const val ACTION_ACCEPT = "app.shotlist.action.ACCEPT_FINDING"
    const val ACTION_DISMISS = "app.shotlist.action.DISMISS_FINDING"
    const val ACTION_REMINDER = "app.shotlist.action.SOFT_REMINDER"

    const val EXTRA_FINDING_ID = "findingId"
    private const val EXTRA_KIND = "kind"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_DETAIL = "detail"
    private const val EXTRA_STARTS_AT = "startsAt"
    private const val EXTRA_LOCATION = "location"

    private val notificationWhenFormat = DateTimeFormatter.ofPattern("EEE h:mm a")

    fun calendarInsertIntent(action: ShotlistAction): Intent {
        val start = action.startsAt?.toEpochMilli() ?: System.currentTimeMillis()
        val end = start + 60 * 60 * 1000
        return Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, action.title)
            .putExtra(CalendarContract.Events.DESCRIPTION, "${action.detail}\n\nFrom: ${action.source}")
            .putExtra(CalendarContract.Events.EVENT_LOCATION, action.location)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
    }

    fun copyCode(context: Context, action: ShotlistAction) {
        val value = action.code ?: action.detail
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(action.title, value))
    }

    fun openUrlIntent(action: ShotlistAction): Intent? {
        val target = action.url ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(target))
    }

    fun contactInsertIntent(action: ShotlistAction): Intent = Intent(
        Intent.ACTION_INSERT,
        ContactsContract.Contacts.CONTENT_URI,
    ).apply {
        putExtra(ContactsContract.Intents.Insert.NAME, action.title)
        action.phone?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
        action.email?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
    }

    fun mapSearchIntent(action: ShotlistAction): Intent? {
        val query = action.location ?: action.detail.takeIf { it.isNotBlank() } ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
    }

    fun scheduleSoftReminder(context: Context, action: ShotlistAction, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ShotlistReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putAction(action)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${action.id}:$triggerAtMillis".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    fun scheduleEventReminders(context: Context, action: ShotlistAction) {
        val start = action.startsAt ?: return
        val zone = ZoneId.systemDefault()
        val event = start.atZone(zone)
        val dayBefore = event.minusDays(1).toInstant().toEpochMilli()
        val morningOf = event.toLocalDate().atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        scheduleSoftReminder(context, action, dayBefore)
        scheduleSoftReminder(context, action, morningOf)
    }

    /** Called by the ingest worker only after a new finding is committed. */
    fun postNewFinding(context: Context, finding: Finding) {
        if (finding.type != "EVENT" && finding.type != "DEADLINE") return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val action = finding.toNotificationAction()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId(action), buildReminderNotification(context, action))
    }

    fun buildReminderNotification(context: Context, action: ShotlistAction): Notification {
        ensureReminderChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_FINDING_ID, action.findingId ?: -1L)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openIntent = PendingIntent.getActivity(
            context,
            "${action.id}:open".hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val acceptIntent = PendingIntent.getBroadcast(
            context,
            "${action.id}:accept".hashCode(),
            Intent(context, ShotlistReminderReceiver::class.java)
                .setAction(ACTION_ACCEPT)
                .putAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            "${action.id}:dismiss".hashCode(),
            Intent(context, ShotlistReminderReceiver::class.java)
                .setAction(ACTION_DISMISS)
                .putAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val prompt = buildString {
            append(action.title)
            action.startsAt?.atZone(ZoneId.systemDefault())?.let {
                append(" · ")
                append(it.format(notificationWhenFormat))
            }
            append(" — add it?")
        }

        return Notification.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shotlist)
            .setContentTitle(prompt)
            .setContentText(action.detail)
            .setStyle(Notification.BigTextStyle().bigText(action.detail))
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_stat_shotlist, "Add", acceptIntent)
            .addAction(R.drawable.ic_stat_shotlist, "Dismiss", dismissIntent)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build()
    }

    fun actionFromIntent(intent: Intent): ShotlistAction? {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return null
        val kind = intent.getStringExtra(EXTRA_KIND)?.let {
            runCatching { ActionKind.valueOf(it) }.getOrNull()
        } ?: return null
        val findingId = intent.getLongExtra(EXTRA_FINDING_ID, -1L).takeIf { it >= 0 }
        val startsAt = intent.getLongExtra(EXTRA_STARTS_AT, -1L).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
        return ShotlistAction(
            id = findingId?.let { "finding-$it" } ?: "reminder-${title.hashCode()}",
            findingId = findingId,
            kind = kind,
            title = title,
            detail = intent.getStringExtra(EXTRA_DETAIL).orEmpty(),
            source = "screenshot",
            startsAt = startsAt,
            location = intent.getStringExtra(EXTRA_LOCATION),
            confidence = 1f,
        )
    }

    fun notificationId(action: ShotlistAction): Int = action.id.hashCode()

    private fun Intent.putAction(action: ShotlistAction): Intent = apply {
        putExtra(EXTRA_FINDING_ID, action.findingId ?: -1L)
        putExtra(EXTRA_KIND, action.kind.name)
        putExtra(EXTRA_TITLE, action.title)
        putExtra(EXTRA_DETAIL, action.detail)
        putExtra(EXTRA_STARTS_AT, action.startsAt?.toEpochMilli() ?: -1L)
        putExtra(EXTRA_LOCATION, action.location)
    }

    private fun Finding.toNotificationAction(): ShotlistAction = ShotlistAction(
        id = "finding-$id",
        findingId = id,
        kind = if (type == "DEADLINE") ActionKind.Deadline else ActionKind.Event,
        title = title,
        detail = snippet,
        source = "screenshot #$shotId",
        confidence = confidence,
        startsAt = whenAt?.let(Instant::ofEpochMilli),
        location = payload.takeIf { it.isNotBlank() },
    )

    private fun ensureReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Screenshot reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Action cards for events and deadlines found in screenshots."
        }
        manager.createNotificationChannel(channel)
    }
}
