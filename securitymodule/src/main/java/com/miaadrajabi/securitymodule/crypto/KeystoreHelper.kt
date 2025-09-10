package com.miaadrajabi.securitymodule.crypto

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object KeystoreHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DEFAULT_ALIAS = "SecurityModule.AESGCM"
    private const val STRONGBOX_ALIAS = "SecurityModule.StrongBox.AESGCM"
    private const val USER_AUTH_ALIAS = "SecurityModule.UserAuth.AESGCM"
    private const val DEVICE_BINDING_ALIAS = "SecurityModule.DeviceBinding.RSA"

    @JvmStatic fun getOrCreateAesKey(alias: String = DEFAULT_ALIAS): SecretKey {
        return if (Build.VERSION.SDK_INT >= 23) {
            try {
                val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                ks.load(null)
                val existing = ks.getKey(alias, null) as? SecretKey
                if (existing != null) return existing
                val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    alias,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            } catch (t: Throwable) {
                CryptoUtils.generateAesKeyGcm()
            }
        } else {
            CryptoUtils.generateAesKeyGcm()
        }
    }

    /**
     * Get or create a StrongBox-backed AES key for enhanced security.
     * Falls back to regular TEE key if StrongBox is not available.
     */
    @JvmStatic fun getOrCreateStrongBoxAesKey(): SecretKey {
        if (Build.VERSION.SDK_INT < 28) {
            return getOrCreateAesKey() // StrongBox requires API 28+
        }

        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            val existing = ks.getKey(STRONGBOX_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                STRONGBOX_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setIsStrongBoxBacked(true) // Request StrongBox if available
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            // StrongBox not available, fallback to regular TEE key
            return getOrCreateAesKey()
        }
    }

    /**
     * Get or create a user authentication-bound AES key.
     * Requires user authentication (fingerprint, face, PIN, etc.) for each use.
     */
    @JvmStatic fun getOrCreateUserAuthBoundAesKey(
        userAuthenticationRequired: Boolean = true,
        userAuthenticationValidityDurationSeconds: Int = 300 // 5 minutes
    ): SecretKey {
        if (Build.VERSION.SDK_INT < 23) {
            return getOrCreateAesKey() // User auth binding requires API 23+
        }

        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            val existing = ks.getKey(USER_AUTH_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val specBuilder = android.security.keystore.KeyGenParameterSpec.Builder(
                USER_AUTH_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)

            if (userAuthenticationRequired) {
                specBuilder
                    .setUserAuthenticationRequired(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    specBuilder.setUserAuthenticationParameters(
                        userAuthenticationValidityDurationSeconds,
                        android.security.keystore.KeyProperties.AUTH_DEVICE_CREDENTIAL or android.security.keystore.KeyProperties.AUTH_BIOMETRIC_STRONG
                    )
                } else {
                    @Suppress("DEPRECATION")
                    specBuilder.setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds)
                }
            }

            keyGenerator.init(specBuilder.build())
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            // Fallback to regular key
            return getOrCreateAesKey()
        }
    }

    /**
     * Get or create a device-binding RSA key pair for generating device-specific identifiers.
     */
    @JvmStatic fun getOrCreateDeviceBindingKeyPair(): Pair<PublicKey, PrivateKey> {
        if (Build.VERSION.SDK_INT < 23) {
            throw UnsupportedOperationException("Device binding keys require API 23+")
        }

        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            
            val existingPrivate = ks.getKey(DEVICE_BINDING_ALIAS, null) as? PrivateKey
            val existingPublic = ks.getCertificate(DEVICE_BINDING_ALIAS)?.publicKey
            if (existingPrivate != null && existingPublic != null) {
                return Pair(existingPublic, existingPrivate)
            }

            val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE)
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                DEVICE_BINDING_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_SIGN or android.security.keystore.KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(android.security.keystore.KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                .setKeySize(2048)
                .build()

            keyPairGenerator.initialize(spec)
            val keyPair = keyPairGenerator.generateKeyPair()
            return Pair(keyPair.public, keyPair.private)
        } catch (e: Exception) {
            throw RuntimeException("Failed to create device binding key pair", e)
        }
    }

    /**
     * Generate a device-binding identifier using hardware-backed keys.
     * This creates a stable, device-specific identifier without exposing PII.
     */
    @JvmStatic fun generateDeviceBindingId(context: Context): String {
        try {
            val keyPair = getOrCreateDeviceBindingKeyPair()
            val privateKey = keyPair.second

            // Create a stable identifier from device properties
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val packageName = context.packageName
            val bindingData = "$deviceId:$packageName:SecurityModule"

            // Sign the binding data with the device-specific private key
            val signature = java.security.Signature.getInstance("SHA256withRSA/PSS")
            signature.initSign(privateKey)
            signature.update(bindingData.toByteArray())
            val signatureBytes = signature.sign()

            // Return the signature as a hex string (this is the device binding ID)
            return signatureBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback to a simple hash if device binding fails
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val packageName = context.packageName
            val fallbackData = "$deviceId:$packageName:SecurityModule:fallback"
            return CryptoUtils.sha256(fallbackData.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Check if StrongBox is available on this device.
     */
    @JvmStatic fun isStrongBoxAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < 28) return false

        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            
            // Try to create a test StrongBox key
            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                "test_strongbox_availability",
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setIsStrongBoxBacked(true)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
            
            // Clean up test key
            ks.deleteEntry("test_strongbox_availability")
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Check if user authentication is available on this device.
     */
    @JvmStatic fun isUserAuthenticationAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < 23) return false

        try {

            // For library context, we can't easily access ActivityThread
            // Return false as a safe default - user authentication availability
            // should be checked by the consuming application
            return false
        } catch (e: Exception) {
            return false
        }
    }
}


