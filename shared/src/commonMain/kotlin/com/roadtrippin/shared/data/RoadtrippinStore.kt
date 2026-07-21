package com.roadtrippin.shared.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.roadtrippin.shared.cloud.CloudServices
import com.roadtrippin.shared.domain.Achievement
import com.roadtrippin.shared.domain.AchievementScope
import com.roadtrippin.shared.domain.AppSettings
import com.roadtrippin.shared.domain.Country
import com.roadtrippin.shared.domain.JournalEntry
import com.roadtrippin.shared.domain.JournalEntryKind
import com.roadtrippin.shared.domain.JurisdictionCatalog
import com.roadtrippin.shared.domain.LocationStamp
import com.roadtrippin.shared.domain.PlateSighting
import com.roadtrippin.shared.domain.JournalPhoto
import com.roadtrippin.shared.domain.Region
import com.roadtrippin.shared.domain.TagQuantity
import com.roadtrippin.shared.domain.Traveler
import com.roadtrippin.shared.domain.Trip
import com.roadtrippin.shared.domain.SyncState
import com.roadtrippin.shared.platform.PlatformServices
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class AppScreen { HOME, DASHBOARD, PLATES, JOURNAL, MAP, AWARDS, TRIP_INFO, SETTINGS, ACCOUNT }

