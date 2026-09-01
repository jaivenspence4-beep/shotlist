package app.shotlist.diag

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local-only diagnostics: a size-capped log file plus a share intent, so
 * "it's buggy" can become an exact stack trace without any telemetry.
 * Nothing here ever transmits — sharing is a user-initiated ACTION_SEND.
 */
object Diag {
    private const val MAX_BYTES = 96 * 1024L
    private const val TAIL_KEEP = 64 * 1024
    @Volatile private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.filesDir, "diag.log")
        log("diag", "session start · ${Build.MANUFACTURER} ${Build.MODEL} · " +
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    /** Route fatal crashes into the log before the process dies. */
    fun installCrashHook() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log("crash", "uncaught on ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    @Synchronized
    fun log(tag: String, message: String, tr: Throwable? = null) {
        val file = logFile ?: return
        runCatching {
            val stamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            file.appendText(buildString {
                append(stamp).append(" [").append(tag).append("] ").append(message).append('\n')
                tr?.let { append(it.stackTraceToString()).append('\n') }
            })
            if (file.length() > MAX_BYTES) rotate(file)
        }
    }

    private fun rotate(file: File) {
        val bytes = file.readBytes()
        file.writeBytes(bytes.copyOfRange(bytes.size - TAIL_KEEP, bytes.size))
    }

    /** User-initiated bug report: the log as shareable text. */
    fun shareIntent(context: Context): Intent {
        val text = logFile?.takeIf { it.exists() }?.readText() ?: "(no log)"
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Shotlist bug report")
                .putExtra(Intent.EXTRA_TEXT, text.takeLast(90_000)),
            "Share bug report",
        )
    }
}
