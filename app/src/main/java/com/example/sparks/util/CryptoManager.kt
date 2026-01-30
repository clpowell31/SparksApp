package com.example.sparks.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "SparksIdentityKey"

    // 1. RSA SETTINGS (For User Identity)
    private const val RSA_ALGORITHM = "RSA"
    private const val RSA_BLOCK_MODE = "ECB"
    private const val RSA_PADDING = "PKCS1Padding"
    private const val RSA_TRANSFORMATION = "$RSA_ALGORITHM/$RSA_BLOCK_MODE/$RSA_PADDING"

    // 2. AES SETTINGS (For Message Content)
    private const val AES_ALGORITHM = "AES"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding" // <--- This is the correct name
    private const val AES_KEY_SIZE = 256

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    // --- INITIALIZATION ---

    fun checkOrGenerateKeys(): String? {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKeyPair()
        }
        return getMyPublicKeyString()
    }

    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()

        keyPairGenerator.initialize(spec)
        keyPairGenerator.generateKeyPair()
    }

    // --- PUBLIC KEY SHARING ---

    fun getMyPublicKeyString(): String? {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
        val publicKey = entry?.certificate?.publicKey ?: return null
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun parsePublicKey(base64PublicKey: String): PublicKey {
        val bytes = Base64.decode(base64PublicKey, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    // --- ENCRYPTION FLOW ---

    fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGen.init(AES_KEY_SIZE)
        return keyGen.generateKey()
    }

    fun encryptMessageWithAes(message: String, aesKey: SecretKey): String {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))

        // Combine IV and CipherText
        val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        return "$ivString:$cipherString"
    }

    fun wrapAesKeyForRecipient(aesKey: SecretKey, recipientPublicKey: PublicKey): String {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
        val encryptedKeyBytes = cipher.doFinal(aesKey.encoded)
        return Base64.encodeToString(encryptedKeyBytes, Base64.NO_WRAP)
    }

    // --- DECRYPTION FLOW ---

    // ** FIX: Removed 'private' so ChatViewModel can access it **
    fun unwrapAesKey(wrappedKeyBase64: String): SecretKey? {
        return try {
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as? java.security.PrivateKey ?: return null

            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)

            val encryptedKeyBytes = Base64.decode(wrappedKeyBase64, Base64.NO_WRAP)
            val decodedKey = cipher.doFinal(encryptedKeyBytes)

            SecretKeySpec(decodedKey, AES_ALGORITHM)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decryptMessage(encryptedContent: String, wrappedAesKey: String): String {
        try {
            val aesKey = unwrapAesKey(wrappedAesKey) ?: return "Error: No Key"

            val parts = encryptedContent.split(":")
            if (parts.size != 2) return "Error: Format"

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec)

            val plainBytes = cipher.doFinal(cipherBytes)
            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "🔒 Decryption Failed"
        }
    }

    // --- MEDIA ENCRYPTION (BYTE ARRAYS) ---

    // ** FIX: Updated TRANSFORMATION to AES_TRANSFORMATION **
    fun encryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    // ** FIX: Updated TRANSFORMATION to AES_TRANSFORMATION **
    fun decryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encrypted)
    }
}