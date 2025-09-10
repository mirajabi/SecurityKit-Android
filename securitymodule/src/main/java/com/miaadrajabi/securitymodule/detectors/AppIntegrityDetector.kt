package com.miaadrajabi.securitymodule.detectors

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import com.miaadrajabi.securitymodule.SecurityFinding
import com.miaadrajabi.securitymodule.Severity
import com.miaadrajabi.securitymodule.crypto.CryptoUtils
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Advanced App Integrity detector with multi-layer verification:
 * - Package name validation
 * - Signing certificate verification (v2/v3/v4)
 * - Installer package validation
 * - APK/DEX checksum verification
 * - Native library (.so) checksum verification
 */
object AppIntegrityDetector {

    data class AppIntegrityConfig(
        val expectedPackageName: String,
        val expectedSigningHashes: Set<String>,
        val allowedInstallers: Set<String> = setOf(
            "com.android.vending", // Google Play Store
            "com.huawei.appmarket", // Huawei AppGallery
            "com.samsung.android.galaxyapps", // Samsung Galaxy Store
            "com.amazon.venezia", // Amazon Appstore
            "com.sec.android.app.samsungapps" // Samsung Apps
        ),
        val expectedDexChecksums: Map<String, String> = emptyMap(), // version -> sha256
        val expectedSoChecksums: Map<String, String> = emptyMap() // arch -> sha256
    )

    /**
     * Perform comprehensive app integrity checks.
     */
    fun checkAppIntegrity(context: Context, config: AppIntegrityConfig): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            // 1. Package name validation
            findings.addAll(checkPackageName(packageInfo, config.expectedPackageName))

            // 2. Signing certificate verification
            findings.addAll(checkSigningCertificates(packageInfo, config.expectedSigningHashes))

            // 3. Installer package validation
            findings.addAll(checkInstallerPackage(context, config.allowedInstallers))

            // 4. APK/DEX checksum verification
            findings.addAll(checkDexChecksums(context, config.expectedDexChecksums))

