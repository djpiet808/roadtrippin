package com.roadtrippin.shared.cloud

import com.roadtrippin.shared.data.PersistedAppState
import com.roadtrippin.shared.data.SyncTombstone
import com.roadtrippin.shared.data.AchievementEvaluator
import com.roadtrippin.shared.domain.JournalEntry
import com.roadtrippin.shared.domain.JournalEntryKind
import com.roadtrippin.shared.domain.JournalPhoto
import com.roadtrippin.shared.domain.LocationStamp
import com.roadtrippin.shared.domain.PlateSighting
import com.roadtrippin.shared.domain.TagQuantity
import com.roadtrippin.shared.domain.Traveler
import com.roadtrippin.shared.domain.Trip
import com.roadtrippin.shared.platform.PlatformServices
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Reconciles the device snapshot with the normalized Roadtrippin schema. Room remains the
 * authority observed by the UI; this service only returns a merged snapshot after every cloud
 * write has succeeded.
 */
class SupabaseSyncService(
    private val client: SupabaseClient?,
) {
    suspend fun sync(local: PersistedAppState, userId: String): PersistedAppState {
        val supabase = checkNotNull(client) { "Supabase is not configured in this build." }
        val authenticatedId = supabase.auth.currentSessionOrNull()?.user?.id
        check(authenticatedId == userId) { "The active Supabase session changed before sync." }

        val localWithUploadedPhotos = uploadPendingPhotos(supabase, local, userId)
        val cloudTombstones = supabase.from("sync_tombstones").select().decodeList<TombstoneRow>()
            .map(TombstoneRow::toDomain)
        val tombstones = mergeTombstones(localWithUploadedPhotos.tombstones, cloudTombstones)

        val localRows = LocalRows.from(localWithUploadedPhotos.trips, userId)
        val remoteTrips = supabase.from("trips").select().decodeList<TripRow>()
        val remoteTravelers = supabase.from("travelers").select().decodeList<TravelerRow>()
        val remoteSightings = supabase.from("plate_sightings").select().decodeList<SightingRow>()
        val remoteEntries = supabase.from("journal_entries").select().decodeList<JournalEntryRow>()
        val remoteTags = supabase.from("tags").select().decodeList<TagRow>()
        val remoteEntryTags = supabase.from("journal_entry_tags").select().decodeList<EntryTagRow>()
        val remotePhotos = supabase.from("journal_photos").select().decodeList<PhotoRow>()
        val remoteAwards = supabase.from("achievement_awards").select().decodeList<AchievementAwardRow>()
        val localPhotoPaths = localWithUploadedPhotos.trips
            .flatMap { trip -> trip.journal.flatMap(JournalEntry::photos) }
            .filter { it.localPath.isNotBlank() }
            .associate { it.id to it.localPath }

        val trips = mergeRows(localRows.trips, remoteTrips, tombstones, "trip")
        val travelers = mergeRows(localRows.travelers, remoteTravelers, tombstones, "traveler")
            .filterValues { it.tripId in trips.keys }
        val sightings = mergeRows(localRows.sightings, remoteSightings, tombstones, "plate_sighting")
            .filterValues { it.tripId in trips.keys }
        val entries = mergeRows(localRows.entries, remoteEntries, tombstones, "journal_entry")
            .filterValues { it.tripId in trips.keys }
        val photos = mergeRows(localRows.photos, remotePhotos, tombstones, "journal_photo")
            .filterValues { it.tripId in trips.keys && it.entryId in entries.keys }
        val hydratedPhotoPaths = hydrateRemotePhotos(supabase, photos.values, localPhotoPaths)

        val localEntryIds = localRows.entries.keys
        val entryTags = entries.values.flatMap { entry ->
            val localWon = entry.id in localEntryIds && entry == localRows.entries[entry.id]
            val source = if (localWon) localRows.entryTags else remoteEntryTags
            source.filter { it.entryId == entry.id }
        }
        val referencedTagIds = entryTags.mapTo(mutableSetOf()) { it.tagId }
        val tags = mergeRows(localRows.tags, remoteTags, tombstones, "tag")
            .filterKeys { it in referencedTagIds }

        deleteTombstonedRows(supabase, tombstones, remotePhotos)
        upsertAll(supabase, "trips", trips.values, "user_id,id")
        upsertAll(supabase, "travelers", travelers.values, "user_id,id")
        upsertAll(supabase, "plate_sightings", sightings.values, "user_id,id")
        upsertAll(supabase, "journal_entries", entries.values, "user_id,id")
        upsertAll(supabase, "tags", tags.values, "user_id,id")

        // Tag quantities are part of the journal-entry edit version. Replacing associations for
        // that entry prevents a removed tag from being resurrected by a stale device.
        entries.keys.forEach { entryId ->
            supabase.from("journal_entry_tags").delete { filter { eq("entry_id", entryId) } }
        }
        upsertAll(supabase, "journal_entry_tags", entryTags, "user_id,entry_id,tag_id")
        upsertAll(supabase, "journal_photos", photos.values, "user_id,id")
        upsertAll(
            supabase,
            "sync_tombstones",
            tombstones.map { TombstoneRow.from(it, userId) },
            "user_id,entity_type,entity_id",
        )

        val mergedTrips = rebuildTrips(
            trips = trips.values,
            travelers = travelers.values,
            sightings = sightings.values,
            entries = entries.values,
            tags = tags.values,
            entryTags = entryTags,
            photos = photos.values,
            unsyncedLocalPhotos = localRows.unsyncedPhotos,
            localPhotoPaths = hydratedPhotoPaths,
        )
        syncAchievements(supabase, mergedTrips, remoteAwards, userId)
        val activeId = localWithUploadedPhotos.activeTripId?.takeIf { id -> mergedTrips.any { it.id == id && it.isActive } }
            ?: mergedTrips.filter(Trip::isActive).maxByOrNull(Trip::modifiedAt)?.id
        return localWithUploadedPhotos.copy(
            trips = mergedTrips.sortedByDescending(Trip::startedAt),
            activeTripId = activeId,
            tombstones = tombstones,
        )
    }

    private suspend fun deleteTombstonedRows(
        supabase: SupabaseClient,
        tombstones: List<SyncTombstone>,
        remotePhotos: List<PhotoRow>,
    ) {
        val tables = mapOf(
            "journal_photo" to "journal_photos",
            "journal_entry" to "journal_entries",
            "plate_sighting" to "plate_sightings",
            "traveler" to "travelers",
            "achievement_award" to "achievement_awards",
            "tag" to "tags",
            "trip" to "trips",
        )
        val deletedPhotoIds = tombstones.filter { it.entityType == "journal_photo" }.mapTo(mutableSetOf()) { it.entityId }
        val deletedEntryIds = tombstones.filter { it.entityType == "journal_entry" }.mapTo(mutableSetOf()) { it.entityId }
        val deletedTripIds = tombstones.filter { it.entityType == "trip" }.mapTo(mutableSetOf()) { it.entityId }
        val storagePaths = remotePhotos.filter {
            it.id in deletedPhotoIds || it.entryId in deletedEntryIds || it.tripId in deletedTripIds
        }.map(PhotoRow::storagePath)
        if (storagePaths.isNotEmpty()) {
            supabase.storage.from(PHOTO_BUCKET).delete(*storagePaths.toTypedArray())
        }
        tombstones.sortedBy { if (it.entityType == "trip") 1 else 0 }.forEach { tombstone ->
            val table = tables[tombstone.entityType] ?: return@forEach
            supabase.from(table).delete { filter { eq("id", tombstone.entityId) } }
        }
    }

    private suspend fun uploadPendingPhotos(
        supabase: SupabaseClient,
        local: PersistedAppState,
        userId: String,
    ): PersistedAppState {
        val bucket = supabase.storage.from(PHOTO_BUCKET)
        val updatedTrips = local.trips.map { trip ->
            trip.copy(journal = trip.journal.map { entry ->
                entry.copy(photos = entry.photos.map { photo ->
                    if (photo.remotePath != null) return@map photo
                    val bytes = checkNotNull(PlatformServices.readLocalFile(photo.localPath)) {
                        "A queued journal photo is no longer available on this device."
                    }
                    val path = "$userId/${trip.id}/${entry.id}/${photo.id}"
                    bucket.upload(path, bytes) {
                        upsert = true
                        contentType = ContentType.Image.JPEG
                    }
                    photo.copy(remotePath = path)
                })
            })
        }
        return local.copy(trips = updatedTrips)
    }

    private suspend fun hydrateRemotePhotos(
        supabase: SupabaseClient,
        photos: Collection<PhotoRow>,
        knownPaths: Map<String, String>,
    ): Map<String, String> {
        val result = knownPaths.toMutableMap()
        val bucket = supabase.storage.from(PHOTO_BUCKET)
        photos.forEach { photo ->
            if (result[photo.id].isNullOrBlank()) {
                val bytes = bucket.downloadAuthenticated(photo.storagePath)
                PlatformServices.saveJournalPhoto(photo.id, bytes)?.let { result[photo.id] = it }
            }
        }
        return result
    }

    private suspend fun syncAchievements(
        supabase: SupabaseClient,
        trips: List<Trip>,
        remoteAwards: List<AchievementAwardRow>,
        userId: String,
    ) {
        val desired = buildList {
            trips.forEach { trip ->
                AchievementEvaluator.tripAchievements(trip).filter { it.earned }.forEach { achievement ->
                    add(achievement.id to trip.id)
                }
            }
            AchievementEvaluator.lifetimeAchievements(trips).filter { it.earned }.forEach { achievement ->
                add(achievement.id to null)
            }
        }.toSet()
        val existingByKey = remoteAwards.associateBy { it.achievementId to it.tripId }
        remoteAwards.filterNot { (it.achievementId to it.tripId) in desired }.forEach { stale ->
            supabase.from("achievement_awards").delete { filter { eq("id", stale.id) } }
        }
        val now = PlatformServices.nowEpochMillis()
        val rows = desired.map { key ->
            existingByKey[key] ?: AchievementAwardRow.from(
                achievementId = key.first,
                tripId = key.second,
                userId = userId,
                earnedAt = now,
                modifiedAt = key.second?.let { id -> trips.firstOrNull { it.id == id }?.modifiedAt } ?: now,
            )
        }
        upsertAll(supabase, "achievement_awards", rows, "user_id,id")
    }

    private companion object {
        const val PHOTO_BUCKET = "roadtrippin-journal-photos"
    }
}