class RoadtrippinStore(
    private val repository: AppStateRepository? = AppStateRepositories.room,
) {
    private val scope = MainScope()
    private var cloudOnline = false
    private var cloudSessionId: String? = null
    val trips = mutableStateListOf<Trip>()
    private val outbox = mutableStateListOf<SyncMutation>()
    private val tombstones = mutableStateListOf<SyncTombstone>()
    var isLoaded by mutableStateOf(repository == null)
        private set
    var settings by mutableStateOf(AppSettings())
        private set
    var activeTripId by mutableStateOf<String?>(null)
        private set
    var screen by mutableStateOf(AppScreen.HOME)
    var syncState by mutableStateOf(SyncState.LOCAL_ONLY)
        private set
    var confirmedCloudAccountId by mutableStateOf<String?>(null)
        private set

    val pendingSyncCount: Int get() = outbox.size
    val retainedDeletionCount: Int get() = tombstones.size
    val localPhotoCount: Int get() = trips.sumOf { trip -> trip.journal.sumOf { it.photos.size } }

    val activeTrip: Trip? get() = trips.firstOrNull { it.id == activeTripId }

    init {
        repository?.let { persistedRepository ->
            scope.launch {
                persistedRepository.observe().collectLatest { persisted ->
                    if (persisted != null) {
                        settings = persisted.settings
                        trips.clear()
                        trips.addAll(persisted.trips)
                        activeTripId = persisted.activeTripId
                            ?.takeIf { id -> persisted.trips.any { it.id == id && it.isActive } }
                        outbox.clear()
                        outbox.addAll(persisted.outbox)
                        tombstones.clear()
                        tombstones.addAll(persisted.tombstones)
                        confirmedCloudAccountId = persisted.confirmedCloudAccountId
                        syncState = when {
                            persisted.outbox.isNotEmpty() -> SyncState.QUEUED
                            persisted.confirmedCloudAccountId != null -> SyncState.SYNCED
                            else -> SyncState.LOCAL_ONLY
                        }
                    }
                    isLoaded = true
                }
            }
        }
        if (repository != null) {
            scope.launch {
                combine(
                    PlatformServices.observeConnectivity(),
                    CloudServices.accounts.observeSession(),
                ) { online, session -> online to session }.collectLatest { (online, session) ->
                    val userId = session?.userId
                    cloudOnline = online
                    cloudSessionId = userId
                    if (
                        online && userId != null && confirmedCloudAccountId == userId &&
                        outbox.isNotEmpty() && syncState != SyncState.SYNCING
                    ) {
                        syncNow(userId)
                    }
                }
            }
        }
    }

    fun acceptSafety() {
        settings = settings.copy(safetyAccepted = true)
        persist()
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        persist()
    }

    fun startTrip(location: LocationStamp?) {
        activeTrip?.let { endTrip(location) }
        val now = PlatformServices.nowEpochMillis()
        val trip = Trip(
            id = PlatformServices.randomId(),
            startedAt = now,
            startLocation = location ?: LocationStamp(),
            modifiedAt = now,
        )
        trips.add(0, trip)
        activeTripId = trip.id
        screen = AppScreen.DASHBOARD
        persist()
    }

    fun openTrip(tripId: String) {
        activeTripId = tripId
        screen = AppScreen.DASHBOARD
        persist()
    }

    fun endTrip(location: LocationStamp?) {
        val trip = activeTrip ?: return
        val now = PlatformServices.nowEpochMillis()
        replaceTrip(trip.copy(endedAt = now, endLocation = location, modifiedAt = now), save = false)
        activeTripId = null
        screen = AppScreen.HOME
        persist()
    }

    fun reopenTrip(tripId: String) {
        activeTrip?.let { current ->
            replaceTrip(
                current.copy(endedAt = PlatformServices.nowEpochMillis(), modifiedAt = PlatformServices.nowEpochMillis()),
                save = false,
            )
        }
        val trip = trips.firstOrNull { it.id == tripId } ?: return
        replaceTrip(
            trip.copy(endedAt = null, endLocation = null, modifiedAt = PlatformServices.nowEpochMillis()),
            save = false,
        )
        activeTripId = tripId
        screen = AppScreen.DASHBOARD
        persist()
    }

    fun updateTripInfo(
        name: String,
        destination: String,
        vehicle: String,
        notes: String,
        travelerNames: List<String>,
    ) {
        val trip = activeTrip ?: return
        val existingByName = trip.travelers.associateBy { it.name.trim().lowercase() }
        val travelers = travelerNames.mapNotNull { raw ->
            raw.trim().takeIf(String::isNotBlank)?.let { name ->
                existingByName[name.lowercase()] ?: Traveler(PlatformServices.randomId(), name)
            }
        }
        val retainedIds = travelers.mapTo(mutableSetOf()) { it.id }
        trip.travelers.filterNot { it.id in retainedIds }.forEach {
            addTombstone("traveler", it.id)
        }
        replaceTrip(
            trip.copy(
                name = name.trim(),
                destination = destination.trim(),
                vehicle = vehicle.trim(),
                notes = notes.trim(),
                travelers = travelers,
                modifiedAt = PlatformServices.nowEpochMillis(),
            )
        )
    }

    fun recordSighting(code: String, location: LocationStamp?): Boolean {
        val trip = activeTrip ?: return false
        val existing = trip.sightings.firstOrNull { it.jurisdictionCode == code }
        if (existing != null) return false
        val now = PlatformServices.nowEpochMillis()
        val sightings = trip.sightings + PlateSighting(
            id = PlatformServices.randomId(),
            jurisdictionCode = code,
            firstSeenAt = now,
            location = location ?: LocationStamp(),
            modifiedAt = now,
        )
        replaceTrip(trip.copy(sightings = sightings, modifiedAt = now))
        return true
    }

    fun updateSighting(code: String, firstSeenAt: Long, location: LocationStamp) {
        val trip = activeTrip ?: return
        val existing = trip.sightings.firstOrNull { it.jurisdictionCode == code } ?: return
        val now = PlatformServices.nowEpochMillis()
        replaceTrip(
            trip.copy(
                sightings = trip.sightings.map { sighting ->
                    if (sighting.id == existing.id) sighting.copy(
                        firstSeenAt = firstSeenAt,
                        location = location,
                        modifiedAt = now,
                    ) else sighting
                },
                modifiedAt = now,
            )
        )
    }

    fun updateSightingPlace(code: String, placeName: String) {
        val trip = activeTrip ?: return
        val existing = trip.sightings.firstOrNull { it.jurisdictionCode == code } ?: return
        if (existing.location.placeName != null || placeName.isBlank()) return
        val now = PlatformServices.nowEpochMillis()
        replaceTrip(
            trip.copy(
                sightings = trip.sightings.map { sighting ->
                    if (sighting.id == existing.id) sighting.copy(
                        location = sighting.location.copy(placeName = placeName.trim()),
                        modifiedAt = now,
                    ) else sighting
                },
                modifiedAt = now,
            )
        )
    }

    fun removeSighting(code: String) {
        val trip = activeTrip ?: return
        trip.sightings.firstOrNull { it.jurisdictionCode == code }?.let {
            addTombstone("plate_sighting", it.id)
        }
        replaceTrip(
            trip.copy(
                sightings = trip.sightings.filterNot { it.jurisdictionCode == code },
                modifiedAt = PlatformServices.nowEpochMillis(),
            )
        )
    }

    fun addJournalEntry(
        kind: JournalEntryKind,
        title: String,
        body: String,
        tags: List<TagQuantity>,
        location: LocationStamp?,
        photos: List<JournalPhoto> = emptyList(),
    ): String? {
        val trip = activeTrip ?: return null
        val now = PlatformServices.nowEpochMillis()
        val entry = JournalEntry(
            id = PlatformServices.randomId(),
            kind = kind,
            title = title.trim(),
            body = body.trim(),
            occurredAt = now,
            location = location ?: LocationStamp(),
            tags = tags.filter { it.tag.isNotBlank() && it.quantity > 0 },
            photos = photos.distinctBy(JournalPhoto::id).take(MAX_PHOTOS_PER_ENTRY),
            modifiedAt = now,
        )
        replaceTrip(trip.copy(journal = listOf(entry) + trip.journal, modifiedAt = now))
        return entry.id
    }

    fun deleteJournalEntry(entryId: String) {
        val trip = activeTrip ?: return
        val entry = trip.journal.firstOrNull { it.id == entryId } ?: return
        entry.photos.forEach { addTombstone("journal_photo", it.id) }
        addTombstone("journal_entry", entry.id)
        replaceTrip(
            trip.copy(
                journal = trip.journal.filterNot { it.id == entryId },
                modifiedAt = PlatformServices.nowEpochMillis(),
            )
        )
    }

    fun updateJournalEntry(
        entryId: String,
        kind: JournalEntryKind,
        title: String,
        body: String,
        occurredAt: Long,
        location: LocationStamp,
        tags: List<TagQuantity>,
        photos: List<JournalPhoto>? = null,
    ) {
        val trip = activeTrip ?: return
        val existingEntry = trip.journal.firstOrNull { it.id == entryId } ?: return
        photos?.let { updatedPhotos ->
            val retainedIds = updatedPhotos.mapTo(mutableSetOf()) { it.id }
            existingEntry.photos.filterNot { it.id in retainedIds }.forEach {
                addTombstone("journal_photo", it.id)
            }
        }
        val now = PlatformServices.nowEpochMillis()
        replaceTrip(
            trip.copy(
                journal = trip.journal.map { entry ->
                    if (entry.id == entryId) entry.copy(
                        kind = kind,
                        title = title.trim(),
                        body = body.trim(),
                        occurredAt = occurredAt,
                        location = location,
                        tags = tags.filter { it.tag.isNotBlank() && it.quantity > 0 },
                        photos = photos?.distinctBy(JournalPhoto::id)?.take(MAX_PHOTOS_PER_ENTRY) ?: entry.photos,
                        modifiedAt = now,
                    ) else entry
                },
                modifiedAt = now,
            )
        )
    }

    fun updateJournalPlace(entryId: String, placeName: String) {
        val trip = activeTrip ?: return
        val existing = trip.journal.firstOrNull { it.id == entryId } ?: return
        if (existing.location.placeName != null || placeName.isBlank()) return
        val now = PlatformServices.nowEpochMillis()
        replaceTrip(
            trip.copy(
                journal = trip.journal.map { entry ->
                    if (entry.id == entryId) entry.copy(
                        location = entry.location.copy(placeName = placeName.trim()),
                        modifiedAt = now,
                    ) else entry
                },
                modifiedAt = now,
            )
        )
    }

    fun addJournalPhotos(entryId: String, photos: List<JournalPhoto>): Int {
        val trip = activeTrip ?: return 0
        val entry = trip.journal.firstOrNull { it.id == entryId } ?: return 0
        val combined = (entry.photos + photos).distinctBy(JournalPhoto::id).take(MAX_PHOTOS_PER_ENTRY)
        val added = combined.size - entry.photos.size
        if (added <= 0) return 0
        val now = PlatformServices.nowEpochMillis()
        replaceTrip(
            trip.copy(
                journal = trip.journal.map { existing ->
                    if (existing.id == entryId) existing.copy(photos = combined, modifiedAt = now) else existing
                },
                modifiedAt = now,
            )
        )
        return added
    }

    fun achievementsFor(trip: Trip): List<Achievement> = AchievementEvaluator.tripAchievements(trip)
    fun lifetimeAchievements(): List<Achievement> = AchievementEvaluator.lifetimeAchievements(trips)

    fun shareSummary(trip: Trip): String {
        val seen = trip.sightings.mapNotNull { JurisdictionCatalog.byCode[it.jurisdictionCode] }
        val states = seen.filter { it.country == Country.UNITED_STATES }.joinToString(", ") { it.code }
        return buildString {
            appendLine("${trip.displayName} — Roadtrippin")
            appendLine("${seen.size} of ${JurisdictionCatalog.all.size} plates spotted")
            if (states.isNotBlank()) appendLine("USA: $states")
            if (seen.any { it.code == "BC" }) appendLine("Canada: British Columbia")
            if (seen.any { it.code == "MX" }) appendLine("Mexico spotted")
            append("${trip.journal.size} road journal entries")
        }
    }

    suspend fun clearLocalData() {
        repository?.clear()
        trips.clear()
        activeTripId = null
        settings = AppSettings()
        outbox.clear()
        tombstones.clear()
        confirmedCloudAccountId = null
        syncState = SyncState.LOCAL_ONLY
        screen = AppScreen.HOME
    }

    fun cloudUploadNeedsConfirmation(userId: String): Boolean = confirmedCloudAccountId != userId

    suspend fun confirmCloudUploadAndSync(userId: String): Result<Unit> {
        confirmedCloudAccountId = userId
        persist(enqueue = false)
        return syncNow(userId)
    }

    suspend fun syncNow(userId: String): Result<Unit> {
        if (confirmedCloudAccountId != userId) {
            return Result.failure(IllegalStateException("Confirm this account before syncing device data."))
        }
        syncState = SyncState.SYNCING
        val localSnapshot = snapshot()
        return runCatching {
            val merged = CloudServices.sync.sync(localSnapshot, userId)
            settings = merged.settings
            trips.clear()
            trips.addAll(merged.trips)
            activeTripId = merged.activeTripId
                ?.takeIf { id -> merged.trips.any { it.id == id && it.isActive } }
            tombstones.clear()
            tombstones.addAll(merged.tombstones)
            outbox.clear()
            syncState = SyncState.SYNCED
            persist(enqueue = false)
        }.onFailure {
            for (index in outbox.indices) {
                outbox[index] = outbox[index].copy(attempts = outbox[index].attempts + 1)
            }
            syncState = SyncState.ERROR
            persist(enqueue = false)
        }
    }

    private fun replaceTrip(updated: Trip, save: Boolean = true) {
        val index = trips.indexOfFirst { it.id == updated.id }
        if (index >= 0) trips[index] = updated
        if (save) persist()
    }

    private fun addTombstone(entityType: String, entityId: String) {
        val replacement = SyncTombstone(
            entityType = entityType,
            entityId = entityId,
            deletedAt = PlatformServices.nowEpochMillis(),
            mutationId = PlatformServices.randomId(),
        )
        val index = tombstones.indexOfFirst {
            it.entityType == entityType && it.entityId == entityId
        }
        if (index >= 0) tombstones[index] = replacement else tombstones += replacement
    }

    private fun persist(enqueue: Boolean = true) {
        if (enqueue) {
            outbox += SyncMutation(
                id = PlatformServices.randomId(),
                createdAt = PlatformServices.nowEpochMillis(),
            )
            syncState = SyncState.QUEUED
        }
        val persistedRepository = repository ?: return
        val snapshot = snapshot()
        scope.launch {
            persistedRepository.save(snapshot)
            if (
                enqueue && cloudOnline && cloudSessionId != null &&
                confirmedCloudAccountId == cloudSessionId && syncState != SyncState.SYNCING
            ) {
                syncNow(checkNotNull(cloudSessionId))
            }
        }
    }

    private fun snapshot() = PersistedAppState(
            settings = settings,
            trips = trips.toList(),
            activeTripId = activeTripId,
            outbox = outbox.toList(),
            tombstones = tombstones.toList(),
            confirmedCloudAccountId = confirmedCloudAccountId,
        )

    companion object {
        const val MAX_PHOTOS_PER_ENTRY = 5
    }
}

