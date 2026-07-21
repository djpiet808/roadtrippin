package com.roadtrippin.shared.data

import com.roadtrippin.shared.domain.Achievement
import com.roadtrippin.shared.domain.JournalEntry
import com.roadtrippin.shared.domain.JournalPhoto
import com.roadtrippin.shared.domain.PlateSighting
import com.roadtrippin.shared.domain.SyncState
import com.roadtrippin.shared.domain.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun observeTrips(): Flow<List<Trip>>
    suspend fun upsert(trip: Trip)
    suspend fun delete(tripId: String)
}

interface SightingRepository {
    fun observeSightings(tripId: String): Flow<List<PlateSighting>>
    suspend fun upsert(tripId: String, sighting: PlateSighting)
    suspend fun delete(tripId: String, sightingId: String)
}

interface JournalRepository {
    fun observeEntries(tripId: String): Flow<List<JournalEntry>>
    suspend fun upsert(tripId: String, entry: JournalEntry)
    suspend fun delete(tripId: String, entryId: String)
    suspend fun attachPhotos(tripId: String, entryId: String, photos: List<JournalPhoto>)
}

interface AchievementRepository {
    fun observeTripAwards(tripId: String): Flow<List<Achievement>>
    fun observeLifetimeAwards(): Flow<List<Achievement>>
    suspend fun recalculate()
}

data class AccountSession(
    val userId: String,
    val email: String?,
)

interface AccountRepository {
    fun observeSession(): Flow<AccountSession?>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun signOut()
    suspend fun deleteSharedIdentity()
}

interface SyncRepository {
    fun observeState(): Flow<SyncState>
    suspend fun enqueueFullUpload()
    suspend fun syncNow()
}
