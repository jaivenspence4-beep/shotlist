package app.shotlist.health.api

import app.shotlist.data.GlucoseSample
import app.shotlist.data.GlucoseSyncState

/** In-memory Health Connect: records per origin, tokens that replay a change log. */
class FakeHealthGateway : HealthGateway {
    var availability = HealthAvailability.AVAILABLE
    var permitted = true
    var pageSize = 2
    var revokeThrows = false
    var revokeCalls = 0

    val records = LinkedHashMap<String, GlucoseRecord>()
    private val log = ArrayList<GlucoseChange>()
    private val expiredTokens = HashSet<String>()
    private var tokenGeneration = 0

    fun write(record: GlucoseRecord) {
        records[record.recordId] = record
        log += GlucoseChange.Upsert(record)
    }

    fun delete(recordId: String) {
        records.remove(recordId)
        log += GlucoseChange.Delete(recordId)
    }

    fun expire(token: String) { expiredTokens += token }

    override suspend fun availability() = availability
    override suspend fun hasReadPermission() = permitted

    override suspend fun origins(from: Long, until: Long): List<String> =
        records.values.filter { it.observedAt >= from && it.observedAt < until }.map { it.sourcePackage }.distinct().sorted()

    override suspend fun readSnapshot(origin: String, from: Long, until: Long, pageToken: String?): GlucosePage {
        val all = records.values
            .filter { it.sourcePackage == origin && it.observedAt >= from && it.observedAt < until }
            .sortedBy { it.observedAt }
        val start = pageToken?.toInt() ?: 0
        val page = all.drop(start).take(pageSize)
        val next = (start + pageSize).takeIf { it < all.size }?.toString()
        return GlucosePage(page, next)
    }

    /** Tokens are unique per issue so an expired one can never be re-issued. */
    override suspend fun changesToken(origin: String): String = "t${log.size}g${++tokenGeneration}"

    override suspend fun getChanges(token: String): ChangesPage {
        if (token in expiredTokens) throw TokenExpiredException()
        val start = token.removePrefix("t").substringBefore("g").toInt()
        val page = log.drop(start).take(pageSize)
        val nextIndex = start + page.size
        return ChangesPage(page, "t${nextIndex}g${++tokenGeneration}", hasMore = nextIndex < log.size)
    }

    override suspend fun revokeAll() {
        revokeCalls++
        if (revokeThrows) throw IllegalStateException("provider gone")
    }
}

class FakeGlucoseStore : GlucoseStore {
    var state = GlucoseSyncState()
    val samples = LinkedHashMap<Pair<String, String>, GlucoseSample>()
    var momentsWiped = false

    override suspend fun syncState() = state
    override suspend fun saveSyncState(state: GlucoseSyncState) { this.state = state }

    override suspend fun replaceWindow(origin: String, from: Long, until: Long, rows: List<GlucoseSample>) {
        samples.entries.removeIf { (key, row) ->
            key.first == origin && row.observedAt >= from && row.observedAt < until
        }
        upsertAll(rows)
    }

    override suspend fun upsertAll(rows: List<GlucoseSample>) {
        rows.forEach { samples[it.sourcePackage to it.recordId] = it }
    }

    override suspend fun deleteByRecordId(origin: String, recordId: String): Int {
        val before = samples.size
        samples.remove(origin to recordId)
        return before - samples.size
    }

    override suspend fun deleteAllHealth() {
        samples.clear()
        momentsWiped = true
        state = GlucoseSyncState()
    }
}

fun record(
    id: String,
    observedAt: Long,
    mmol: Double = 5.5,
    origin: String = "com.abbott.lingo",
    specimen: SpecimenSource = SpecimenSource.INTERSTITIAL_FLUID,
) = GlucoseRecord(
    recordId = id,
    sourcePackage = origin,
    observedAt = observedAt,
    zoneOffsetSeconds = -25200,
    mmolPerLiter = mmol,
    specimen = specimen,
)

fun sample(
    id: String,
    observedAt: Long,
    mmol: Double = 5.5,
    origin: String = "com.abbott.lingo",
    specimen: SpecimenSource = SpecimenSource.INTERSTITIAL_FLUID,
) = GlucoseSample(
    sourcePackage = origin,
    recordId = id,
    observedAt = observedAt,
    zoneOffsetSeconds = null,
    mmolPerLiter = mmol,
    specimenSource = specimen.name,
    importedAt = 0L,
)