private interface VersionedRow {
    val id: String
    val modifiedAt: String
    val mutationId: String
}

private fun <T : VersionedRow> mergeRows(
    local: Map<String, T>,
    remote: List<T>,
    tombstones: List<SyncTombstone>,
    entityType: String,
): Map<String, T> {
    val deleted = tombstones.filter { it.entityType == entityType }.associateBy { it.entityId }
    return (local.keys + remote.map { it.id }).associateWithNotNull { id ->
        val localRow = local[id]
        val remoteRow = remote.firstOrNull { it.id == id }
        val winner = when {
            localRow == null -> remoteRow
            remoteRow == null -> localRow
            winningSide(rowVersion(localRow), rowVersion(remoteRow)) <= 0 -> localRow
            else -> remoteRow
        } ?: return@associateWithNotNull null
        val tombstone = deleted[id]
        if (tombstone != null && winningSide(rowVersion(winner), tombstoneVersion(tombstone)) >= 0) null else winner
    }
}

private fun rowVersion(row: VersionedRow): Pair<Long, String> =
    parseEpoch(row.modifiedAt) to row.mutationId

private fun tombstoneVersion(tombstone: SyncTombstone): Pair<Long, String> =
    tombstone.deletedAt to tombstone.mutationId

internal fun winningSide(local: Pair<Long, String>, remote: Pair<Long, String>): Int {
    val time = remote.first.compareTo(local.first)
    return if (time != 0) time else remote.second.compareTo(local.second)
}

