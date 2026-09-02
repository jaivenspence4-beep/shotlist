package app.shotlist.onboarding

import android.content.Context

/**
 * A small, explicit preference signal from onboarding. The order matters: the
 * first choice should win ties when Inbox and daily quests are personalized.
 * Empty means balanced defaults, never a broken or empty experience.
 */
enum class FocusArea(
    val key: String,
    val title: String,
    val detail: String,
    val findingTypes: Set<String>,
    val questKeys: List<String>,
) {
    PLANS(
        key = "plans",
        title = "Plans & deadlines",
        detail = "Events, tickets, deliveries, and dates",
        findingTypes = setOf("EVENT", "DEADLINE", "TRACKING"),
        questKeys = listOf("handle_three", "clear_inbox", "scan_one"),
    ),
    SHOPPING(
        key = "shopping",
        title = "Things to buy & visit",
        detail = "Products, prices, links, and places",
        findingTypes = setOf("PRODUCT", "PLACE", "URL"),
        questKeys = listOf("scan_one", "handle_three", "clear_inbox"),
    ),
    DETAILS(
        key = "details",
        title = "Useful details",
        detail = "Codes, Wi-Fi, phone numbers, and passes",
        findingTypes = setOf("CODE", "WIFI", "PHONE"),
        questKeys = listOf("handle_three", "scan_one", "clear_inbox"),
    ),
    IDEAS(
        key = "ideas",
        title = "Ideas & recipes",
        detail = "Things worth trying again later",
        findingTypes = setOf("RECIPE", "MEME"),
        questKeys = listOf("scan_one", "clear_inbox", "handle_three"),
    ),
    ;

    companion object {
        fun fromKey(key: String): FocusArea? = entries.firstOrNull { it.key == key }
    }
}

object OnboardingPreferences {
    const val PREFS_NAME = "shotlist_onboarding"
    private const val KEY_FOCUS_ORDER = "focus_order"
    const val MAX_FOCUS_AREAS = 2

    fun read(context: Context): List<FocusArea> {
        val stored = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOCUS_ORDER, null)
            .orEmpty()
        return decode(stored)
    }

    fun write(context: Context, areas: List<FocusArea>) {
        val clean = areas.distinct().take(MAX_FOCUS_AREAS)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FOCUS_ORDER, encode(clean))
            .apply()
    }

    /** Lower values surface first. Unknown and unselected categories stay balanced. */
    fun findingPriority(type: String, areas: List<FocusArea>): Int {
        val focusIndex = areas.indexOfFirst { type in it.findingTypes }
        return if (focusIndex >= 0) focusIndex else areas.size
    }

    /** Stable keys keep the quest package independent from onboarding UI types. */
    fun preferredQuestKeys(areas: List<FocusArea>): List<String> =
        areas.flatMap(FocusArea::questKeys).distinct()

    fun encode(areas: List<FocusArea>): String =
        areas.distinct().take(MAX_FOCUS_AREAS).joinToString(",") { it.key }

    fun decode(value: String): List<FocusArea> =
        value.split(',')
            .mapNotNull { FocusArea.fromKey(it.trim()) }
            .distinct()
            .take(MAX_FOCUS_AREAS)
}
