package app.shotlist.engine.memories

import android.content.Context
import app.shotlist.data.Finding
import app.shotlist.data.Shot
import app.shotlist.data.ShotlistDb

/**
 * Time Machine (t68): picks today's memory — a screenshot from ~30, ~90, or
 * ~365 days ago (±3 days) that actually produced findings. Real moments beat
 * noise; vaulted-only shots never surface as memories, and a dismissed memory
 * stays dismissed (per-shot, persisted).
 */
object MemoryEngine {

    data class Memory(val shot: Shot, val findings: List<Finding>, val agoLabel: String)

    private val windows = listOf(
        365L to "One year ago",
        90L to "Three months ago",
        30L to "One month ago",
    )
    private const val SLACK_DAYS = 3L
    private const val DAY_MS = 24 * 3600_000L

    suspend fun todayMemory(context: Context): Memory? {
        val db = ShotlistDb.get(context)
        val prefs = context.getSharedPreferences("shotlist_memories", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        windows.forEach { (days, label) ->
            val center = now - days * DAY_MS
            val shots = db.shots().processedBetween(
                center - SLACK_DAYS * DAY_MS, center + SLACK_DAYS * DAY_MS,
            )
            shots.forEach { shot ->
                if (shot.mediaId < 0) return@forEach // share-copy: file deleted after OCR
                if (prefs.getBoolean("dismissed_${shot.id}", false)) return@forEach
                val findings = db.findings().forShot(shot.id)
                    .filter { it.state != "DISMISSED" }
                // A shot with ANY vaulted finding never becomes a memory: the
                // raw pixels would expose the secret even if the text list is
                // filtered (Codex t71 catch — mixed-vault screenshots).
                if (findings.isNotEmpty() && findings.none { it.vaulted }) {
                    return Memory(shot, findings, label)
                }
            }
        }
        return null
    }

    fun dismiss(context: Context, shotId: Long) {
        context.getSharedPreferences("shotlist_memories", Context.MODE_PRIVATE)
            .edit().putBoolean("dismissed_$shotId", true).apply()
    }
}
