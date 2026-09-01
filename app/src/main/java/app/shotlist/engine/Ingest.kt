package app.shotlist.engine

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/** Row from MediaStore that we believe is a screenshot. */
data class ScreenshotRow(val mediaId: Long, val uri: Uri, val takenAt: Long)

/**
 * Screenshot heuristics per docs/research: OEMs vary folder names and file
 * naming, so match on relative path OR display name.
 */
object ScreenshotFilter {
    private val nameHints = listOf("screenshot", "screen_shot", "screencap")

    fun looksLikeScreenshot(relativePath: String?, displayName: String?): Boolean {
        val path = relativePath?.lowercase().orEmpty()
        val name = displayName?.lowercase().orEmpty()
        return path.contains("screenshot") || nameHints.any { name.startsWith(it) || name.contains(it) }
    }
}

object MediaQueries {
    // RELATIVE_PATH exists only from API 29; querying it on 26-28 throws on the
    // first scan (caught by the Play-readiness audit). Filter falls back to
    // display-name hints there.
    private val projection = buildList {
        add(MediaStore.Images.Media._ID)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            add(MediaStore.Images.Media.RELATIVE_PATH)
        }
        add(MediaStore.Images.Media.DISPLAY_NAME)
        add(MediaStore.Images.Media.DATE_ADDED)
    }.toTypedArray()

    /** Newest screenshots first, up to [limit]. */
    fun recentScreenshots(context: Context, limit: Int, sinceEpochSec: Long = 0): List<ScreenshotRow> {
        val rows = mutableListOf<ScreenshotRow>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.DATE_ADDED} > ?",
            arrayOf(sinceEpochSec.toString()),
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathCol = c.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val nameCol = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (c.moveToNext() && rows.size < limit) {
                val path = if (pathCol >= 0) c.getString(pathCol) else null
                val name = if (nameCol >= 0) c.getString(nameCol) else null
                if (!ScreenshotFilter.looksLikeScreenshot(path, name)) continue
                val id = c.getLong(idCol)
                rows += ScreenshotRow(
                    mediaId = id,
                    uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    ),
                    takenAt = c.getLong(dateCol) * 1000,
                )
            }
        }
        return rows
    }
}

/**
 * Watches MediaStore for new images and enqueues ingest work for anything that
 * looks like a fresh screenshot. Lifetime-scoped to the app process for v1;
 * a foreground service can adopt it later if OEM battery policy demands.
 */
class MediaObserver(private val context: Context) {
    private var lastSeenSec = System.currentTimeMillis() / 1000
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            val fresh = MediaQueries.recentScreenshots(context, limit = 5, sinceEpochSec = lastSeenSec)
            if (fresh.isNotEmpty()) {
                lastSeenSec = System.currentTimeMillis() / 1000
                fresh.forEach { IngestWorker.enqueue(context, it) }
            }
        }
    }

    fun start() {
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer,
        )
    }

    fun stop() {
        context.contentResolver.unregisterContentObserver(observer)
    }
}

object IngestWorker {
    const val KEY_MEDIA_ID = "mediaId"
    const val KEY_URI = "uri"
    const val KEY_TAKEN_AT = "takenAt"

    /**
     * Share-sheet path. The grant on a shared uri can expire before WorkManager
     * runs, so the image is copied into app storage first (off the main thread),
     * then queued under a stable negative pseudo-id — never colliding with real
     * MediaStore ids, and still deduped by the unique mediaId index.
     */
    fun enqueueShared(context: Context, uri: Uri) {
        val app = context.applicationContext
        Thread {
            val dir = java.io.File(app.filesDir, "shared").apply { mkdirs() }
            val file = java.io.File(dir, "share-${System.currentTimeMillis()}.img")
            runCatching {
                val copied = app.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                    true
                } ?: false
                if (!copied) {
                    file.delete()
                    return@runCatching
                }
                val pseudoId = -(file.name.hashCode().toLong().let {
                    if (it == Long.MIN_VALUE) 1L else kotlin.math.abs(it)
                })
                enqueue(
                    app,
                    ScreenshotRow(
                        mediaId = pseudoId,
                        uri = Uri.fromFile(file),
                        takenAt = System.currentTimeMillis(),
                    ),
                )
            }.onFailure {
                // Never leave a partial copy behind on a failed share.
                file.delete()
            }
        }.start()
    }

    fun enqueue(context: Context, row: ScreenshotRow) {
        val req = OneTimeWorkRequestBuilder<OcrIngestWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(KEY_MEDIA_ID, row.mediaId)
                    .putString(KEY_URI, row.uri.toString())
                    .putLong(KEY_TAKEN_AT, row.takenAt)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(req)
    }
}