private fun mergeTombstones(
    local: List<SyncTombstone>,
    remote: List<SyncTombstone>,
): List<SyncTombstone> = (local + remote)
    .groupBy { it.entityType to it.entityId }
    .values
    .map { values -> values.maxWith(compareBy<SyncTombstone> { it.deletedAt }.thenBy { it.mutationId }) }

private suspend inline fun <reified T : Any> upsertAll(
    client: SupabaseClient,
    table: String,
    rows: Collection<T>,
    conflict: String,
) {
    if (rows.isEmpty()) return
    client.from(table).upsert(rows.toList()) { onConflict = conflict }
}

private data class LocalRows(
    val trips: Map<String, TripRow>,
    val travelers: Map<String, TravelerRow>,
    val sightings: Map<String, SightingRow>,
    val entries: Map<String, JournalEntryRow>,
    val tags: Map<String, TagRow>,
    val entryTags: List<EntryTagRow>,
    val photos: Map<String, PhotoRow>,
    val unsyncedPhotos: Map<String, List<JournalPhoto>>,
) {
    companion object {
        fun from(trips: List<Trip>, userId: String): LocalRows {
            val tripRows = trips.associate { trip ->
                val base = TripRow.from(trip, userId)
                trip.id to base.copy(mutationId = mutationId(base.id, trip.modifiedAt, Json.encodeToString(base)))
            }
            val travelerRows = trips.flatMap { trip ->
                trip.travelers.map { TravelerRow.from(it, trip, userId) }
            }.associateBy(TravelerRow::id)
            val sightingRows = trips.flatMap { trip ->
                trip.sightings.map { SightingRow.from(it, trip.id, userId) }
            }.associateBy(SightingRow::id)
            val entryRows = trips.flatMap { trip ->
                trip.journal.map { JournalEntryRow.from(it, trip.id, userId) }
            }.associateBy(JournalEntryRow::id)
            val allTags = mutableMapOf<String, TagRow>()
            val joins = mutableListOf<EntryTagRow>()
            val syncedPhotos = mutableMapOf<String, PhotoRow>()
            val unsyncedPhotos = mutableMapOf<String, List<JournalPhoto>>()
            trips.forEach { trip ->
                trip.journal.forEach { entry ->
                    entry.tags.forEach { quantity ->
                        val tagId = stableUuid("tag:${quantity.tag.trim().lowercase()}")
                        val row = TagRow.from(tagId, quantity.tag.trim(), entry.modifiedAt, userId)
                        val previous = allTags[tagId]
                        if (previous == null || winningSide(rowVersion(previous), rowVersion(row)) > 0) allTags[tagId] = row
                        joins += EntryTagRow.from(entry, tagId, quantity.quantity, userId)
                    }
                    entry.photos.forEachIndexed { index, photo ->
                        if (photo.remotePath != null) {
                            syncedPhotos[photo.id] = PhotoRow.from(photo, trip.id, entry, index, userId)
                        }
                    }
                    val pending = entry.photos.filter { it.remotePath == null }
                    if (pending.isNotEmpty()) unsyncedPhotos[entry.id] = pending
                }
            }
            return LocalRows(
                tripRows,
                travelerRows,
                sightingRows,
                entryRows,
                allTags,
                joins,
                syncedPhotos,
                unsyncedPhotos,
            )
        }
    }
}

