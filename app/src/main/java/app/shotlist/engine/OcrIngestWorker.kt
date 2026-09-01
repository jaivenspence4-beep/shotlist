package app.shotlist.engine

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.shotlist.data.Shot
import app.shotlist.data.ShotlistDb
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** OCR one screenshot, extract, classify, store. Fully on-device. */
class OcrIngestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // MediaStore ids are positive; share-sheet pseudo-ids are negative.
        // 0 is the only invalid value (missing input).
        val mediaId = inputData.getLong(IngestWorker.KEY_MEDIA_ID, 0)
        val uriStr = inputData.getString(IngestWorker.KEY_URI) ?: return Result.failure()
        val takenAt = inputData.getLong(IngestWorker.KEY_TAKEN_AT, System.currentTimeMillis())
        if (mediaId == 0L) return Result.failure()

        val db = ShotlistDb.get(applicationContext)

        // Dedupe — but a row still in NEW state is an earlier attempt that
        // failed before OCR (Result.retry lands here again): resume it instead
        // of declaring victory, or retries would never actually retry.
        val existing = db.shots().byMediaId(mediaId)
        if (existing != null && existing.status != "NEW") return Result.success()

        val shotId = existing?.id ?: db.shots().insert(
            Shot(mediaId = mediaId, uri = uriStr, takenAt = takenAt)
        )
        if (shotId <= 0) return Result.success() // conflict: another worker got it

        val text = runCatching { ocr(Uri.parse(uriStr)) }.getOrElse { return Result.retry() }

        val signals = Extractor.extract(text)
        val findings = Classifier.classify(shotId, text, signals)
        db.findings().insertAll(findings)
        db.shots().markProcessed(
            id = shotId,
            text = text,
            status = if (findings.isEmpty()) "IGNORED" else "PROCESSED",
        )

        // Retention: nothing in the app reads a share-sheet copy after OCR —
        // no consumer renders Shot.uri — so every copy is deleted once its
        // text and findings are stored. If thumbnails ship later, this becomes
        // conditional again.
        if (mediaId < 0) {
            val uri = Uri.parse(uriStr)
            if (uri.scheme == "file") {
                uri.path?.let { path ->
                    val file = java.io.File(path)
                    if (file.parentFile?.name == "shared") file.delete()
                }
            }
        }
        return Result.success()
    }

    private suspend fun ocr(uri: Uri): String {
        val image = InputImage.fromFilePath(applicationContext, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return recognizer.process(image).await().text
    }
}

/** Minimal Task→coroutine bridge; avoids pulling in play-services coroutines. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
