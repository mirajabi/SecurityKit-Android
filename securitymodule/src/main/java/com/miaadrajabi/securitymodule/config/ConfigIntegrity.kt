package com.miaadrajabi.securitymodule.config

import android.content.Context
import com.miaadrajabi.securitymodule.crypto.CryptoUtils
import com.miaadrajabi.securitymodule.crypto.SecureHmacHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

/**
 * Configuration integrity verification utilities.
 * Provides HMAC and RSA signature verification for security configurations.
 */
object ConfigIntegrity {

    data class SignedConfig(
        val config: SecurityConfig,
        val signature: String,
        val algorithm: String = "HMAC-SHA256"
    )

    data class NetworkConfig(
        val configUrl: String,
        val signatureUrl: String? = null,
        val publicKeyPem: String? = null,
        val hmacKey: String? = null,
        val timeoutSeconds: Long = 30
    )

    /**
     * Verify HMAC signature of a configuration using secure Android Keystore key.
     */
    @JvmStatic
    suspend fun verifyHmacSignature(
        config: SecurityConfig,
        signature: String,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val configJson = Json.encodeToString(config)
            val hmacKey = if (context != null) {
                SecureHmacHelper.getOrCreateDeviceBoundHmacKey(context)
            } else {
                SecureHmacHelper.getOrCreateSecureHmacKey()
            }
            SecureHmacHelper.verifyHmacSignature(configJson.toByteArray(), signature, hmacKey)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verify HMAC signature of a configuration using legacy string key (for backward compatibility).
     * @deprecated Use verifyHmacSignature with Context parameter instead
     */
    @Deprecated("Use verifyHmacSignature with Context parameter for better security")
    @JvmStatic
    suspend fun verifyHmacSignature(
        config: SecurityConfig,
        signature: String,
        hmacKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val configJson = Json.encodeToString(config)
            val expectedSignature = CryptoUtils.hmacSha256(configJson.toByteArray(), hmacKey.toByteArray())
            CryptoUtils.constantTimeEquals(signature, expectedSignature)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verify HMAC signature over RAW JSON bytes using secure Android Keystore key.
     */
    @JvmStatic
    suspend fun verifyHmacSignatureRaw(
        rawJson: String,
        signature: String,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val hmacKey = if (context != null) {
                SecureHmacHelper.getOrCreateDeviceBoundHmacKey(context)
            } else {
                SecureHmacHelper.getOrCreateSecureHmacKey()
            }
            SecureHmacHelper.verifyHmacSignature(rawJson.toByteArray(), signature, hmacKey)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verify HMAC signature over RAW JSON bytes using legacy string key (for backward compatibility).
     * @deprecated Use verifyHmacSignatureRaw with Context parameter instead
     */
    @Deprecated("Use verifyHmacSignatureRaw with Context parameter for better security")
    @JvmStatic
    suspend fun verifyHmacSignatureRaw(
        rawJson: String,
        signature: String,
        hmacKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val expectedSignature = CryptoUtils.hmacSha256(rawJson.toByteArray(), hmacKey.toByteArray())
            CryptoUtils.constantTimeEquals(signature, expectedSignature)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verify RSA signature of a configuration.
     */
    @JvmStatic
    suspend fun verifyRsaSignature(
        config: SecurityConfig,
        signature: String,
        publicKeyPem: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val configJson = Json.encodeToString(config)
            val publicKey = parsePublicKeyFromPem(publicKeyPem)
            val signatureBytes = hexStringToByteArray(signature)
            
            val sig = java.security.Signature.getInstance("SHA256withRSA/PSS")
            sig.initVerify(publicKey)
            sig.update(configJson.toByteArray())
            sig.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Load and verify configuration from signed asset using secure Android Keystore HMAC.
     */
    @JvmStatic
    suspend fun loadSignedConfigFromAsset(
        context: Context,
        configAssetPath: String,
        signatureAssetPath: String,
        useSecureHmac: Boolean = true,
        publicKeyPem: String? = null
    ): SecurityConfig? = withContext(Dispatchers.IO) {
        try {
            val configJson = context.assets.open(configAssetPath).bufferedReader().use { it.readText() }
            val signature = context.assets.open(signatureAssetPath).bufferedReader().use { it.readText() }
            
            val config = Json.decodeFromString<SecurityConfig>(configJson)
            val isValid = when {
                useSecureHmac -> verifyHmacSignature(config, signature, context)
                publicKeyPem != null -> verifyRsaSignature(config, signature, publicKeyPem)
                else -> false
            }
            if (isValid) config else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load and verify configuration from signed asset using legacy HMAC key (for backward compatibility).
     * @deprecated Use loadSignedConfigFromAsset with useSecureHmac parameter instead
     */
    @Deprecated("Use loadSignedConfigFromAsset with useSecureHmac parameter for better security")
    @JvmStatic
    suspend fun loadSignedConfigFromAsset(
        context: Context,
        configAssetPath: String,
        signatureAssetPath: String,
        hmacKey: String? = null,
        publicKeyPem: String? = null
    ): SecurityConfig? = withContext(Dispatchers.IO) {
        try {
            val configJson = context.assets.open(configAssetPath).bufferedReader().use { it.readText() }
            val signature = context.assets.open(signatureAssetPath).bufferedReader().use { it.readText() }
            
            val config = Json.decodeFromString<SecurityConfig>(configJson)
            val isValid = when {
                hmacKey != null -> verifyHmacSignature(config, signature, hmacKey)
                publicKeyPem != null -> verifyRsaSignature(config, signature, publicKeyPem)
                else -> false
            }
            if (isValid) config else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load and verify configuration from signed asset using RAW JSON HMAC with secure Android Keystore.
     */
    @JvmStatic
    suspend fun loadSignedConfigFromAssetRaw(
        context: Context,
        configAssetPath: String,
        signatureAssetPath: String,
        useSecureHmac: Boolean = true
    ): SecurityConfig? = withContext(Dispatchers.IO) {
        try {
            val configJson = context.assets.open(configAssetPath).bufferedReader().use { it.readText() }
            val signature = context.assets.open(signatureAssetPath).bufferedReader().use { it.readText() }
            if (!verifyHmacSignatureRaw(configJson, signature, if (useSecureHmac) context else null)) return@withContext null
            Json.decodeFromString<SecurityConfig>(configJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load and verify configuration from signed asset using RAW JSON HMAC with legacy key (for backward compatibility).
     * @deprecated Use loadSignedConfigFromAssetRaw with useSecureHmac parameter instead
     */
    @Deprecated("Use loadSignedConfigFromAssetRaw with useSecureHmac parameter for better security")
    @JvmStatic
    suspend fun loadSignedConfigFromAssetRaw(
        context: Context,
        configAssetPath: String,
        signatureAssetPath: String,
        hmacKey: String
    ): SecurityConfig? = withContext(Dispatchers.IO) {
        try {
            val configJson = context.assets.open(configAssetPath).bufferedReader().use { it.readText() }
            val signature = context.assets.open(signatureAssetPath).bufferedReader().use { it.readText() }
            if (!verifyHmacSignatureRaw(configJson, signature, hmacKey)) return@withContext null
            Json.decodeFromString<SecurityConfig>(configJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load and verify configuration from network with signature verification.
     */
    @JvmStatic
    suspend fun loadSignedConfigFromNetwork(
        networkConfig: NetworkConfig
    ): SecurityConfig? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(networkConfig.timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(networkConfig.timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(networkConfig.timeoutSeconds, TimeUnit.SECONDS)
                .build()

            // Fetch configuration
            val configRequest = Request.Builder()
                .url(networkConfig.configUrl)
                .build()
            
            val configResponse = client.newCall(configRequest).execute()
            if (!configResponse.isSuccessful) return@withContext null
            
            val configJson = configResponse.body?.string() ?: return@withContext null
            val config = Json.decodeFromString<SecurityConfig>(configJson)

            // Fetch and verify signature
            val signature = when {
                networkConfig.signatureUrl != null -> {
                    val signatureRequest = Request.Builder()
                        .url(networkConfig.signatureUrl)
                        .build()
                    val signatureResponse = client.newCall(signatureRequest).execute()
                    if (!signatureResponse.isSuccessful) return@withContext null
                    signatureResponse.body?.string() ?: return@withContext null
                }
                else -> {
                    // Signature might be in a custom header or embedded in response
                    configResponse.header("X-Config-Signature") ?: return@withContext null
                }
            }

            val isValid = when {
                networkConfig.hmacKey != null -> verifyHmacSignature(config, signature, networkConfig.hmacKey)
                networkConfig.publicKeyPem != null -> verifyRsaSignature(config, signature, networkConfig.publicKeyPem)
                else -> false
            }

            if (isValid) config else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create HMAC signature for a configuration.
     */
    @JvmStatic
    suspend fun createHmacSignature(
        config: SecurityConfig,
        hmacKey: String
    ): String = withContext(Dispatchers.IO) {
        val configJson = Json.encodeToString(config)
        CryptoUtils.hmacSha256(configJson.toByteArray(), hmacKey.toByteArray())
    }

    /**
     * Parse RSA public key from PEM format.
     */
    private fun parsePublicKeyFromPem(publicKeyPem: String): PublicKey {
        val publicKeyDER = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        
        val keyBytes = android.util.Base64.decode(publicKeyDER, android.util.Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = java.security.KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * Convert hex string to byte array.
     */
    private fun hexStringToByteArray(hexString: String): ByteArray {
        val len = hexString.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4) + Character.digit(hexString[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * Enhanced SecurityConfigLoader with integrity verification support.
     */
    object SecureLoader {
        
        /**
         * Load configuration from asset with HMAC verification.
         */
        @JvmStatic
        suspend fun fromAssetWithHmac(
            context: Context,
            configAssetPath: String,
            signatureAssetPath: String,
            hmacKey: String
        ): SecurityConfig? {
            return loadSignedConfigFromAsset(context, configAssetPath, signatureAssetPath, hmacKey, null)
        }

        /**
         * Load configuration from asset with RAW JSON HMAC verification.
         */
        @JvmStatic
        suspend fun fromAssetWithHmacRaw(
            context: Context,
            configAssetPath: String,
            signatureAssetPath: String,
            hmacKey: String
        ): SecurityConfig? {
            return loadSignedConfigFromAssetRaw(context, configAssetPath, signatureAssetPath, hmacKey)
        }

        /**
         * Load configuration from asset with RSA signature verification.
         */
        @JvmStatic
        suspend fun fromAssetWithRsa(
            context: Context,
            configAssetPath: String,
            signatureAssetPath: String,
            publicKeyPem: String
        ): SecurityConfig? {
            return loadSignedConfigFromAsset(context, configAssetPath, signatureAssetPath, null, publicKeyPem)
        }

        /**
         * Load configuration from network with HMAC verification.
         */
        @JvmStatic
        suspend fun fromNetworkWithHmac(
            configUrl: String,
            signatureUrl: String?,
            hmacKey: String,
            timeoutSeconds: Long = 30
        ): SecurityConfig? {
            val networkConfig = NetworkConfig(
                configUrl = configUrl,
                signatureUrl = signatureUrl,
                hmacKey = hmacKey,
                timeoutSeconds = timeoutSeconds
            )
            return loadSignedConfigFromNetwork(networkConfig)
        }

        /**
         * Load configuration from network with RSA signature verification.
         */
        @JvmStatic
        suspend fun fromNetworkWithRsa(
            configUrl: String,
            signatureUrl: String?,
            publicKeyPem: String,
            timeoutSeconds: Long = 30
        ): SecurityConfig? {
            val networkConfig = NetworkConfig(
                configUrl = configUrl,
                signatureUrl = signatureUrl,
                publicKeyPem = publicKeyPem,
                timeoutSeconds = timeoutSeconds
            )
            return loadSignedConfigFromNetwork(networkConfig)
        }
    }
}
