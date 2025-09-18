package com.miaadrajabi.securitymodule.examples

import android.content.Context
import com.miaadrajabi.securitymodule.config.ConfigIntegrity
import com.miaadrajabi.securitymodule.config.SecurityConfig
import com.miaadrajabi.securitymodule.config.SecurityConfigLoader
import com.miaadrajabi.securitymodule.crypto.SecureHmacHelper
import kotlinx.coroutines.runBlocking

/**
 * Example usage of secure HMAC with Android Keystore.
 * This demonstrates how to use the new secure HMAC implementation.
 */
object SecureHmacExample {

    /**
     * Example 1: Load configuration with secure HMAC (recommended for production)
     */
    fun loadConfigWithSecureHmac(context: Context): SecurityConfig? {
        return try {
            // This will automatically use Android Keystore for HMAC verification
            SecurityConfigLoader.fromAssetPreferSigned(
                context = context,
                assetName = "security_config.json",
                signatureAssetName = "security_config.sig",
                useSecureHmac = true // Use secure Android Keystore HMAC
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Example 2: Manual HMAC verification with secure key
     */
    fun verifyConfigManually(context: Context, configJson: String, signature: String): Boolean {
        return runBlocking {
            try {
                val config = SecurityConfigLoader.fromJsonString(configJson)
                ConfigIntegrity.verifyHmacSignature(
                    config = config,
                    signature = signature,
                    context = context // This will use device-bound HMAC key
                )
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Example 3: Generate HMAC signature for testing (requires server-side implementation)
     */
    fun generateHmacForTesting(context: Context, data: String): String? {
        return try {
            val hmacKey = SecureHmacHelper.getOrCreateDeviceBoundHmacKey(context)
            SecureHmacHelper.computeHmacSha256(data.toByteArray(), hmacKey)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Example 4: Check StrongBox availability
     */
    fun checkStrongBoxAvailability(): Boolean {
        return SecureHmacHelper.isStrongBoxAvailableForHmac()
    }

    /**
     * Example 5: Migration from legacy HMAC to secure HMAC
     */
    fun migrateToSecureHmac(context: Context): Boolean {
        return try {
            // Clear old keys if needed
            SecureHmacHelper.clearAllHmacKeys()
            
            // Test secure HMAC
            val testData = "test data for migration"
            val signature = generateHmacForTesting(context, testData)
            
            if (signature != null) {
                val hmacKey = SecureHmacHelper.getOrCreateDeviceBoundHmacKey(context)
                SecureHmacHelper.verifyHmacSignature(testData.toByteArray(), signature, hmacKey)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Example 6: Production-ready configuration loading with fallback
     */
    fun loadProductionConfig(context: Context): SecurityConfig? {
        return try {
            // Try secure HMAC first
            val secureConfig = loadConfigWithSecureHmac(context)
            if (secureConfig != null) {
                return secureConfig
            }

            // Fallback to unsigned config (for development)
            SecurityConfigLoader.fromAsset(context, "security_config.json")
        } catch (e: Exception) {
            // Final fallback to default config
            SecurityConfig()
        }
    }
}
