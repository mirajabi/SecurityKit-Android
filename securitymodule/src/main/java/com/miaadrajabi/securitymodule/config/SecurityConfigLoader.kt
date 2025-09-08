package com.miaadrajabi.securitymodule.config

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.InputStream

object SecurityConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromJsonString(jsonString: String): SecurityConfig =
        json.decodeFromString(SecurityConfig.serializer(), jsonString)

    fun fromAsset(context: Context, assetName: String = "security_config.json"): SecurityConfig {
        val stream: InputStream = context.assets.open(assetName)
        val bytes = stream.use { it.readBytes() }
        return fromJsonString(String(bytes, Charsets.UTF_8))
    }
}


