package com.roadtrippin.shared.platform

import com.roadtrippin.shared.domain.CheerStyle
import com.roadtrippin.shared.domain.LocationStamp
import com.roadtrippin.shared.domain.JournalPhoto
import kotlinx.coroutines.flow.Flow

expect object PlatformServices {
    fun nowEpochMillis(): Long
    fun randomId(): String
    fun formatDateTime(epochMillis: Long): String
    suspend fun currentLocation(): LocationStamp?
    suspend fun reverseGeocode(location: LocationStamp): String?
    suspend fun pickJournalPhotos(limit: Int): List<JournalPhoto>
    suspend fun takeJournalPhoto(): JournalPhoto?
    suspend fun readLocalFile(path: String): ByteArray?
    suspend fun saveJournalPhoto(id: String, bytes: ByteArray): String?
    fun observeConnectivity(): Flow<Boolean>
    val supportsNativeAppleSignIn: Boolean
    suspend fun requestAppleIdToken(): String?
    fun celebrate(sound: Boolean, haptics: Boolean, cheerStyle: CheerStyle)
    fun shareText(title: String, text: String)
    fun shareMapImage(title: String, svg: String, summary: String)
}
