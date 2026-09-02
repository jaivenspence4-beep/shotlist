package app.shotlist.ui.quests

import android.content.Context
import app.shotlist.data.ShotlistDb
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class DailyQuest(
    val key: String,
    val title: String,
    val detail: String,
    val target: Int,
    val xp: Int,
) {
    HANDLE_THREE(
        key = "handle_three",
        title = "Handle three finds",
        detail = "Add, save, snooze, or clear three cards.",
        target = 3,
        xp = 30,
    ),
    SCAN_ONE(
        key = "scan_one",
        title = "Scan something useful",
        detail = "Anything, QR, or Docs all count.",
        target = 1,
        xp = 20,
    ),
    CLEAR_INBOX(
        key = "clear_inbox",
        title = "Leave nothing waiting",
        detail = "Take the inbox down to zero.",
        target = 1,
        xp = 40,
    ),
}

data class QuestProgress(
    val quest: DailyQuest,
    val progress: Int,
    val complete: Boolean,
)

data class LevelProgress(
    val level: Int,
    val totalXp: Int,
    val xpInLevel: Int,
    val xpForNextLevel: Int,
) {
    val fraction: Float
        get() = (xpInLevel.toFloat() / xpForNextLevel.coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class QuestDashboard(
    val day: LocalDate,
    val quests: List<QuestProgress>,
    val level: LevelProgress,
) {
    val completedCount: Int get() = quests.count { it.complete }
}

/**
 * Daily quests use snapshots of records Shotlist already needs: current finding
 * states and scan rows. No behavioral event stream is added. A once-per-day
 * baseline turns those monotonic counts into today's progress, while awarded XP
 * and completed quest keys make recomposition and process restarts idempotent.
 */
object DailyQuestEngine {
    private const val PREFS = "shotlist_onboarding"
    private const val KEY_DAY = "quest_day"
    private const val KEY_BASE_HANDLED = "quest_base_handled"
    private const val KEY_BASE_SCANS = "quest_base_scans"
    private const val KEY_BASE_SUGGESTED = "quest_base_suggested"
    private const val KEY_COMPLETED = "quest_completed"
    private const val KEY_TOTAL_XP = "quest_total_xp"

    private val evaluationMutex = Mutex()

    fun observe(context: Context): Flow<QuestDashboard> {
        val appContext = context.applicationContext
        val db = ShotlistDb.get(appContext)
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        return combine(
            db.findings().suggestedCount(),
            db.findings().handledCount(),
            db.scans().count(),
            localDayFlow(),
        ) { suggested, handled, scans, day ->
            SourceSnapshot(day, suggested, handled, scans)
        }.map { source ->
            evaluationMutex.withLock { evaluate(prefs, source) }
        }.distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    private fun evaluate(
        prefs: android.content.SharedPreferences,
        source: SourceSnapshot,
    ): QuestDashboard {
        val storedDay = prefs.getString(KEY_DAY, null)
        val today = source.day.toString()
        if (storedDay != today) {
            prefs.edit()
                .putString(KEY_DAY, today)
                .putInt(KEY_BASE_HANDLED, source.handled)
                .putInt(KEY_BASE_SCANS, source.scans)
                .putInt(KEY_BASE_SUGGESTED, source.suggested)
                .putStringSet(KEY_COMPLETED, emptySet())
                .commit()
        }

        val baselineHandled = prefs.getInt(KEY_BASE_HANDLED, source.handled)
        val baselineScans = prefs.getInt(KEY_BASE_SCANS, source.scans)
        val baselineSuggested = prefs.getInt(KEY_BASE_SUGGESTED, source.suggested)
        val quests = buildList {
            add(DailyQuest.HANDLE_THREE)
            add(DailyQuest.SCAN_ONE)
            if (baselineSuggested > 0) add(DailyQuest.CLEAR_INBOX)
        }
        val rawProgress = mapOf(
            DailyQuest.HANDLE_THREE to (source.handled - baselineHandled).coerceAtLeast(0),
            DailyQuest.SCAN_ONE to (source.scans - baselineScans).coerceAtLeast(0),
            DailyQuest.CLEAR_INBOX to if (baselineSuggested > 0 && source.suggested == 0) 1 else 0,
        )

        val completed = prefs.getStringSet(KEY_COMPLETED, emptySet()).orEmpty().toMutableSet()
        val newlyCompleted = quests.filter { quest ->
            rawProgress.getValue(quest) >= quest.target && quest.key !in completed
        }
        var totalXp = prefs.getInt(KEY_TOTAL_XP, 0).coerceAtLeast(0)
        if (newlyCompleted.isNotEmpty()) {
            completed += newlyCompleted.map { it.key }
            totalXp += newlyCompleted.sumOf { it.xp }
            prefs.edit()
                .putStringSet(KEY_COMPLETED, completed)
                .putInt(KEY_TOTAL_XP, totalXp)
                .commit()
        }

        return QuestDashboard(
            day = source.day,
            quests = quests.map { quest ->
                val complete = quest.key in completed
                QuestProgress(
                    quest = quest,
                    progress = if (complete) {
                        quest.target
                    } else {
                        rawProgress.getValue(quest).coerceAtMost(quest.target)
                    },
                    complete = complete,
                )
            },
            level = levelProgress(totalXp),
        )
    }

    internal fun levelProgress(totalXp: Int): LevelProgress {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        var threshold = xpNeeded(level)
        while (remaining >= threshold) {
            remaining -= threshold
            level += 1
            threshold = xpNeeded(level)
        }
        return LevelProgress(
            level = level,
            totalXp = totalXp.coerceAtLeast(0),
            xpInLevel = remaining,
            xpForNextLevel = threshold,
        )
    }

    private fun xpNeeded(level: Int): Int = 60 + level * 20

    private data class SourceSnapshot(
        val day: LocalDate,
        val suggested: Int,
        val handled: Int,
        val scans: Int,
    )

    private fun localDayFlow(): Flow<LocalDate> = flow {
        while (true) {
            val now = ZonedDateTime.now()
            emit(now.toLocalDate())
            val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val untilTomorrow = Duration.between(now, nextDay).toMillis().coerceAtLeast(1_000L)
            delay(untilTomorrow + 250L)
        }
    }
}
