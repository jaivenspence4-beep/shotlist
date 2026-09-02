package app.shotlist.engine

import android.content.Context

/**
 * The one door the UI layers use. Consumers observe results through the Room
 * flows (ShotlistDb) — this only starts machinery.
 */
object EngineApi {
    private var observer: MediaObserver? = null

    /**
     * Call once at app start. When the classifier version bumps, suggestions
     * made by the older brain are purged and their shots re-scanned, so a fix
     * for junk findings actually cleans the user's inbox instead of only
     * improving future screenshots.
     */
    fun ensureFreshClassification(context: Context) {
        // NB: don't name this "app" — it shadows the app.shotlist package root.
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences("shotlist_engine", Context.MODE_PRIVATE)
        if (prefs.getInt("classifier_version", 0) >= Classifier.VERSION) return
        Thread {
            kotlinx.coroutines.runBlocking {
                val db = app.shotlist.data.ShotlistDb.get(appCtx)
                db.findings().purgeSuggested()
                db.shots().purgeOrphans()
            }
            prefs.edit().putInt("classifier_version", Classifier.VERSION).apply()
            // Partial access (Android 14 "select photos") counts: those users
            // were purged then never rescanned, leaving a permanently empty
            // inbox — Codex's t29 review catch.
            val permissions = buildList {
                add(android.Manifest.permission.READ_MEDIA_IMAGES)
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                }
            }
            val granted = permissions.any {
                androidx.core.content.ContextCompat.checkSelfPermission(appCtx, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (granted) backfill(appCtx, limit = 100)
        }.start()
    }

    /**
     * Begin watching for new screenshots. Idempotent. Call after permission.
     * Also schedules the periodic sweep (survives process death, which the
     * live observer does not) and runs an immediate catch-up sweep.
     */
    fun startObserving(context: Context) {
        SweepWorker.ensureScheduled(context.applicationContext)
        SweepWorker.sweepNow(context.applicationContext)
        app.shotlist.engine.digest.DigestWorker.ensureScheduled(context.applicationContext)
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