            // 5. Native library checksum verification
            findings.addAll(checkNativeLibraryChecksums(context, config.expectedSoChecksums))

        } catch (e: Exception) {
            findings.add(SecurityFinding(
                id = "app_integrity_error",
                title = "App Integrity Check Failed",
                severity = Severity.BLOCK,
                metadata = mapOf("error" to (e.message ?: "Unknown error"))
            ))
        }

        return findings
    }

    /**
     * Check if the package name matches the expected value.
     */
    private fun checkPackageName(packageInfo: PackageInfo, expectedPackageName: String): List<SecurityFinding> {
        val actualPackageName = packageInfo.packageName
        return if (actualPackageName == expectedPackageName) {
            listOf(SecurityFinding(
                id = "package_name_valid",
                title = "Package Name Valid",
                severity = Severity.OK,
                metadata = mapOf("package_name" to actualPackageName)
            ))
        } else {
            listOf(SecurityFinding(
                id = "package_name_mismatch",
                title = "Package Name Mismatch",
                severity = Severity.BLOCK,
                metadata = mapOf("expected" to expectedPackageName, "actual" to actualPackageName)
            ))
        }
    }

    /**
     * Check signing certificates against expected hashes.
     */
    private fun checkSigningCertificates(packageInfo: PackageInfo, expectedHashes: Set<String>): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        if (expectedHashes.isEmpty()) {
            findings.add(SecurityFinding(
                id = "signing_no_expected_hashes",
                title = "No Expected Signing Hashes",
                severity = Severity.WARN,
                metadata = mapOf("message" to "No expected signing certificate hashes configured")
            ))
            return findings
        }

        val actualHashes = mutableSetOf<String>()

        // Get signatures based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use SigningInfo for API 28+
            val signingInfo = packageInfo.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    // Multiple signers (v2/v3 signing)
                    val apkContentsSigners = signingInfo.apkContentsSigners
                    apkContentsSigners.forEach { signature ->
                        val hash = CryptoUtils.sha256(signature.toByteArray()).joinToString("") { "%02x".format(it) }
                        actualHashes.add(hash)
                    }
                } else {
                    // Single signer
                    val signingCertificateHistory = signingInfo.signingCertificateHistory
                    signingCertificateHistory.forEach { signature ->
                        val hash = CryptoUtils.sha256(signature.toByteArray()).joinToString("") { "%02x".format(it) }
                        actualHashes.add(hash)
                    }
                }
            }
        } else {
            // Use legacy signatures for older versions
            @Suppress("DEPRECATION")
            val signatures = packageInfo.signatures
            signatures?.forEach { signature ->
                val hash = CryptoUtils.sha256(signature.toByteArray()).joinToString("") { "%02x".format(it) }
                actualHashes.add(hash)
            }
        }

        // Check if any actual hash matches expected hashes
        val hasValidSignature = actualHashes.any { it in expectedHashes }

        if (hasValidSignature) {
            findings.add(SecurityFinding(
                id = "signing_certificate_valid",
                title = "Signing Certificate Valid",
                severity = Severity.OK,
                metadata = mapOf("message" to "App signing certificate matches expected values")
            ))
        } else {
            findings.add(SecurityFinding(
                id = "signing_certificate_invalid",
                title = "Signing Certificate Invalid",
                severity = Severity.BLOCK,
                metadata = mapOf(
                    "expected" to expectedHashes.joinToString(),
                    "actual" to actualHashes.joinToString()
                )
            ))
        }

        return findings
    }

    /**
     * Check if the app was installed from an allowed installer.
     */
    private fun checkInstallerPackage(context: Context, allowedInstallers: Set<String>): List<SecurityFinding> {
        val installerPackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }

        return if (installerPackageName == null) {
            listOf(SecurityFinding(
                id = "installer_unknown",
                title = "Installer Package Unknown",
                severity = Severity.WARN,
                metadata = mapOf("message" to "Could not determine installer package name")
            ))
        } else if (installerPackageName in allowedInstallers) {
            listOf(SecurityFinding(
                id = "installer_valid",
                title = "Installer Package Valid",
                severity = Severity.OK,
                metadata = mapOf("installer" to installerPackageName)
            ))
        } else {
            listOf(SecurityFinding(
                id = "installer_invalid",
                title = "Installer Package Invalid",
                severity = Severity.BLOCK,
                metadata = mapOf(
                    "installer" to installerPackageName,
                    "allowed" to allowedInstallers.joinToString()
                )
            ))
        }
    }

    /**
     * Check DEX file checksums.
     */
    private fun checkDexChecksums(context: Context, expectedChecksums: Map<String, String>): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        if (expectedChecksums.isEmpty()) {
            return findings // Skip if no expected checksums configured
        }

        try {
            val apkPath = context.applicationInfo.sourceDir
            val actualChecksums = calculateDexChecksums(apkPath)

            var allValid = true
            for ((version, expectedHash) in expectedChecksums) {
                val actualHash = actualChecksums[version]
                if (actualHash == null) {
                    findings.add(SecurityFinding(
                        id = "dex_checksum_missing_$version",
                        title = "DEX Checksum Missing for Version $version",
                        severity = Severity.WARN,
                        metadata = mapOf("version" to version)
                    ))
                    allValid = false
                } else if (actualHash != expectedHash) {
                    findings.add(SecurityFinding(
                        id = "dex_checksum_mismatch_$version",
                        title = "DEX Checksum Mismatch for Version $version",
                        severity = Severity.BLOCK,
                        metadata = mapOf(
                            "version" to version,
                            "expected" to expectedHash,
                            "actual" to actualHash
                        )
                    ))
                    allValid = false
                }
            }

            if (allValid && actualChecksums.isNotEmpty()) {
                findings.add(SecurityFinding(
                    id = "dex_checksums_valid",
                    title = "DEX Checksums Valid",
                    severity = Severity.OK,
                    metadata = mapOf("count" to actualChecksums.size.toString())
                ))
            }

        } catch (e: Exception) {
            findings.add(SecurityFinding(
                id = "dex_checksum_error",
                title = "DEX Checksum Verification Failed",
                severity = Severity.WARN,
                metadata = mapOf("error" to (e.message ?: "Unknown error"))
            ))
        }

        return findings
    }

    /**
     * Check native library (.so) checksums.
     */
    private fun checkNativeLibraryChecksums(context: Context, expectedChecksums: Map<String, String>): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        if (expectedChecksums.isEmpty()) {
            return findings // Skip if no expected checksums configured
        }

        try {
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val actualChecksums = calculateNativeLibraryChecksums(nativeLibDir)

            var allValid = true
            for ((arch, expectedHash) in expectedChecksums) {
                val actualHash = actualChecksums[arch]
                if (actualHash == null) {
                    findings.add(SecurityFinding(
                        id = "native_lib_missing_$arch",
                        title = "Native Library Missing for Architecture $arch",
                        severity = Severity.WARN,
                        metadata = mapOf("architecture" to arch)
                    ))
                    allValid = false
                } else if (actualHash != expectedHash) {
                    findings.add(SecurityFinding(
                        id = "native_lib_checksum_mismatch_$arch",
                        title = "Native Library Checksum Mismatch for $arch",
                        severity = Severity.BLOCK,
                        metadata = mapOf(
                            "architecture" to arch,
                            "expected" to expectedHash,
                            "actual" to actualHash
                        )
                    ))
                    allValid = false
                }
            }

            if (allValid && actualChecksums.isNotEmpty()) {
                findings.add(SecurityFinding(
                    id = "native_lib_checksums_valid",
                    title = "Native Library Checksums Valid",
                    severity = Severity.OK,
                    metadata = mapOf("count" to actualChecksums.size.toString())
                ))
            }

        } catch (e: Exception) {
            findings.add(SecurityFinding(
                id = "native_lib_checksum_error",
                title = "Native Library Checksum Verification Failed",
                severity = Severity.WARN,
                metadata = mapOf("error" to (e.message ?: "Unknown error"))
            ))
        }

        return findings
    }

    /**
     * Calculate checksums for DEX files in the APK.
     */
    private fun calculateDexChecksums(apkPath: String): Map<String, String> {
        val checksums = mutableMapOf<String, String>()
        
        try {
            ZipInputStream(FileInputStream(apkPath)).use { zipStream ->
                var entry: ZipEntry? = zipStream.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("classes") && entry.name.endsWith(".dex")) {
                        val version = entry.name.removePrefix("classes").removeSuffix(".dex")
                        val versionKey = if (version.isEmpty()) "main" else version
                        
                        val digest = MessageDigest.getInstance("SHA-256")
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (zipStream.read(buffer).also { bytesRead = it } != -1) {
                            digest.update(buffer, 0, bytesRead)
                        }
                        val hash = digest.digest().joinToString("") { "%02x".format(it) }
                        checksums[versionKey] = hash
                    }
                    entry = zipStream.nextEntry
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the entire check
        }
        
        return checksums
    }

    /**
     * Calculate checksums for native libraries.
     */
    private fun calculateNativeLibraryChecksums(nativeLibDir: File): Map<String, String> {
        val checksums = mutableMapOf<String, String>()
        
        if (!nativeLibDir.exists() || !nativeLibDir.isDirectory) {
            return checksums
        }

        try {
            nativeLibDir.listFiles()?.forEach { archDir ->
                if (archDir.isDirectory) {
                    val archName = archDir.name
                    val soFiles = archDir.listFiles { _, name -> name.endsWith(".so") }
                    
                    if (soFiles != null && soFiles.isNotEmpty()) {
                        // Calculate combined hash of all .so files in this architecture
                        val digest = MessageDigest.getInstance("SHA-256")
                        soFiles.sortedBy { it.name }.forEach { soFile ->
                            soFile.inputStream().use { input ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    digest.update(buffer, 0, bytesRead)
                                }
                            }
                        }
                        val hash = digest.digest().joinToString("") { "%02x".format(it) }
                        checksums[archName] = hash
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the entire check
        }
        
        return checksums
    }
}
