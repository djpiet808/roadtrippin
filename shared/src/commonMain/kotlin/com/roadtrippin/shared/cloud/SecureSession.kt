package com.roadtrippin.shared.cloud

import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

expect object SecureSessionStorage {
    fun save(value: String)
    fun load(): String?
    fun delete()
}

class RoadtrippinSessionManager : SessionManager {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun saveSession(session: UserSession) {
        SecureSessionStorage.save(json.encodeToString(session))
    }

    override suspend fun loadSession(): UserSession =
        checkNotNull(loadSessionOrNull()) { "No saved Roadtrippin session" }

    override suspend fun loadSessionOrNull(): UserSession? = SecureSessionStorage.load()?.let { encoded ->
        runCatching { json.decodeFromString<UserSession>(encoded) }
            .onFailure { SecureSessionStorage.delete() }
            .getOrNull()
    }

    override suspend fun deleteSession() {
        SecureSessionStorage.delete()
    }
}
