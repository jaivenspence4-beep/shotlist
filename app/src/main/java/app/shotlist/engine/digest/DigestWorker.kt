package app.shotlist.engine.digest

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.shotlist.data.Finding
import app.shotlist.data.ShotlistDb
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * The Morning Digest (t70): one 8am notification — what's due today and
 * what's expiring soon. Spotify-Daily-Mix shape, with two trust laws:
 * it NEVER fires when there is nothing to say, and it never fires without
 * notification permission. When Time Machine's memory engine lands, its
 * one-memory line joins this digest rather than pinging separately.
 */
class DigestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val endOfDay = LocalDate.now(zone).atTime(LocalTime.MAX).atZone(zone)
            .toInstant().toEpochMilli()
        val in48h = now + 48 * 3600_000L

        val db = ShotlistDb.get(ctx)
        val dueToday = db.findings().dueBetween(now, endOfDay)
        val expiringSoon = db.findings().deadlinesBetween(endOfDay + 1, in48h)

        if (dueToday.isEmpty() && expiringSoon.isEmpty()) return Result.success()

        notify(ctx, dueToday, expiringSoon)
        return Result.success()
    }

    private fun notify(ctx: Context, due: List<Finding>, expiring: List<Finding>) {
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL, "Morning digest", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "One quiet summary of your day, from your screenshots." }
        )

        val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
        val lines = buildList {
            due.take(3).forEach { f ->
                val at = f.whenAt?.let {
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault(),
                    ).format(timeFmt)
                }
                add(if (at != null) "${f.title} · $at" else f.title)
            }
            expiring.take(2).forEach { add("Expiring soon: ${it.title}") }
        }
        val title = when {
            due.isNotEmpty() && expiring.isNotEmpty() ->
                "${due.size} today · ${expiring.size} expiring soon"
            due.isNotEmpty() -> if (due.size == 1) "1 thing today" else "${due.size} things today"
            else -> "${expiring.size} expiring soon"
        }

        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(ctx.packageName)
        val content = PendingIntent.getActivity(
            ctx, 8081, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val style = android.app.Notification.InboxStyle()
        lines.forEach { style.addLine(it) }
        manager.notify(
            8081,
            android.app.Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(app.shotlist.R.drawable.ic_stat_shotlist)
                .setContentTitle(title)
                .setContentText(lines.firstOrNull() ?: "")
                .setStyle(style)
                .setContentIntent(content)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        private const val CHANNEL = "shotlist_digest"
        private const val WORK = "shotlist-digest"

        /** Schedule daily at ~8am local. Idempotent (KEEP). */
        fun ensureScheduled(context: Context) {
            val zone = ZoneId.systemDefault()
            val nowDt = LocalDateTime.now(zone)
            var next = nowDt.toLocalDate().atTime(8, 0)
            if (!next.isAfter(nowDt)) next = next.plusDays(1)
            val delay = Duration.between(nowDt, next)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DigestWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                    .build(),
            )
        }
    }
}