private fun rebuildTrips(
    trips: Collection<TripRow>,
    travelers: Collection<TravelerRow>,
    sightings: Collection<SightingRow>,
    entries: Collection<JournalEntryRow>,
    tags: Collection<TagRow>,
    entryTags: List<EntryTagRow>,
    photos: Collection<PhotoRow>,
    unsyncedLocalPhotos: Map<String, List<JournalPhoto>>,
    localPhotoPaths: Map<String, String>,
): List<Trip> {
    val tagsById = tags.associateBy(TagRow::id)
    val quantitiesByEntry = entryTags.groupBy(EntryTagRow::entryId).mapValues { (_, rows) ->
        rows.mapNotNull { row -> tagsById[row.tagId]?.let { TagQuantity(it.name, row.quantity) } }
    }
    val photosByEntry = photos.groupBy(PhotoRow::entryId).mapValues { (_, rows) ->
        rows.sortedBy(PhotoRow::sortOrder).map { it.toDomain(localPhotoPaths[it.id].orEmpty()) }
    }
    return trips.map { trip ->
        val journal = entries.filter { it.tripId == trip.id }.map { entry ->
            entry.toDomain(
                tags = quantitiesByEntry[entry.id].orEmpty(),
                photos = (photosByEntry[entry.id].orEmpty() + unsyncedLocalPhotos[entry.id].orEmpty())
                    .distinctBy(JournalPhoto::id)
                    .take(5),
            )
        }
        trip.toDomain(
            travelers = travelers.filter { it.tripId == trip.id }.map(TravelerRow::toDomain),
            sightings = sightings.filter { it.tripId == trip.id }.map(SightingRow::toDomain),
            journal = journal,
        )
    }
}

private fun parseEpoch(value: String): Long = Instant.parse(value).toEpochMilliseconds()
private fun iso(epochMillis: Long): String = Instant.fromEpochMilliseconds(epochMillis).toString()

private fun mutationId(id: String, modifiedAt: Long, payload: String): String =
    stableUuid("$id:$modifiedAt:$payload")

private fun stableUuid(value: String): String {
    fun hash(seed: Int): UInt {
        var result = seed
        value.forEach { result = result * 31 + it.code }
        return result.toUInt()
    }
    val hex = listOf(0x13579BDF, 0x2468ACE0, 0x10203040, 0x55667788)
        .joinToString("") { hash(it).toString(16).padStart(8, '0') }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
}

private inline fun <K, V> Iterable<K>.associateWithNotNull(transform: (K) -> V?): Map<K, V> =
    buildMap { for (key in this@associateWithNotNull) transform(key)?.let { put(key, it) } }

