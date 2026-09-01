package app.shotlist.data

data class PurgeCandidate(
    val shotId: Long,
    val uri: String,
    val takenAt: Long,
    val reason: String,
)
