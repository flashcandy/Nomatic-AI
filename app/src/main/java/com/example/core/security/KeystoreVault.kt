package com.example.core.security

import android.app.KeyguardManager
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface SecretVault {
    fun saveApiKey(providerId: String, plainKey: String)
    fun getApiKey(providerId: String): String?
    fun getMaskedKey(providerId: String): String?
    fun hasApiKey(providerId: String): Boolean
    fun deleteApiKey(providerId: String)
    fun clearAllKeys()
}

class AndroidKeystoreVault(private val context: Context) : SecretVault {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nomatic_ai_master_aes_gcm_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val VAULT_FILENAME = "nomatic_keystore_vault.enc"

        fun maskKey(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            if (trimmed.length <= 8) return "••••••••"
            val prefix = trimmed.take(3)
            val suffix = trimmed.takeLast(4)
            return "$prefix••••••••••••$suffix"
        }
    }

    private val vaultFile: File
        get() = File(context.filesDir, VAULT_FILENAME)

    private val fallbackSessionKey: SecretKey by lazy {
        val prefs = context.getSharedPreferences("nomatic_fallback_salt", Context.MODE_PRIVATE)
        val existing = prefs.getString("k", null)
        val bytes = if (existing != null) {
            Base64.decode(existing, Base64.NO_WRAP)
        } else {
            val generated = ByteArray(32).also { SecureRandom().nextBytes(it) }
            prefs.edit().putString("k", Base64.encodeToString(generated, Base64.NO_WRAP)).commit()
            generated
        }
        SecretKeySpec(bytes, "AES")
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existingEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existingEntry != null) {
                existingEntry.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Fallback for local JVM/Robolectric test environments without hardware KeyStore
            fallbackSessionKey
        }
    }

    @Synchronized
    override fun saveApiKey(providerId: String, plainKey: String) {
        val sanitized = plainKey.trim()
        if (sanitized.isEmpty()) {
            deleteApiKey(providerId)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val plainBytes = sanitized.toByteArray(StandardCharsets.UTF_8)
        val cipherBytes = cipher.doFinal(plainBytes)
        // Zero out sensitive buffer
        plainBytes.fill(0)

        val payload = JSONObject().apply {
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ct", Base64.encodeToString(cipherBytes, Base64.NO_WRAP))
            put("updatedAt", System.currentTimeMillis())
        }

        val root = readVaultJson()
        root.put(providerId.lowercase(), payload)
        vaultFile.writeText(root.toString(), StandardCharsets.UTF_8)
    }

    @Synchronized
    override fun getApiKey(providerId: String): String? {
        return try {
            val root = readVaultJson()
            val entry = root.optJSONObject(providerId.lowercase()) ?: return null
            val iv = Base64.decode(entry.getString("iv"), Base64.NO_WRAP)
            val cipherBytes = Base64.decode(entry.getString("ct"), Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            val result = String(decryptedBytes, StandardCharsets.UTF_8)
            decryptedBytes.fill(0)
            result.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    override fun getMaskedKey(providerId: String): String? {
        val raw = getApiKey(providerId) ?: return null
        return maskKey(raw)
    }

    override fun hasApiKey(providerId: String): Boolean {
        return !getApiKey(providerId).isNullOrBlank()
    }

    @Synchronized
    override fun deleteApiKey(providerId: String) {
        val root = readVaultJson()
        root.remove(providerId.lowercase())
        vaultFile.writeText(root.toString(), StandardCharsets.UTF_8)
    }

    @Synchronized
    override fun clearAllKeys() {
        if (vaultFile.exists()) {
            vaultFile.delete()
        }
    }

    private fun readVaultJson(): JSONObject {
        return try {
            if (!vaultFile.exists()) return JSONObject()
            val content = vaultFile.readText(StandardCharsets.UTF_8)
            if (content.isBlank()) JSONObject() else JSONObject(content)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }
}
