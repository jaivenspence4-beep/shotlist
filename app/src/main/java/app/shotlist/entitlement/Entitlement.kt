package app.shotlist.entitlement

private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L

/**
 * Local product policy only. Billing is deliberately not connected yet; a
 * debug-only switch may select either state so both experiences can be tested.
 */
enum class Entitlement(
    val recallHistoryDays: Int?,
    val vaultItemLimit: Int?,
) {
    FREE(recallHistoryDays = 30, vaultItemLimit = 3),
    PRO(recallHistoryDays = null, vaultItemLimit = null),
    ;

    val isPro: Boolean get() = this == PRO

    fun includesRecallShot(takenAt: Long, now: Long = System.currentTimeMillis()): Boolean {
        val days = recallHistoryDays ?: return true
        return takenAt >= now - days * DAY_MILLIS
    }

    fun canAddVaultItem(currentCount: Int): Boolean =
        vaultItemLimit?.let { currentCount < it } ?: true

    companion object {
        const val DEBUG_PREF_KEY = "debug_entitlement"

        fun fromStored(raw: String?): Entitlement =
            entries.firstOrNull { it.name == raw } ?: FREE
    }
}
