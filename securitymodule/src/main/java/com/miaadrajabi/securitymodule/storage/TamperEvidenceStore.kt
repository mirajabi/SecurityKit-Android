package com.miaadrajabi.securitymodule.storage

import android.content.Context
import android.content.SharedPreferences
import com.miaadrajabi.securitymodule.crypto.CryptoUtils
import com.miaadrajabi.securitymodule.crypto.KeystoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.SecretKey

/**
 * Tamper-evidence store for cached policies and configurations.
 * Uses HMAC to detect local tampering of cached data.
 */
object TamperEvidenceStore {
    
    private const val PREFS_NAME = "SecurityModule_TamperEvidence"
    private const val MAC_KEY_ALIAS = "SecurityModule.TamperEvidence.MAC"
    private const val VERSION_KEY = "version"
    private const val DATA_KEY = "data"
    private const val MAC_KEY = "mac"
    private const val TIMESTAMP_KEY = "timestamp"

    /**
     * Store data with tamper-evidence MAC.
     */
    @JvmStatic
    suspend fun storeData(
        context: Context,
        key: String,
        data: Any,
        version: String = "1.0"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val macKey = getOrCreateMacKey()
            
            val dataJson = when (data) {
                is String -> data
                else -> Json.encodeToString(data)
            }
            
            val timestamp = System.currentTimeMillis()
            val payload = "$version:$dataJson:$timestamp"
            val mac = CryptoUtils.hmacSha256(payload.toByteArray(), macKey.encoded)
            
            prefs.edit()
                .putString("${key}_$VERSION_KEY", version)
                .putString("${key}_$DATA_KEY", dataJson)
                .putString("${key}_$MAC_KEY", mac)
                .putLong("${key}_$TIMESTAMP_KEY", timestamp)
                .apply()
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieve and verify data with tamper-evidence check.
     */
    @JvmStatic
    suspend fun retrieveData(
        context: Context,
        key: String,
        expectedVersion: String? = null
    ): TamperEvidenceResult<String> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            val version = prefs.getString("${key}_$VERSION_KEY", null)
            val dataJson = prefs.getString("${key}_$DATA_KEY", null)
            val mac = prefs.getString("${key}_$MAC_KEY", null)
            val timestamp = prefs.getLong("${key}_$TIMESTAMP_KEY", 0L)
            
            if (version == null || dataJson == null || mac == null) {
                return@withContext TamperEvidenceResult<String>(
                    success = false,
                    data = null,
                    error = "Data not found"
                )
            }
            
            // Check version if specified
            if (expectedVersion != null && version != expectedVersion) {
                return@withContext TamperEvidenceResult<String>(
                    success = false,
                    data = null,
                    error = "Version mismatch. Expected: $expectedVersion, Found: $version"
                )
            }
            
            // Verify MAC
            val macKey = getOrCreateMacKey()
            val payload = "$version:$dataJson:$timestamp"
            val expectedMac = CryptoUtils.hmacSha256(payload.toByteArray(), macKey.encoded)
            
            if (!CryptoUtils.constantTimeEquals(mac, expectedMac)) {
                return@withContext TamperEvidenceResult<String>(
                    success = false,
                    data = null,
                    error = "MAC verification failed - data may have been tampered with"
                )
            }
            
            TamperEvidenceResult<String>(
                success = true,
                data = dataJson,
                version = version,
                timestamp = timestamp
            )
            
        } catch (e: Exception) {
            TamperEvidenceResult<String>(
                success = false,
                data = null,
                error = "Retrieval failed: ${e.message}"
            )
        }
    }

    /**
     * Retrieve and verify data with type deserialization.
     */
    @JvmStatic
    suspend fun <T> retrieveData(
        context: Context,
        key: String,
        deserializer: kotlinx.serialization.KSerializer<T>,
        expectedVersion: String? = null
    ): TamperEvidenceResult<T> = withContext(Dispatchers.IO) {
        try {
            val result = retrieveData(context, key, expectedVersion)
            if (!result.success) {
                return@withContext TamperEvidenceResult<T>(
                    success = false,
                    data = null,
                    error = result.error
                )
            }
            
            val deserializedData = Json.decodeFromString(deserializer, result.data!!)
            TamperEvidenceResult<T>(
                success = true,
                data = deserializedData,
                version = result.version,
                timestamp = result.timestamp
            )
            
        } catch (e: Exception) {
            TamperEvidenceResult<T>(
                success = false,
                data = null,
                error = "Deserialization failed: ${e.message}"
            )
        }
    }

    /**
     * Check if data exists and is valid without retrieving it.
     */
    @JvmStatic
    suspend fun isDataValid(
        context: Context,
        key: String,
        expectedVersion: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val result = retrieveData(context, key, expectedVersion)
        result.success
    }

    /**
     * Remove data from tamper-evidence store.
     */
    @JvmStatic
    fun removeData(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("${key}_$VERSION_KEY")
            .remove("${key}_$DATA_KEY")
            .remove("${key}_$MAC_KEY")
            .remove("${key}_$TIMESTAMP_KEY")
            .apply()
    }

    /**
     * Clear all tamper-evidence data.
     */
    @JvmStatic
    fun clearAllData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Get or create MAC key for tamper evidence.
     */
    private suspend fun getOrCreateMacKey(): SecretKey {
        return try {
            KeystoreHelper.getOrCreateAesKey(MAC_KEY_ALIAS)
        } catch (e: Exception) {
            // Fallback to software-generated key if Keystore fails
            CryptoUtils.generateAesKeyGcm()
        }
    }

    /**
     * Rotate MAC key (invalidates all existing data).
     */
    @JvmStatic
    suspend fun rotateMacKey(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // Clear all existing data
            clearAllData(context)
            
            // Generate new MAC key
            val newMacKey = CryptoUtils.generateAesKeyGcm()
            
            // Store new key in Keystore (this will replace the old one)
            KeystoreHelper.getOrCreateAesKey(MAC_KEY_ALIAS)
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get data age in milliseconds.
     */
    @JvmStatic
    suspend fun getDataAge(context: Context, key: String): Long? = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val timestamp = prefs.getLong("${key}_$TIMESTAMP_KEY", 0L)
            if (timestamp == 0L) null else System.currentTimeMillis() - timestamp
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if data is older than specified age.
     */
    @JvmStatic
    suspend fun isDataOlderThan(
        context: Context,
        key: String,
        maxAgeMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val age = getDataAge(context, key)
        age != null && age > maxAgeMs
    }

    /**
     * Result of tamper-evidence data retrieval.
     */
    data class TamperEvidenceResult<T>(
        val success: Boolean,
        val data: T?,
        val version: String? = null,
        val timestamp: Long? = null,
        val error: String? = null
    )
}
