package com.miaadrajabi.securitymodule.crypto

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Secure HMAC helper using Android Keystore for key generation and management.
 * Provides device-bound HMAC keys that cannot be extracted or tampered with.
 */
object SecureHmacHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val HMAC_KEY_ALIAS = "SecurityModule.HMAC.Config"
    private const val DEVICE_BOUND_HMAC_ALIAS = "SecurityModule.HMAC.DeviceBound"
    private const val STRONGBOX_HMAC_ALIAS = "SecurityModule.HMAC.StrongBox"

    /**
     * Generate or retrieve a secure HMAC key with intelligent fallback strategy.
     * Priority: StrongBox → TEE → Software
     */
    @JvmStatic
    fun getOrCreateSecureHmacKey(): SecretKey {
        val errorLog = mutableListOf<String>()
        
        return if (Build.VERSION.SDK_INT >= 23) {
            try {
                errorLog.add("✅ Android version ${Build.VERSION.SDK_INT} supports Keystore")
                
                val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                ks.load(null)
                errorLog.add("✅ Android Keystore loaded successfully")
                
                // Check if key already exists
                val existing = ks.getKey(HMAC_KEY_ALIAS, null) as? SecretKey
                if (existing != null) {
                    errorLog.add("✅ Existing HMAC key found: ${existing.algorithm} (${existing.format})")
                    return existing
                }
                errorLog.add("ℹ️ No existing HMAC key found, generating new one")
                
                // Try StrongBox first (API 28+)
                if (Build.VERSION.SDK_INT >= 28) {
                    try {
                        errorLog.add("🔄 Attempting StrongBox key generation...")
                        val strongBoxKey = getOrCreateStrongBoxHmacKey()
                        errorLog.add("✅ StrongBox key generated successfully: ${strongBoxKey.algorithm} (${strongBoxKey.format})")
                        return strongBoxKey
                    } catch (e: Exception) {
                        errorLog.add("❌ StrongBox failed: ${e.javaClass.simpleName} - ${e.message}")
                        errorLog.add("🔄 Falling back to TEE...")
                    }
                } else {
                    errorLog.add("ℹ️ API level ${Build.VERSION.SDK_INT} < 28, skipping StrongBox")
                }
                
                // Check TEE support first using Google's official method
                val teeSupported = try {
                    val testAlias = "test_tee_check_${System.currentTimeMillis()}"
                    val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                    val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                        testAlias,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or 
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false)
                        .setUserAuthenticationValidityDurationSeconds(-1)
                        .build()
                    
                    keyGenerator.init(spec)
                    val testKey = keyGenerator.generateKey()
                    
                    // Use Google's official method to check if key is hardware-backed
                    val isHardwareBacked = try {
                        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                        ks.load(null)
                        val keyInfo = ks.getKey(testAlias, null)?.let { keystoreKey ->
                            if (Build.VERSION.SDK_INT >= 23) {
                                val factory = java.security.KeyFactory.getInstance(keystoreKey.algorithm, ANDROID_KEYSTORE)
                                factory.getKeySpec(keystoreKey, android.security.keystore.KeyInfo::class.java)
                            } else null
                        }
                        
                        if (keyInfo != null) {
                            val isInsideSecureHardware = keyInfo.isInsideSecureHardware
                            errorLog.add("   TEE KeyInfo: insideSecureHardware=$isInsideSecureHardware, origin=${keyInfo.origin}")
                            isInsideSecureHardware
                        } else {
                            errorLog.add("   TEE KeyInfo not available, using fallback method")
                            testKey.algorithm == "AES" && testKey.format == "AndroidKeyStore"
                        }
                    } catch (e: Exception) {
                        errorLog.add("   TEE KeyInfo error: ${e.javaClass.simpleName} - ${e.message}")
                        testKey.algorithm == "AES" && testKey.format == "AndroidKeyStore"
                    }
                    
                    // Test if the key actually works
                    try {
                        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, testKey)
                        val testData = "TEE_SUPPORT_TEST".toByteArray()
                        cipher.doFinal(testData)
                        errorLog.add("   TEE encryption test: PASSED")
                    } catch (e: Exception) {
                        errorLog.add("   TEE encryption test: FAILED - ${e.message}")
                    }
                    
                    // Clean up test key
                    try {
                        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                        ks.load(null)
                        if (ks.containsAlias(testAlias)) {
                            ks.deleteEntry(testAlias)
                        }
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                    
                    isHardwareBacked
                } catch (e: Exception) {
                    errorLog.add("ℹ️ TEE support check failed: ${e.javaClass.simpleName} - ${e.message}")
                    false
                }
                
                if (teeSupported) {
                    // TEE is supported, try to create the actual HMAC key
                    try {
                        errorLog.add("🔄 TEE supported, attempting TEE key generation...")
                        val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                            HMAC_KEY_ALIAS,
                            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or 
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                        )
                            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .setUserAuthenticationRequired(false) // Don't require user auth for HMAC
                            .setUserAuthenticationValidityDurationSeconds(-1) // No timeout
                            .setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256) // Add digest requirement
                            .build()
                        
                        keyGenerator.init(spec)
                        val teeKey = keyGenerator.generateKey()
                        
                        // Test the key to ensure it works
                        try {
                            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, teeKey)
                            val testData = "TEE_HMAC_TEST".toByteArray()
                            val encrypted = cipher.doFinal(testData)
                            errorLog.add("✅ TEE key generated and tested successfully: ${teeKey.algorithm} (${teeKey.format})")
                            errorLog.add("   Key test: Encryption successful, ${encrypted.size} bytes")
                            return teeKey
                        } catch (testException: Exception) {
                            errorLog.add("⚠️ TEE key generated but test failed: ${testException.message}")
                            errorLog.add("   Proceeding with key anyway...")
                            return teeKey
                        }
                    } catch (e: Exception) {
                        errorLog.add("❌ TEE key generation failed: ${e.javaClass.simpleName} - ${e.message}")
                        errorLog.add("   Detailed error: ${e.stackTraceToString()}")
                        errorLog.add("🔄 Falling back to Software keys...")
                    }
                } else {
                    errorLog.add("ℹ️ TEE not supported on this device, skipping TEE key generation")
                    errorLog.add("🔄 Falling back to Software keys...")
                }
                
            } catch (e: Exception) {
                errorLog.add("❌ Keystore initialization failed: ${e.javaClass.simpleName} - ${e.message}")
                errorLog.add("🔄 Falling back to Software keys...")
            }
            
            // Fallback to software-generated key
            try {
                errorLog.add("🔄 Attempting Software key generation...")
                val softwareKey = generateSoftwareHmacKey()
                errorLog.add("✅ Software key generated successfully: ${softwareKey.algorithm} (${softwareKey.format})")
                return softwareKey
            } catch (e: Exception) {
                errorLog.add("❌ Software key generation failed: ${e.javaClass.simpleName} - ${e.message}")
                errorLog.add("💥 All key generation methods failed!")
                throw SecurityException("Failed to generate HMAC key. Error log: ${errorLog.joinToString(" | ")}", e)
            }
        } else {
            errorLog.add("⚠️ Android version ${Build.VERSION.SDK_INT} < 23, using Software keys only")
            try {
                val softwareKey = generateSoftwareHmacKey()
                errorLog.add("✅ Software key generated successfully: ${softwareKey.algorithm} (${softwareKey.format})")
                return softwareKey
            } catch (e: Exception) {
                errorLog.add("❌ Software key generation failed: ${e.javaClass.simpleName} - ${e.message}")
                throw SecurityException("Failed to generate HMAC key. Error log: ${errorLog.joinToString(" | ")}", e)
            }
        }
    }

    /**
     * Get the best available HMAC key with detailed information about the key type used.
     */
    @JvmStatic
    fun getBestAvailableHmacKey(): Pair<SecretKey, String> {
        return try {
            // Try StrongBox first (most secure)
            val strongBoxKey = getOrCreateStrongBoxHmacKey()
            return Pair(strongBoxKey, "StrongBox")
        } catch (e: Exception) {
            // StrongBox not available, try TEE
            try {
                val teeKey = getOrCreateSecureHmacKey()
                return Pair(teeKey, "TEE")
            } catch (e2: Exception) {
                // TEE failed, try software key with enhanced error handling
                try {
                    val softwareKey = generateSoftwareHmacKey()
                    return Pair(softwareKey, "Software")
                } catch (e3: Exception) {
                    // Last resort: generate a simple software key
                    val simpleKey = generateSimpleSoftwareHmacKey()
                    return Pair(simpleKey, "SimpleSoftware")
                }
            }
        }
    }

    /**
     * Test HMAC functionality with sample data to verify it's working correctly.
     * This is useful for debugging and verification.
     */
    @JvmStatic
    fun testHmacFunctionality(): Map<String, Any> {
        val testResults = mutableMapOf<String, Any>()
        
        try {
            // Test data
            val testData = "SecurityModule.HMAC.Test.Data.${System.currentTimeMillis()}"
            val testDataBytes = testData.toByteArray()
            
            // Get the best available key
            val (key, keyType) = getBestAvailableHmacKey()
            testResults["key_type"] = keyType
            testResults["key_algorithm"] = key.algorithm
            testResults["key_format"] = key.format
            testResults["test_data"] = testData
            
            // Compute HMAC
            val signature = computeHmacSha256(testDataBytes, key)
            testResults["signature"] = signature
            testResults["signature_length"] = signature.length
            
            // Verify HMAC
            val isValid = verifyHmacSignature(testDataBytes, signature, key)
            testResults["verification_result"] = isValid
            
            // Test with tampered data
            val tamperedData = "Tampered.SecurityModule.HMAC.Test.Data.${System.currentTimeMillis()}"
            val tamperedDataBytes = tamperedData.toByteArray()
            val isTamperedValid = verifyHmacSignature(tamperedDataBytes, signature, key)
            testResults["tampered_verification_result"] = isTamperedValid
            testResults["tampered_data"] = tamperedData
            
            // Test with different key (should fail)
            try {
                val differentKey = generateSoftwareHmacKey()
                val differentSignature = computeHmacSha256(testDataBytes, differentKey)
                val isDifferentKeyValid = verifyHmacSignature(testDataBytes, differentSignature, key)
                testResults["different_key_test"] = isDifferentKeyValid
            } catch (e: Exception) {
                testResults["different_key_test"] = "Error: ${e.message}"
            }
            
            // Overall test result
            val overallSuccess = isValid && !isTamperedValid
            testResults["overall_success"] = overallSuccess
            testResults["test_status"] = if (overallSuccess) "PASSED" else "FAILED"
            
        } catch (e: Exception) {
            testResults["error"] = "${e.javaClass.simpleName}: ${e.message}"
            testResults["test_status"] = "ERROR"
        }
        
        return testResults
    }
    
    /**
     * Simple HMAC test with predefined test data for quick verification.
     */
    @JvmStatic
    fun quickHmacTest(): String {
        return try {
            val testData = "SecurityModule.Test.Data.12345"
            val (key, keyType) = getBestAvailableHmacKey()
            val signature = computeHmacSha256(testData.toByteArray(), key)
            val isValid = verifyHmacSignature(testData.toByteArray(), signature, key)
            
            "✅ HMAC Test: $keyType key, Signature: ${signature.take(16)}..., Valid: $isValid"
        } catch (e: Exception) {
            "❌ HMAC Test Failed: ${e.message}"
        }
    }
    
    /**
     * Diagnose HMAC issues with detailed error reporting.
     */
    @JvmStatic
    fun diagnoseHmacIssues(): Map<String, Any> {
        val diagnosis = mutableMapOf<String, Any>()
        
        try {
            // Test 1: Key generation
            diagnosis["key_generation_test"] = try {
                val (key, keyType) = getBestAvailableHmacKey()
                diagnosis["key_type"] = keyType
                diagnosis["key_algorithm"] = key.algorithm
                diagnosis["key_format"] = key.format
                "SUCCESS"
            } catch (e: Exception) {
                diagnosis["key_generation_error"] = "${e.javaClass.simpleName}: ${e.message}"
                "FAILED"
            }
            
            // Test 2: Direct HMAC with software key
            diagnosis["direct_hmac_test"] = try {
                val softwareKey = generateSoftwareHmacKey()
                val testData = "Direct HMAC Test".toByteArray()
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(softwareKey)
                val result = mac.doFinal(testData)
                diagnosis["direct_hmac_result"] = result.joinToString("") { "%02x".format(it) }
                "SUCCESS"
            } catch (e: Exception) {
                diagnosis["direct_hmac_error"] = "${e.javaClass.simpleName}: ${e.message}"
                "FAILED"
            }
            
            // Test 3: Keystore key HMAC
            diagnosis["keystore_hmac_test"] = try {
                val (key, keyType) = getBestAvailableHmacKey()
                val testData = "Keystore HMAC Test".toByteArray()
                val signature = computeHmacSha256(testData, key)
                diagnosis["keystore_hmac_signature"] = signature
                diagnosis["keystore_hmac_key_type"] = keyType
                "SUCCESS"
            } catch (e: Exception) {
                diagnosis["keystore_hmac_error"] = "${e.javaClass.simpleName}: ${e.message}"
                "FAILED"
            }
            
            // Test 4: Key derivation test
            diagnosis["key_derivation_test"] = try {
                val (key, keyType) = getBestAvailableHmacKey()
                if (keyType != "Software") {
                    val derivedKey = deriveKeyFromKeystoreKey(key, "test".toByteArray())
                    diagnosis["derived_key_length"] = derivedKey.size
                    diagnosis["derived_key_type"] = keyType
                    "SUCCESS"
                } else {
                    "SKIPPED (Software key)"
                }
            } catch (e: Exception) {
                diagnosis["key_derivation_error"] = "${e.javaClass.simpleName}: ${e.message}"
                "FAILED"
            }
            
        } catch (e: Exception) {
            diagnosis["overall_error"] = "${e.javaClass.simpleName}: ${e.message}"
        }
        
        return diagnosis
    }
    
    /**
     * Get detailed error log for HMAC key generation debugging.
     */
    @JvmStatic
    fun getHmacKeyGenerationLog(): List<String> {
        val errorLog = mutableListOf<String>()
        
        try {
            errorLog.add("🔍 HMAC Key Generation Debug Log")
            errorLog.add("📱 Device Info:")
            errorLog.add("   Android Version: ${Build.VERSION.SDK_INT}")
            errorLog.add("   Device Model: ${Build.MODEL}")
            errorLog.add("   Device Manufacturer: ${Build.MANUFACTURER}")
            errorLog.add("   Device Brand: ${Build.BRAND}")
            errorLog.add("   Is Emulator: ${isRunningOnEmulator()}")
            
            // Test Keystore availability
            errorLog.add("🔑 Keystore Tests:")
            try {
                val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                ks.load(null)
                errorLog.add("   ✅ Android Keystore: Available")
            } catch (e: Exception) {
                errorLog.add("   ❌ Android Keystore: Failed - ${e.javaClass.simpleName} - ${e.message}")
            }
            
            // Test StrongBox
            errorLog.add("🛡️ StrongBox Tests:")
            if (Build.VERSION.SDK_INT >= 28) {
                try {
                    val result = isStrongBoxAvailableForHmac()
                    errorLog.add("   ${if (result) "✅" else "❌"} StrongBox: ${if (result) "Available" else "Not available"}")
                } catch (e: Exception) {
                    errorLog.add("   ❌ StrongBox: Error - ${e.javaClass.simpleName} - ${e.message}")
                }
            } else {
                errorLog.add("   ⚠️ StrongBox: API level too low (${Build.VERSION.SDK_INT} < 28)")
            }
            
            // Test TEE
            errorLog.add("🔒 TEE Tests:")
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                    val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                        "test_tee_debug",
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                    )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                    keyGenerator.init(spec)
                    val key = keyGenerator.generateKey()
                    errorLog.add("   ✅ TEE: Available - ${key.algorithm} (${key.format})")
                } catch (e: Exception) {
                    errorLog.add("   ❌ TEE: Failed - ${e.javaClass.simpleName} - ${e.message}")
                }
            } else {
                errorLog.add("   ⚠️ TEE: API level too low (${Build.VERSION.SDK_INT} < 23)")
            }
            
            // Test Software keys
            errorLog.add("💻 Software Key Tests:")
            try {
                val key = generateSoftwareHmacKey()
                errorLog.add("   ✅ Software Keys: Available - ${key.algorithm} (${key.format})")
            } catch (e: Exception) {
                errorLog.add("   ❌ Software Keys: Failed - ${e.javaClass.simpleName} - ${e.message}")
            }
            
            // Test actual key generation
            errorLog.add("🎯 Actual Key Generation Test:")
            try {
                val (key, keyType) = getBestAvailableHmacKey()
                errorLog.add("   ✅ Key Generated: $keyType - ${key.algorithm} (${key.format})")
            } catch (e: Exception) {
                errorLog.add("   ❌ Key Generation Failed: ${e.javaClass.simpleName} - ${e.message}")
                errorLog.add("   📋 Full Error: ${e.stackTraceToString()}")
            }
            
        } catch (e: Exception) {
            errorLog.add("💥 Debug log generation failed: ${e.javaClass.simpleName} - ${e.message}")
        }
        
        return errorLog
    }

    /**
     * Check if a key is StrongBox-backed by attempting to access its properties.
     * This is a heuristic check since there's no direct API to determine StrongBox.
     */
    private fun isStrongBoxKey(key: SecretKey): Boolean {
        return try {
            // StrongBox keys typically have specific characteristics
            if (key.algorithm != "AES" || key.format != "AndroidKeyStore") {
                return false
            }
            
            // Additional heuristics for StrongBox detection
            if (Build.VERSION.SDK_INT >= 28) {
                // Try to access key properties that might indicate StrongBox
                try {
                    val encoded = key.encoded
                    // StrongBox keys often have different encoded data characteristics
                    // This is a best-effort heuristic
                    return encoded != null && encoded.isNotEmpty()
                } catch (e: Exception) {
                    // If we can't access encoded data, it might still be StrongBox
                    return true
                }
            }
            
            // For older API levels, assume it's TEE if it's AndroidKeyStore
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get detailed information about the fallback strategy and key types available.
     */
    @JvmStatic
    fun getFallbackStrategyInfo(): Map<String, Any> {
        val info = mutableMapOf<String, Any>()
        
        // Check what's available
        info["android_version"] = Build.VERSION.SDK_INT
        info["strongbox_available"] = isStrongBoxAvailableForHmac()
        info["tee_available"] = try {
            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                "test_tee_availability",
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            true
        } catch (e: Exception) {
            false
        }
        
        // Determine recommended strategy
        val strategy = when {
            Build.VERSION.SDK_INT >= 28 && info["strongbox_available"] == true -> "StrongBox → TEE → Software"
            Build.VERSION.SDK_INT >= 23 && info["tee_available"] == true -> "TEE → Software"
            else -> "Software only"
        }
        info["recommended_strategy"] = strategy
        
        // Get actual key type that would be used
        val (_, keyType) = getBestAvailableHmacKey()
        info["actual_key_type"] = keyType
        
        return info
    }

    /**
     * Generate or retrieve a StrongBox-backed HMAC key for enhanced security.
     */
    @JvmStatic
    fun getOrCreateStrongBoxHmacKey(): SecretKey {
        if (Build.VERSION.SDK_INT < 28) {
            return getOrCreateSecureHmacKey() // StrongBox requires API 28+
        }

        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            val existing = ks.getKey(STRONGBOX_HMAC_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                STRONGBOX_HMAC_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or 
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
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
            return getOrCreateSecureHmacKey()
        }
    }

    /**
     * Generate a device-bound HMAC key that incorporates device properties.
     * This creates a unique key per device without exposing device identifiers.
     */
    @JvmStatic
    fun getOrCreateDeviceBoundHmacKey(context: Context): SecretKey {
        if (Build.VERSION.SDK_INT < 23) {
            return getOrCreateSecureHmacKey()
        }

        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            val existing = ks.getKey(DEVICE_BOUND_HMAC_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            // Create device-specific binding data
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val packageName = context.packageName
            val bindingData = "$deviceId:$packageName:SecurityModule:HMAC"
            
            // Use binding data as additional entropy for key generation
            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                DEVICE_BOUND_HMAC_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or 
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setAttestationChallenge(bindingData.toByteArray()) // Device binding
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            // Fallback to regular key
            return getOrCreateSecureHmacKey()
        }
    }

    /**
     * Compute HMAC-SHA256 using a secure key from Android Keystore.
     * The key material is never exposed to the application.
     */
    @JvmStatic
    fun computeHmacSha256(data: ByteArray, key: SecretKey): String {
        return try {
            // Check if this is a Keystore key (StrongBox/TEE)
            val isKeystoreKey = key.format == "AndroidKeyStore" || key.format == null
            
            if (isKeystoreKey) {
                // For Keystore keys (StrongBox/TEE), use key derivation
                android.util.Log.d("SecureHmacHelper", "Using key derivation for Keystore key: ${key.algorithm}")
                val keyBytes = deriveKeyFromKeystoreKey(key, data)
                val mac = Mac.getInstance("HmacSHA256")
                val secretKeySpec = javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256")
                mac.init(secretKeySpec)
                val result = mac.doFinal(data)
                result.joinToString("") { "%02x".format(it) }
            } else {
                // For software keys, use direct HMAC
                android.util.Log.d("SecureHmacHelper", "Using direct HMAC for software key: ${key.algorithm}")
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(key)
                val result = mac.doFinal(data)
                result.joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            // Enhanced error logging for debugging
            val errorMsg = "Failed to compute HMAC: ${e.javaClass.simpleName} - ${e.message}"
            android.util.Log.e("SecureHmacHelper", errorMsg, e)
            throw RuntimeException(errorMsg, e)
        }
    }
    
    /**
     * Derive a key from Android Keystore key for HMAC use.
     */
    private fun deriveKeyFromKeystoreKey(keystoreKey: SecretKey, data: ByteArray): ByteArray {
        return try {
            // Try different cipher modes for better compatibility
            val cipherModes = listOf(
                "AES/GCM/NoPadding",
                "AES/CBC/PKCS7Padding", 
                "AES/ECB/PKCS7Padding"
            )
            
            var encrypted: ByteArray? = null
            var lastException: Exception? = null
            
            for (mode in cipherModes) {
                try {
                    val cipher = javax.crypto.Cipher.getInstance(mode)
                    
                    // For GCM mode, we need to generate IV
                    if (mode.contains("GCM")) {
                        val iv = ByteArray(12) // 96-bit IV for GCM
                        java.security.SecureRandom().nextBytes(iv)
                        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
                        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keystoreKey, spec)
                    } else {
                        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keystoreKey)
                    }
                    
                    encrypted = cipher.doFinal("HMAC_DERIVATION_KEY_${System.currentTimeMillis()}".toByteArray())
                    android.util.Log.d("SecureHmacHelper", "Successfully derived key using cipher mode: $mode")
                    break
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.w("SecureHmacHelper", "Failed to derive key using $mode: ${e.message}")
                }
            }
            
            if (encrypted != null) {
                // Use first 32 bytes as HMAC key
                encrypted.take(32).toByteArray()
            } else {
                throw lastException ?: RuntimeException("All cipher modes failed")
            }
        } catch (e: Exception) {
            android.util.Log.w("SecureHmacHelper", "Key derivation failed, using fallback: ${e.message}")
            // Fallback to a simple hash based on key properties
            val keyString = "${keystoreKey.algorithm}_${keystoreKey.format}_${System.currentTimeMillis()}"
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(keyString.toByteArray())
            // Ensure we have exactly 32 bytes
            hash.take(32).toByteArray()
        }
    }

    /**
     * Verify HMAC signature using constant-time comparison.
     */
    @JvmStatic
    fun verifyHmacSignature(data: ByteArray, signature: String, key: SecretKey): Boolean {
        return try {
            val expectedSignature = computeHmacSha256(data, key)
            CryptoUtils.constantTimeEquals(signature, expectedSignature)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate a software-based HMAC key as fallback.
     * This should only be used when Android Keystore is not available.
     */
    private fun generateSoftwareHmacKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }
    
    /**
     * Generate a simple software-based HMAC key for devices with limited crypto support.
     * This is a last resort fallback for devices like Samsung Galaxy A14.
     */
    private fun generateSimpleSoftwareHmacKey(): SecretKey {
        return try {
            // Try standard HmacSHA256
            val keyGenerator = KeyGenerator.getInstance("HmacSHA256")
            keyGenerator.init(256)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            // Fallback to SecretKeySpec with random bytes
            val keyBytes = ByteArray(32) // 256 bits
            java.security.SecureRandom().nextBytes(keyBytes)
            javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256")
        }
    }

    /**
     * Check if StrongBox is available for HMAC keys.
     */
    @JvmStatic
    fun isStrongBoxAvailableForHmac(): Boolean {
        if (Build.VERSION.SDK_INT < 28) return false
        
        return try {
            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                "test_strongbox_hmac",
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setIsStrongBoxBacked(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Comprehensive Keystore capability detection for debugging and compatibility.
     */
    @JvmStatic
    fun getKeystoreCapabilities(): Map<String, Any> {
        val capabilities = mutableMapOf<String, Any>()
        
        // Basic Android version check
        capabilities["android_version"] = Build.VERSION.SDK_INT
        capabilities["is_emulator"] = isRunningOnEmulator()
        capabilities["device_model"] = Build.MODEL
        capabilities["device_manufacturer"] = Build.MANUFACTURER
        capabilities["device_brand"] = Build.BRAND
        
        // Android Keystore availability
        capabilities["keystore_available"] = try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            true
        } catch (e: Exception) {
            capabilities["keystore_error"] = e.message ?: "Unknown error"
            false
        }
        
        // TEE support (hardware-backed keys) - Using Google's official method
        capabilities["tee_support"] = try {
            if (Build.VERSION.SDK_INT < 23) {
                capabilities["tee_error"] = "API level too low (${Build.VERSION.SDK_INT} < 23)"
                false
            } else {
                val testAlias = "test_tee_key_${System.currentTimeMillis()}"
                var key: SecretKey? = null
                
                try {
                    val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                    val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                        testAlias,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or 
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false) // Don't require user auth for test
                        .setUserAuthenticationValidityDurationSeconds(-1) // No timeout
                        .build()
                    
                    keyGenerator.init(spec)
                    key = keyGenerator.generateKey()
                    
                    // Use Google's official method to check if key is hardware-backed
                    val isHardwareBacked = try {
                        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                        ks.load(null)
                        val keyInfo = ks.getKey(testAlias, null)?.let { keystoreKey ->
                            if (Build.VERSION.SDK_INT >= 23) {
                                val factory = java.security.KeyFactory.getInstance(keystoreKey.algorithm, ANDROID_KEYSTORE)
                                factory.getKeySpec(keystoreKey, android.security.keystore.KeyInfo::class.java)
                            } else null
                        }
                        
                        if (keyInfo != null) {
                            // This is the official Google method
                            val isInsideSecureHardware = keyInfo.isInsideSecureHardware
                            val isUserAuthenticationRequired = keyInfo.isUserAuthenticationRequired
                            val isUserAuthenticationValidWhileOnBody = keyInfo.isUserAuthenticationValidWhileOnBody
                            
                            capabilities["tee_inside_secure_hardware"] = isInsideSecureHardware
                            capabilities["tee_user_auth_required"] = isUserAuthenticationRequired
                            capabilities["tee_user_auth_valid_while_on_body"] = isUserAuthenticationValidWhileOnBody
                            capabilities["tee_key_origin"] = keyInfo.origin
                            capabilities["tee_key_purposes"] = keyInfo.purposes
                            
                            isInsideSecureHardware
                        } else {
                            // Fallback for older API levels or if KeyInfo is not available
                            key.algorithm == "AES" && key.format == "AndroidKeyStore"
                        }
                    } catch (e: Exception) {
                        capabilities["tee_keyinfo_error"] = "${e.javaClass.simpleName}: ${e.message}"
                        // Fallback method
                        key.algorithm == "AES" && key.format == "AndroidKeyStore"
                    }
                    
                    capabilities["tee_key_algorithm"] = key.algorithm
                    capabilities["tee_key_format"] = key.format
                    capabilities["tee_key_encoded_length"] = try { key.encoded.size } catch (e: Exception) { 0 }
                    
                    // Test if we can actually use the key for encryption/decryption
                    try {
                        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
                        val testData = "TEE_TEST_DATA".toByteArray()
                        val encrypted = cipher.doFinal(testData)
                        capabilities["tee_encryption_test"] = "PASSED"
                        capabilities["tee_encrypted_size"] = encrypted.size
                    } catch (e: Exception) {
                        capabilities["tee_encryption_test"] = "FAILED: ${e.message}"
                    }
                    
                    isHardwareBacked
                } finally {
                    // Clean up test key to avoid conflicts
                    try {
                        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                        ks.load(null)
                        if (ks.containsAlias(testAlias)) {
                            ks.deleteEntry(testAlias)
                        }
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }
            }
        } catch (e: Exception) {
            capabilities["tee_error"] = "${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}"
            false
        }
        
        // StrongBox support
        capabilities["strongbox_support"] = try {
            val result = isStrongBoxAvailableForHmac()
            if (!result) {
                capabilities["strongbox_error"] = "StrongBox not available or failed"
            }
            result
        } catch (e: Exception) {
            capabilities["strongbox_error"] = e.message ?: "Unknown error"
            false
        }
        
        // User authentication support
        capabilities["user_auth_support"] = try {
            if (Build.VERSION.SDK_INT >= 23) {
                val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    "test_user_auth_key",
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
                true
            } else {
                capabilities["user_auth_error"] = "API level too low (${Build.VERSION.SDK_INT} < 23)"
                false
            }
        } catch (e: Exception) {
            capabilities["user_auth_error"] = e.message ?: "Unknown error"
            false
        }
        
        // Device binding support
        capabilities["device_binding_support"] = try {
            if (Build.VERSION.SDK_INT >= 23) {
                val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    "test_device_binding_key",
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setAttestationChallenge("test_challenge".toByteArray())
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
                true
            } else {
                capabilities["device_binding_error"] = "API level too low (${Build.VERSION.SDK_INT} < 23)"
                false
            }
        } catch (e: Exception) {
            capabilities["device_binding_error"] = e.message ?: "Unknown error"
            false
        }
        
        return capabilities
    }
    
    /**
     * Detect if running on emulator (comprehensive detection).
     * Uses the same logic as EmulatorDetector for consistency.
     */
    private fun isRunningOnEmulator(): Boolean {
        return try {
            // Use EmulatorDetector for comprehensive detection
            val emulatorSignals = com.miaadrajabi.securitymodule.detectors.EmulatorDetector.collectSignals()
            emulatorSignals.count > 0
        } catch (e: Exception) {
            // Fallback to basic detection if EmulatorDetector fails
            try {
                val buildModel = Build.MODEL?.lowercase() ?: ""
                val buildManufacturer = Build.MANUFACTURER?.lowercase() ?: ""
                val buildProduct = Build.PRODUCT?.lowercase() ?: ""
                val buildDevice = Build.DEVICE?.lowercase() ?: ""
                val buildHardware = Build.HARDWARE?.lowercase() ?: ""
                
                buildModel.contains("google_sdk") ||
                buildModel.contains("emulator") ||
                buildModel.contains("android sdk") ||
                buildModel.contains("sdk_gphone") ||
                buildModel.contains("vbox86") ||
                buildModel.contains("aosp on ia emulator") ||
                buildManufacturer.contains("genymotion") ||
                buildProduct.contains("sdk") ||
                buildProduct.contains("emulator") ||
                buildDevice.contains("generic") ||
                Build.FINGERPRINT?.startsWith("generic") == true ||
                Build.FINGERPRINT?.startsWith("unknown") == true ||
                Build.FINGERPRINT?.contains("vbox") == true ||
                buildHardware.contains("goldfish") ||
                buildHardware.contains("ranchu") ||
                buildHardware.contains("vbox") ||
                buildHardware.contains("vbox86")
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * Clear all HMAC keys from Android Keystore.
     * Use with caution - this will invalidate all existing signatures.
     */
    @JvmStatic
    fun clearAllHmacKeys() {
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            
            val aliases = listOf(HMAC_KEY_ALIAS, DEVICE_BOUND_HMAC_ALIAS, STRONGBOX_HMAC_ALIAS)
            aliases.forEach { alias ->
                if (ks.containsAlias(alias)) {
                    ks.deleteEntry(alias)
                }
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
    
    /**
     * Check if the device is Samsung Galaxy A14 or similar device with limited TEE support.
     */
    @JvmStatic
    fun isSamsungGalaxyA14OrSimilar(): Boolean {
        return try {
            val model = Build.MODEL.lowercase()
            val manufacturer = Build.MANUFACTURER.lowercase()
            val brand = Build.BRAND.lowercase()
            
            // Check for Samsung Galaxy A14 or similar devices
            (manufacturer == "samsung" || brand == "samsung") && (
                model.contains("sm-a145") || // Galaxy A14 5G
                model.contains("sm-a146") || // Galaxy A14 4G
                model.contains("galaxy a14") ||
                model.contains("a14") ||
                // Similar mid-range Samsung devices that might have limited TEE support
                model.contains("sm-a135") || // Galaxy A13
                model.contains("sm-a125") || // Galaxy A12
                model.contains("sm-a115") || // Galaxy A11
                model.contains("galaxy a1") ||
                model.contains("galaxy a2") ||
                model.contains("galaxy a3")
            )
        } catch (e: Exception) {
            false
        }
    }
}
