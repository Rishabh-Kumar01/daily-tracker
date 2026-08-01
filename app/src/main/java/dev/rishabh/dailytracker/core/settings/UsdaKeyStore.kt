package dev.rishabh.dailytracker.core.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val Context.secureSettings by preferencesDataStore(name = "secure_settings")

/**
 * Encrypted-at-rest store for the USDA FoodData Central API key.
 *
 * The key is the user's own secret, never a shipped constant — the spec is explicit that it
 * lives in encrypted storage and never in source. The ciphertext sits in a DataStore, but
 * the bytes are sealed with an AES/GCM key held in the AndroidKeyStore, so the value on disk
 * is unreadable without the hardware-backed key. Nothing here logs or returns the key except
 * to the caller that is about to make the request.
 *
 * OxygenOS never sees a plaintext key: [setApiKey] encrypts before the write, [getApiKey]
 * decrypts after the read.
 */
@OptIn(ExperimentalEncodingApi::class)
@Singleton
class UsdaKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Emits whether a key is currently stored, for gating the "Search USDA" affordance. */
    val hasKey: Flow<Boolean> = context.secureSettings.data.map { it[CIPHERTEXT] != null }

    /** The decrypted key, or null if none is stored (or the ciphertext can no longer be read). */
    suspend fun getApiKey(): String? {
        val stored = context.secureSettings.data.first()[CIPHERTEXT] ?: return null
        return try {
            decrypt(stored)
        } catch (e: Exception) {
            // A key sealed under a Keystore entry that was since invalidated (e.g. the alias
            // was cleared) is unrecoverable; report absent rather than crash, so the UI can
            // re-prompt for it.
            null
        }
    }

    /** Encrypts and persists [key]; a blank value clears any stored key. */
    suspend fun setApiKey(key: String) {
        val trimmed = key.trim()
        context.secureSettings.edit { prefs ->
            if (trimmed.isEmpty()) prefs.remove(CIPHERTEXT) else prefs[CIPHERTEXT] = encrypt(trimmed)
        }
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val bytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        // IV is not secret; it is stored alongside the ciphertext, length-prefixed.
        return Base64.encode(iv) + SEPARATOR + Base64.encode(bytes)
    }

    private fun decrypt(stored: String): String {
        val (ivPart, cipherPart) = stored.split(SEPARATOR, limit = 2)
        val iv = Base64.decode(ivPart)
        val bytes = Base64.decode(cipherPart)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(bytes), Charsets.UTF_8)
    }

    /** Fetches the AES key from the AndroidKeyStore, generating it once on first use. */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        val CIPHERTEXT = stringPreferencesKey("usda_api_key")
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "usda_api_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val SEPARATOR = ":"
    }
}