@Serializable
private data class TripRow(
    @SerialName("user_id") val userId: String,
    override val id: String,
    val name: String,
    val destination: String,
    val vehicle: String,
    val notes: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String?,
    @SerialName("start_latitude") val startLatitude: Double?,
    @SerialName("start_longitude") val startLongitude: Double?,
    @SerialName("start_place_label") val startPlaceLabel: String?,
    @SerialName("end_latitude") val endLatitude: Double?,
    @SerialName("end_longitude") val endLongitude: Double?,
    @SerialName("end_place_label") val endPlaceLabel: String?,
    @SerialName("modified_at") override val modifiedAt: String,
    @SerialName("mutation_id") override val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) : VersionedRow {
    fun toDomain(travelers: List<Traveler>, sightings: List<PlateSighting>, journal: List<JournalEntry>) = Trip(
        id = id,
        name = name,
        destination = destination,
        vehicle = vehicle,
        notes = notes,
        startedAt = parseEpoch(startedAt),
        endedAt = endedAt?.let(::parseEpoch),
        startLocation = LocationStamp(startLatitude, startLongitude, startPlaceLabel),
        endLocation = if (endLatitude != null || endLongitude != null || endPlaceLabel != null) {
            LocationStamp(endLatitude, endLongitude, endPlaceLabel)
        } else null,
        travelers = travelers,
        sightings = sightings,
        journal = journal,
        modifiedAt = parseEpoch(modifiedAt),
    )

    companion object {
        fun from(trip: Trip, userId: String) = TripRow(
            userId, trip.id, trip.name, trip.destination, trip.vehicle, trip.notes,
            iso(trip.startedAt), trip.endedAt?.let(::iso),
            trip.startLocation.latitude, trip.startLocation.longitude, trip.startLocation.placeName,
            trip.endLocation?.latitude, trip.endLocation?.longitude, trip.endLocation?.placeName,
            iso(trip.modifiedAt), stableUuid("trip:${trip.id}:${trip.modifiedAt}"),
        )
    }
}

@Serializable
private data class TravelerRow(
    @SerialName("user_id") val userId: String,
    override val id: String,
    @SerialName("trip_id") val tripId: String,
    val name: String,
    @SerialName("modified_at") override val modifiedAt: String,
    @SerialName("mutation_id") override val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) : VersionedRow {
    fun toDomain() = Traveler(id, name)
    companion object {
        fun from(value: Traveler, trip: Trip, userId: String): TravelerRow {
            val base = TravelerRow(userId, value.id, trip.id, value.name, iso(trip.modifiedAt), "")
            return base.copy(mutationId = mutationId(value.id, trip.modifiedAt, Json.encodeToString(base)))
        }
    }
}

@Serializable
private data class SightingRow(
    @SerialName("user_id") val userId: String,
    override val id: String,
    @SerialName("trip_id") val tripId: String,
    @SerialName("jurisdiction_code") val jurisdictionCode: String,
    @SerialName("first_seen_at") val firstSeenAt: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerialName("place_label") val placeLabel: String?,
    @SerialName("modified_at") override val modifiedAt: String,
    @SerialName("mutation_id") override val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) : VersionedRow {
    fun toDomain() = PlateSighting(
        id, jurisdictionCode, parseEpoch(firstSeenAt), LocationStamp(latitude, longitude, placeLabel), parseEpoch(modifiedAt)
    )
    companion object {
        fun from(value: PlateSighting, tripId: String, userId: String): SightingRow {
            val base = SightingRow(
                userId, value.id, tripId, value.jurisdictionCode, iso(value.firstSeenAt),
                value.location.latitude, value.location.longitude, value.location.placeName,
                iso(value.modifiedAt), "",
            )
            return base.copy(mutationId = mutationId(value.id, value.modifiedAt, Json.encodeToString(base)))
        }
    }
}

