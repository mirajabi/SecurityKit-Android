package com.miaadrajabi.securitymodule.crypto

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * APK HMAC Protection System
 * 
 * This class provides comprehensive protection against APK repackaging by:
 * 1. Generating HMAC signatures for APK files
 * 2. Verifying APK integrity at runtime
 * 3. Detecting signature tampering
 * 4. Using secure hardware-backed keys when available
 */
object ApkHmacProtector {
    
    private const val TAG = "ApkHmacProtector"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val APK_HMAC_KEY_ALIAS = "apk_hmac_protection_key"
    
    /**
     * Data class representing APK integrity information
     */
    data class ApkIntegrityInfo(
        val packageName: String,
        val versionCode: Long,
        val versionName: String,
        val signatureHash: String,
        val apkHash: String,
        val hmacSignature: String,
        val keyType: String,
        val timestamp: Long,
        val isIntegrityValid: Boolean
    )
    
    /**
     * Generate HMAC signature for APK file
     * This should be called during build process or by a secure server
     */
    @JvmStatic
    fun generateApkHmacSignature(
        apkFilePath: String,
        context: Context? = null
    ): String? {
        return try {
            Log.d(TAG, "Generating APK HMAC signature for: $apkFilePath")
            
            // Get HMAC key
            val hmacKey = if (context != null) {
                SecureHmacHelper.getOrCreateDeviceBoundHmacKey(context)
            } else {
                SecureHmacHelper.getOrCreateSecureHmacKey()
            }
            
            // Read APK file
            val apkFile = File(apkFilePath)
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file not found: $apkFilePath")
                return null
            }
            
            // Calculate APK hash
            val apkHash = calculateFileHash(apkFile)
            Log.d(TAG, "APK hash calculated: ${apkHash.take(16)}...")
            
            // Generate HMAC signature
            val hmacSignature = computeHmacSha256(apkHash.toByteArray(), hmacKey)
            Log.d(TAG, "APK HMAC signature generated: ${hmacSignature.take(16)}...")
            
            hmacSignature
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate APK HMAC signature", e)
            null
        }
    }
    
    /**
     * Verify APK integrity at runtime
     * This should be called during app startup
     */
    @JvmStatic
    suspend fun verifyApkIntegrity(context: Context): ApkIntegrityInfo {
        return try {
            Log.d(TAG, "Verifying APK integrity...")
            
            // Get package info
            val packageInfo = getPackageInfo(context)
            val packageName = context.packageName
            
            // Get APK file path
            val apkFilePath = getApkFilePath(context)
            if (apkFilePath == null) {
                Log.e(TAG, "Could not determine APK file path")
                return createInvalidIntegrityInfo(packageName, "APK path not found")
            }
            
            // Calculate current APK hash
            val apkFile = File(apkFilePath)
            val currentApkHash = calculateFileHash(apkFile)
            Log.d(TAG, "Current APK hash: ${currentApkHash.take(16)}...")
            
            // Get stored HMAC signature (this should be embedded in the app)
            val storedHmacSignature = getStoredHmacSignature(context)
            if (storedHmacSignature == null) {
                Log.e(TAG, "No stored HMAC signature found")
                return createInvalidIntegrityInfo(packageName, "No stored HMAC signature")
            }
            
            // Get HMAC key
            val hmacKey = SecureHmacHelper.getOrCreateDeviceBoundHmacKey(context)
            
            // Verify HMAC signature
            val isValid = SecureHmacHelper.verifyHmacSignature(
                currentApkHash.toByteArray(),
                storedHmacSignature,
                hmacKey
            )
            
            // Get signature hash
            val signatureHash = getSignatureHash(packageInfo)
            
            // Determine key type
            val keyType = when {
                hmacKey.format == "AndroidKeyStore" -> "Hardware-backed"
                else -> "Software"
            }
            
            Log.d(TAG, "APK integrity verification result: $isValid")
            
            ApkIntegrityInfo(
                packageName = packageName,
                versionCode = packageInfo.longVersionCode,
                versionName = packageInfo.versionName ?: "unknown",
                signatureHash = signatureHash,
                apkHash = currentApkHash,
                hmacSignature = storedHmacSignature,
                keyType = keyType,
                timestamp = System.currentTimeMillis(),
                isIntegrityValid = isValid
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify APK integrity", e)
            createInvalidIntegrityInfo(context.packageName, e.message ?: "Unknown error")
        }
    }
    
    /**
     * Detect if APK has been repackaged
     */
    @JvmStatic
    suspend fun detectRepackaging(context: Context): Boolean {
        return try {
            val integrityInfo = verifyApkIntegrity(context)
            
            // Check if integrity is valid
            if (!integrityInfo.isIntegrityValid) {
                Log.w(TAG, "APK integrity check failed - possible repackaging detected")
                return true
            }
            
            // Additional checks for repackaging
            val additionalChecks = performAdditionalRepackagingChecks(context)
            
            Log.d(TAG, "Repackaging detection result: $additionalChecks")
            additionalChecks
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect repackaging", e)
            true // Assume repackaging if we can't verify
        }
    }
    
    /**
     * Store HMAC signature in app assets
     * This should be called during build process
     */
    @JvmStatic
    fun storeHmacSignatureInAssets(
        apkFilePath: String,
        context: Context,
        assetFileName: String = "apk_hmac_signature.txt"
    ): Boolean {
        return try {
            val hmacSignature = generateApkHmacSignature(apkFilePath, context)
            if (hmacSignature == null) {
                Log.e(TAG, "Failed to generate HMAC signature")
                return false
            }
            
            // In a real implementation, you would write this to assets during build
            // For now, we'll store it in SharedPreferences as a demonstration
            val prefs = context.getSharedPreferences("apk_hmac", Context.MODE_PRIVATE)
            prefs.edit().putString("stored_hmac", hmacSignature).apply()
            
            Log.d(TAG, "HMAC signature stored successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store HMAC signature", e)
            false
        }
    }
    
    /**
     * Get stored HMAC signature
     */
    private fun getStoredHmacSignature(context: Context): String? {
        return try {
            // In a real implementation, this would read from assets
            // For demonstration, we'll read from SharedPreferences
            val prefs = context.getSharedPreferences("apk_hmac", Context.MODE_PRIVATE)
            prefs.getString("stored_hmac", null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get stored HMAC signature", e)
            null
        }
    }
    
    /**
     * Calculate SHA-256 hash of a file
     */
    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get package info
     */
    private fun getPackageInfo(context: Context): PackageInfo {
        return context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES
        )
    }
    
    /**
     * Get APK file path
     */
    private fun getApkFilePath(context: Context): String? {
        return try {
            val packageInfo = getPackageInfo(context)
            packageInfo.applicationInfo.sourceDir
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get APK file path", e)
            null
        }
    }
    
    /**
     * Get signature hash
     */
    private fun getSignatureHash(packageInfo: PackageInfo): String {
        return try {
            val signatures = packageInfo.signatures ?: packageInfo.signingInfo?.signingCertificateHistory
            if (signatures.isNullOrEmpty()) {
                return "no_signature"
            }
            
            val signature = signatures[0]
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(signature.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get signature hash", e)
            "error"
        }
    }
    
    /**
     * Perform additional repackaging checks
     */
    private suspend fun performAdditionalRepackagingChecks(context: Context): Boolean {
        return try {
            // Check if app is installed from unknown sources
            val packageInfo = getPackageInfo(context)
            val installerPackageName = context.packageManager.getInstallerPackageName(context.packageName)
            
            if (installerPackageName == null) {
                Log.w(TAG, "App installed from unknown source - possible repackaging")
                return true
            }
            
            // Check for suspicious package names
            val suspiciousInstallers = listOf(
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",
                "com.sec.android.app.samsungapps"
            )
            
            if (installerPackageName in suspiciousInstallers) {
                Log.w(TAG, "App installed from suspicious installer: $installerPackageName")
                return true
            }
            
            // Check for debug signatures
            val signatures = packageInfo.signatures ?: packageInfo.signingInfo?.signingCertificateHistory
            if (signatures != null && signatures.isNotEmpty()) {
                val signature = signatures[0]
                val cert = CertificateFactory.getInstance("X.509")
                    .generateCertificate(ByteArrayInputStream(signature.toByteArray())) as X509Certificate
                
                val subject = cert.subjectDN.toString()
                if (subject.contains("debug") || subject.contains("test")) {
                    Log.w(TAG, "Debug signature detected - possible repackaging")
                    return true
                }
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform additional repackaging checks", e)
            true
        }
    }
    
    /**
     * Create invalid integrity info
     */
    private fun createInvalidIntegrityInfo(packageName: String, error: String): ApkIntegrityInfo {
        return ApkIntegrityInfo(
            packageName = packageName,
            versionCode = 0,
            versionName = "unknown",
            signatureHash = "error",
            apkHash = "error",
            hmacSignature = "error",
            keyType = "unknown",
            timestamp = System.currentTimeMillis(),
            isIntegrityValid = false
        )
    }
    
    /**
     * Compute HMAC-SHA256
     */
    private fun computeHmacSha256(data: ByteArray, key: javax.crypto.SecretKey): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(key)
        val result = mac.doFinal(data)
        return result.joinToString("") { "%02x".format(it) }
    }
}
