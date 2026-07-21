package com.roadtrippin.shared.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual object SecureSessionStorage {
    private const val KEY_ALIAS = "roadtrippin_supabase_session"
    private const val PREFS = "roadtrippin_secure_session"
    private const val VALUE = "encrypted_session"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual fun save(value: String) {
        val appContext = checkNotNull(context)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            updateAAD(appContext.packageName.encodeToByteArray())
        }
        val encrypted = cipher.doFinal(value.encodeToByteArray())
        val payload = cipher.iv + encrypted
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(VALUE, Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
    }

    actual fun load(): String? {
        val appContext = context ?: return null
        val encoded = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(VALUE, null) ?: return null
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            require(payload.size > 12)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
                updateAAD(appContext.packageName.encodeToByteArray())
            }
            cipher.doFinal(payload.copyOfRange(12, payload.size)).decodeToString()
        }.onFailure { delete() }.getOrNull()
    }

    actual fun delete() {
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.remove(VALUE)?.apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }
}
