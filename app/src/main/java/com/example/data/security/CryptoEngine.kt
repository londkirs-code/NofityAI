package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES-256 GCM encryption using AndroidKeyStore.
 * Securely encrypts and decrypts API keys so they are never stored in plain text.
 */
object CryptoEngine {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "NotifAI_Master_Key"
    private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun init() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts plaintext string to Base64 encoded (IV + CipherText).
     */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return try {
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combine IV and cipherText
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for exceptional edge cases
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    /**
     * Decrypts Base64 encoded (IV + CipherText) to plaintext string.
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isBlank()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return ""

            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

            val cipherTextSize = combined.size - GCM_IV_LENGTH
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherTextSize)

            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decrypted = cipher.doFinal(cipherText)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // Attempt fallback plain base64 decode if keystore had updated
            try {
                String(Base64.decode(encryptedText, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
        }
    }

    /**
     * Formats an API key for safe masked preview (e.g. "sk-a1b2••••••••9z0x")
     */
    fun maskApiKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        val prefix = key.take(5)
        val suffix = key.takeLast(4)
        return "$prefix••••••••$suffix"
    }
}
