package com.miaadrajabi.securitymodule.config

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import java.io.InputStream

object SecurityConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    @JvmStatic fun fromJsonString(jsonString: String): SecurityConfig =
        json.decodeFromString(SecurityConfig.serializer(), jsonString)

    @JvmStatic fun fromAsset(context: Context, assetName: String = "security_config.json"): SecurityConfig {
        val stream: InputStream = context.assets.open(assetName)
        val bytes = stream.use { it.readBytes() }
        return fromJsonString(String(bytes, Charsets.UTF_8))
    }

    /**
     * Try to load a signed config first (HMAC raw), then fall back to plain asset.
     * HMAC key is supplied by caller (env var, remote, or BuildConfig).
     */
    @JvmStatic fun fromAssetPreferSigned(
        context: Context,
        assetName: String = "security_config.json",
        signatureAssetName: String = "security_config.sig",
        hmacKey: String?
    ): SecurityConfig {
        return try {
            if (!hmacKey.isNullOrEmpty()) {
                val signed = runBlocking {
                    ConfigIntegrity.SecureLoader
                        .fromAssetWithHmacRaw(context, assetName, signatureAssetName, hmacKey)
                }
                if (signed != null) return signed
            }
            fromAsset(context, assetName)
        } catch (t: Throwable) {
            fromAsset(context, assetName)
        }
    }
}


