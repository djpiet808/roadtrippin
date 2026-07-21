package com.roadtrippin.shared.cloud

import com.roadtrippin.shared.data.AccountRepository
import com.roadtrippin.shared.data.AccountSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import com.roadtrippin.shared.platform.PlatformServices

expect object CloudConfig {
    val supabaseUrl: String
    val supabasePublishableKey: String
}

object CloudServices {
    val configured: Boolean
        get() = CloudConfig.supabaseUrl.startsWith("https://") && CloudConfig.supabasePublishableKey.isNotBlank()

    val client: SupabaseClient? by lazy {
        if (!configured) null else createSupabaseClient(
            supabaseUrl = CloudConfig.supabaseUrl,
            supabaseKey = CloudConfig.supabasePublishableKey,
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
                explicitNulls = false
            })
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "roadtrippin"
                host = "auth-callback"
                sessionManager = RoadtrippinSessionManager()
            }
            install(Postgrest) { defaultSchema = "roadtrippin" }
            install(Storage)
        }
    }

    val accounts: SupabaseAccountRepository by lazy { SupabaseAccountRepository(client) }
    val sync: SupabaseSyncService by lazy { SupabaseSyncService(client) }
}

class SupabaseAccountRepository(
    private val client: SupabaseClient?,
) : AccountRepository {
    override fun observeSession(): Flow<AccountSession?> {
        val auth = client?.auth ?: return flowOf(null)
        return auth.sessionStatus.map { status ->
            (status as? SessionStatus.Authenticated)?.session?.user?.let { user ->
                AccountSession(user.id, user.email)
            }
        }
    }

    override suspend fun signIn(email: String, password: String) {
        requireClient().auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String) {
        requireClient().auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signInWithGoogle() {
        requireClient().auth.signInWith(Google)
    }

    suspend fun signInWithApple() {
        check(PlatformServices.supportsNativeAppleSignIn) { "Native Apple sign-in is available on iOS only." }
        val token = PlatformServices.requestAppleIdToken() ?: return
        requireClient().auth.signInWith(IDToken) {
            idToken = token
            provider = Apple
        }
    }

    override suspend fun signOut() {
        requireClient().auth.signOut()
    }

    override suspend fun deleteSharedIdentity() {
        error("Shared deletion remains disabled until the Edge Function passes isolated-project tests")
    }

    private fun requireClient(): SupabaseClient = checkNotNull(client) {
        "Supabase is not configured. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY to ignored local configuration."
    }
}
