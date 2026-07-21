package com.roadtrippin.shared.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Country(val displayName: String) {
    UNITED_STATES("United States"),
    CANADA("Canada"),
    MEXICO("Mexico"),
}

@Serializable
enum class Region(val displayName: String, val colorHex: Long) {
    PACIFIC_NORTHWEST("Pacific Northwest", 0xFF4E79A7),
    PACIFIC("Pacific", 0xFF76B7B2),
    MOUNTAIN_WEST("Mountain West", 0xFF59A14F),
    SOUTHWEST("Southwest", 0xFFF28E2B),
    GREAT_PLAINS("Great Plains", 0xFFEDC948),
    MIDWEST("Midwest", 0xFFB07AA1),
    NEW_ENGLAND("New England", 0xFFE15759),
    MID_ATLANTIC("Mid-Atlantic", 0xFF9C755F),
    SOUTHEAST("Southeast", 0xFFFF9DA7),
    NON_US("International", 0xFF2A9D8F),
}

@Serializable
data class Jurisdiction(
    val code: String,
    val name: String,
    val country: Country,
    val region: Region,
)

@Serializable
data class LocationStamp(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

@Serializable
data class PlateSighting(
    val id: String,
    val jurisdictionCode: String,
    val firstSeenAt: Long,
    val location: LocationStamp,
    val modifiedAt: Long,
)

@Serializable
enum class JournalEntryKind { NOTE, STOP }

@Serializable
data class TagQuantity(
    val tag: String,
    val quantity: Int,
)

@Serializable
data class JournalPhoto(
    val id: String,
    val localPath: String,
    val remotePath: String? = null,
)

@Serializable
data class JournalEntry(
    val id: String,
    val kind: JournalEntryKind,
    val title: String,
    val body: String,
    val occurredAt: Long,
    val location: LocationStamp,
    val tags: List<TagQuantity> = emptyList(),
    val photos: List<JournalPhoto> = emptyList(),
    val modifiedAt: Long,
)

@Serializable
data class Traveler(val id: String, val name: String)

@Serializable
data class Trip(
    val id: String,
    val name: String = "",
    val destination: String = "",
    val vehicle: String = "",
    val notes: String = "",
    val startedAt: Long,
    val endedAt: Long? = null,
    val startLocation: LocationStamp = LocationStamp(),
    val endLocation: LocationStamp? = null,
    val travelers: List<Traveler> = emptyList(),
    val sightings: List<PlateSighting> = emptyList(),
    val journal: List<JournalEntry> = emptyList(),
    val modifiedAt: Long,
) {
    val isActive: Boolean get() = endedAt == null
    val displayName: String get() = name.ifBlank { "Road trip" }
}

@Serializable
enum class AchievementScope { TRIP, LIFETIME }

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val scope: AchievementScope,
    val earned: Boolean,
    val progress: Int,
    val target: Int,
)

@Serializable
enum class SyncState { LOCAL_ONLY, QUEUED, SYNCING, SYNCED, ERROR }

@Serializable
data class AppSettings(
    val safetyAccepted: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val forceDarkMode: Boolean? = null,
)

