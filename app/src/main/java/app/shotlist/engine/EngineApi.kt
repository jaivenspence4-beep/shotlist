package app.shotlist.engine

import android.content.Context

/**
 * The one door the UI layers use. Consumers observe results through the Room
 * flows (ShotlistDb) — this only starts machinery.
 */
object EngineApi {
    private var observer: MediaObserver? = null

    /**
     * Begin watching for new screenshots. Idempotent. Call after permission.
     * Also schedules the periodic sweep (survives process death, which the
     * live observer does not) and runs an immediate catch-up sweep.
     */
    fun startObserving(context: Context) {
        SweepWorker.ensureScheduled(context.applicationContext)
        SweepWorker.sweepNow(context.applicationContext)
        if (observer != null) return
        observer = MediaObserver(context.applicationContext).also { it.start() }
    }

    fun stopObserving() {
        observer?.stop()
        observer = null
    }

    /**
     * The rescue loop: ingest the newest [limit] existing screenshots.
     * Onboarding calls this for the backfill wow moment and watches
     * FindingDao flows for the reveal.
     */
    fun backfill(context: Context, limit: Int = 100) {
        MediaQueries.recentScreenshots(context, limit = limit)
            .forEach { IngestWorker.enqueue(context, it) }
    }
}
