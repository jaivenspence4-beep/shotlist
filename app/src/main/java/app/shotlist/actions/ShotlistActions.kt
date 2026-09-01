package app.shotlist.actions

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import java.time.Instant

enum class ActionKind {
    Event,
    Deadline,
    Product,
    Place,
    Code,
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
)

object ShotlistActions {
    const val REMINDER_CHANNEL_ID = "shotlist_reminders"

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

    fun mapSearchIntent(action: ShotlistAction): Intent? {
        val query = action.location ?: action.detail.takeIf { it.isNotBlank() } ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
    }

    fun scheduleSoftReminder(context: Context, action: ShotlistAction, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ShotlistReminderReceiver::class.java)
            .putExtra("title", action.title)
            .putExtra("detail", action.detail)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            action.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    fun buildReminderNotification(context: Context, action: ShotlistAction): Notification {
        ensureReminderChannel(context)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            action.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(action.title)
            .setContentText(action.detail)
            .setStyle(Notification.BigTextStyle().bigText(action.detail))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

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