internal object AchievementEvaluator {
    fun tripAchievements(trip: Trip): List<Achievement> {
        val seenCodes = trip.sightings.mapTo(mutableSetOf()) { it.jurisdictionCode }
        val stateCount = JurisdictionCatalog.usStates.count { it.code in seenCodes }
        val totalCount = seenCodes.size
        val countries = JurisdictionCatalog.all.filter { it.code in seenCodes }.mapTo(mutableSetOf()) { it.country }
        val notes = trip.journal.count { it.kind == JournalEntryKind.NOTE }
        val photos = trip.journal.sumOf { it.photos.size }
        val stops = trip.journal.count { it.kind == JournalEntryKind.STOP }
        val maxTagQuantity = trip.journal.flatMap { it.tags }.maxOfOrNull { it.quantity } ?: 0

        val achievements = mutableListOf<Achievement>()
        fun add(id: String, title: String, description: String, icon: String, progress: Int, target: Int) {
            achievements += Achievement(
                id, title, description, icon, AchievementScope.TRIP,
                progress >= target, progress.coerceAtMost(target), target,
            )
        }
        add("first_plate", "First Spot", "Find the first plate of the trip", "🚗", totalCount, 1)
        add("plates_10", "Eagle Eye", "Spot 10 plates", "👀", totalCount, 10)
        add("plates_25", "Plate Collector", "Spot 25 plates", "🪪", totalCount, 25)
        add("plates_40", "Road Scholar", "Spot 40 plates", "🛣️", totalCount, 40)
        add("all_50", "All 50", "Spot all 50 US states", "🇺🇸", stateCount, 50)
        add("all_53", "Continental Sweep", "Spot every Roadtrippin plate", "🏆", totalCount, 53)
        add("dc", "Capital Catch", "Spot Washington, D.C.", "🏛️", if ("DC" in seenCodes) 1 else 0, 1)
        add("bc", "North of the Border", "Spot British Columbia", "🍁", if ("BC" in seenCodes) 1 else 0, 1)
        add("mx", "South of the Border", "Spot Mexico", "🌵", if ("MX" in seenCodes) 1 else 0, 1)
        add("three_countries", "Three Countries", "Spot a plate from every country", "🌎", countries.size, 3)

        Region.entries.filterNot { it == Region.NON_US }.forEach { region ->
            val members = JurisdictionCatalog.all.filter { it.region == region }
            val progress = members.count { it.code in seenCodes }
            add(
                "region_${region.name.lowercase()}",
                "${region.displayName} Sweep",
                "Complete the ${region.displayName} region",
                "🗺️",
                progress,
                members.size,
            )
        }
        add("first_note", "Road Reporter", "Write a journal note", "✍️", notes, 1)
        add("notes_10", "Storyteller", "Write 10 journal notes", "📖", notes, 10)
        add("first_photo", "Shutterbug", "Attach a journal photo", "📷", photos, 1)
        add("first_stop", "Pit Stop", "Log a trip stop", "📍", stops, 1)
        add("tag_10", "Count It", "Record a tag quantity of 10", "🔢", maxTagQuantity, 10)
        return achievements
    }

