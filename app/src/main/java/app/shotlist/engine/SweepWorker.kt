package app.shotlist.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * The reliability layer under the live observer. One UI (and most OEM battery
 * managers) kill the app process freely, taking the ContentObserver with it —
 * so a periodic sweep catches everything screenshotted while we were dead,
 * and an on-open sweep catches up instantly when the user returns.
 *
 * Over-querying is harmless: ingest dedupes by mediaId. The watermark only
 * bounds the query.
 */
class SweepWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val since = Watermark.get(applicationContext)
        val rows = MediaQueries.recentScreenshots(
            applicationContext, limit = 25, sinceEpochSec = since,
        )
        rows.forEach { IngestWorker.enqueue(applicationContext, it) }
        Watermark.advance(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "shotlist-sweep"

        fun ensureScheduled(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SweepWorker>(15, TimeUnit.MINUTES).build(),
            )
        }

        fun sweepNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<SweepWorker>().build())
        }
    }
}

/** Last-seen watermark, persisted so process death doesn't reset coverage. */
object Watermark {
    private const val PREFS = "shotlist_engine"
    private const val KEY = "last_sweep_sec"
    /** Overlap margin: media indexing can lag behind wall time on some OEMs. */
    private const val SLACK_SEC = 120L

    fun get(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY, System.currentTimeMillis() / 1000 - 24 * 3600)

    fun advance(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY, System.currentTimeMillis() / 1000 - SLACK_SEC)
            .apply()
    }
}