@Serializable
private data class JournalEntryRow(
    @SerialName("user_id") val userId: String,
    override val id: String,
    @SerialName("trip_id") val tripId: String,
    @SerialName("entry_type") val entryType: String,
    val title: String,
    val body: String,
    @SerialName("occurred_at") val occurredAt: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerialName("place_label") val placeLabel: String?,
    @SerialName("modified_at") override val modifiedAt: String,
    @SerialName("mutation_id") override val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) : VersionedRow {
    fun toDomain(tags: List<TagQuantity>, photos: List<JournalPhoto>) = JournalEntry(
        id = id,
        kind = if (entryType == "stop") JournalEntryKind.STOP else JournalEntryKind.NOTE,
        title = title,
        body = body,
        occurredAt = parseEpoch(occurredAt),
        location = LocationStamp(latitude, longitude, placeLabel),
        tags = tags,
        photos = photos,
        modifiedAt = parseEpoch(modifiedAt),
    )
    companion object {
        fun from(value: JournalEntry, tripId: String, userId: String): JournalEntryRow {
            val base = JournalEntryRow(
                userId, value.id, tripId, value.kind.name.lowercase(), value.title, value.body,
                iso(value.occurredAt), value.location.latitude, value.location.longitude,
                value.location.placeName, iso(value.modifiedAt), "",
            )
            return base.copy(mutationId = mutationId(value.id, value.modifiedAt, Json.encodeToString(base)))
        }
    }
}

@Serializable
private data class TagRow(
    @SerialName("user_id") val userId: String,
    override val id: String,
    val name: String,
    @SerialName("modified_at") override val modifiedAt: String,
    @SerialName("mutation_id") override val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) : VersionedRow {
    companion object {
        fun from(id: String, name: String, modifiedAt: Long, userId: String): TagRow {
            val base = TagRow(userId, id, name, iso(modifiedAt), "")
            return base.copy(mutationId = mutationId(id, modifiedAt, Json.encodeToString(base)))
        }
    }
}

@Serializable
private data class EntryTagRow(
    @SerialName("user_id") val userId: String,
    @SerialName("entry_id") val entryId: String,
    @SerialName("tag_id") val tagId: String,
    val quantity: Int,
    @SerialName("modified_at") val modifiedAt: String,
    @SerialName("mutation_id") val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) {
    companion object {
        fun from(entry: JournalEntry, tagId: String, quantity: Int, userId: String): EntryTagRow {
            val id = stableUuid("entry-tag:${entry.id}:$tagId")
            return EntryTagRow(userId, entry.id, tagId, quantity, iso(entry.modifiedAt), mutationId(id, entry.modifiedAt, "$quantity"))
        }
    }
}

@Serializable
private data class PhotoRow(
    @SerialName("user_id") val userId: String,
    override val id: String,
    @SerialName("trip_id") val tripId: String,
    @SerialName("entry_id") val entryId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("sort_order") val sortOrder: Int,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("modified_at") override val modifiedAt: String,
    @SerialName("mutation_id") override val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) : VersionedRow {
    fun toDomain(localPath: String) = JournalPhoto(id = id, localPath = localPath, remotePath = storagePath)
    companion object {
        fun from(photo: JournalPhoto, tripId: String, entry: JournalEntry, index: Int, userId: String): PhotoRow {
            val path = requireNotNull(photo.remotePath)
            val base = PhotoRow(userId, photo.id, tripId, entry.id, path, index, modifiedAt = iso(entry.modifiedAt), mutationId = "")
            return base.copy(mutationId = mutationId(photo.id, entry.modifiedAt, Json.encodeToString(base)))
        }
    }
}

@Serializable
private data class TombstoneRow(
    @SerialName("user_id") val userId: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("deleted_at") val deletedAt: String,
    @SerialName("mutation_id") val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) {
    fun toDomain() = SyncTombstone(entityType, entityId, parseEpoch(deletedAt), mutationId)
    companion object {
        fun from(value: SyncTombstone, userId: String) = TombstoneRow(
            userId, value.entityType, value.entityId, iso(value.deletedAt), value.mutationId,
        )
    }
}

@Serializable
private data class AchievementAwardRow(
    @SerialName("user_id") val userId: String,
    override val id: String,
    @SerialName("achievement_id") val achievementId: String,
    @SerialName("trip_id") val tripId: String?,
    @SerialName("earned_at") val earnedAt: String,
    @SerialName("modified_at") override val modifiedAt: String,
    @SerialName("mutation_id") override val mutationId: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
) : VersionedRow {
    companion object {
        fun from(
            achievementId: String,
            tripId: String?,
            userId: String,
            earnedAt: Long,
            modifiedAt: Long,
        ): AchievementAwardRow {
            val id = stableUuid("award:$achievementId:${tripId ?: "lifetime"}")
            return AchievementAwardRow(
                userId = userId,
                id = id,
                achievementId = achievementId,
                tripId = tripId,
                earnedAt = iso(earnedAt),
                modifiedAt = iso(modifiedAt),
                mutationId = mutationId(id, modifiedAt, achievementId),
            )
        }
    }
}