    fun lifetimeAchievements(trips: List<Trip>): List<Achievement> {
        val completed = trips.count { !it.isActive }
        val sightings = trips.sumOf { it.sightings.size }
        val notes = trips.sumOf { trip -> trip.journal.count { it.kind == JournalEntryKind.NOTE } }
        val photos = trips.sumOf { trip -> trip.journal.sumOf { it.photos.size } }
        return listOf(
            lifetime("trip_1", "First Finish", "Complete a trip", "🏁", completed, 1),
            lifetime("trip_5", "Frequent Traveler", "Complete five trips", "🧳", completed, 5),
            lifetime("trip_10", "Road Warrior", "Complete ten trips", "🚙", completed, 10),
            lifetime("sightings_100", "Century Spotter", "Record 100 plate sightings", "💯", sightings, 100),
            lifetime("sightings_250", "Plate Legend", "Record 250 plate sightings", "⭐", sightings, 250),
            lifetime("notes_50", "Travel Author", "Write 50 journal notes", "📚", notes, 50),
            lifetime("photos_25", "Memory Keeper", "Attach 25 journal photos", "🖼️", photos, 25),
        )
    }

    private fun lifetime(
        id: String,
        title: String,
        description: String,
        icon: String,
        progress: Int,
        target: Int,
    ) = Achievement(
        id, title, description, icon, AchievementScope.LIFETIME,
        progress >= target, progress.coerceAtMost(target), target,
    )
}
