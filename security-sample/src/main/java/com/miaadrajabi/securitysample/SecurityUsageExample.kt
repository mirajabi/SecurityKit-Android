package com.miaadrajabi.securitysample

import android.content.Context
import com.miaadrajabi.securitymodule.SecurityModule
import com.miaadrajabi.securitymodule.SecurityReport
import com.miaadrajabi.securitymodule.Severity
import com.miaadrajabi.securitymodule.config.SecurityConfig
import com.miaadrajabi.securitymodule.config.SecurityConfigLoader
import com.miaadrajabi.securitymodule.crypto.SecureHmacHelper
import com.miaadrajabi.securitymodule.examples.SecureHmacExample

/**
 * Comprehensive example showing how to use SecurityModule
 * with device-specific overrides and security policy handling
 */
object SecurityUsageExample {

    /**
     * Example 1: Basic usage with automatic security checks
     * If any security condition fails, user will be redirected to warning page
     */
    fun basicUsage(context: Context) {
        // Load configuration from assets with secure HMAC (recommended for production)
        val config = SecurityConfigLoader.fromAssetPreferSigned(
            context = context,
            assetName = "security_config.json",
            signatureAssetName = "security_config.sig",
            useSecureHmac = true // Use Android Keystore for HMAC verification
        )
        
        // Build SecurityModule
        val securityModule = SecurityModule.Builder(context)
            .setConfig(config)
            .build()
        
        // Run security checks
        val report = securityModule.runAllChecksBlocking()
        
        // Check if security issues were found
        if (report.overallSeverity == Severity.BLOCK) {
            // Critical security issues detected - user will be redirected to ReportActivity
            // and the app will exit after showing the warning
            ReportActivity.start(context, report, config)
            return
        }
        
        // All security checks passed - continue with normal app flow
        // Your app logic here...
    }

    /**
     * Example 2: Advanced usage with custom device overrides
     * Configure specific devices to bypass security checks
     */
    fun advancedUsageWithOverrides(context: Context) {
        // Create custom configuration with device overrides
        val config = SecurityConfig(
            features = SecurityConfig.Features(
                rootDetection = true,
                emulatorDetection = true,
                debuggerDetection = true,
                usbDebugDetection = true,
                vpnDetection = true,
                mitmDetection = true,
                screenCaptureProtection = true,
                appSignatureVerification = false, // Disable for testing
                repackagingDetection = true,
                playIntegrityCheck = false,
                advancedAppIntegrity = true,
                strongBoxKeys = true,
                deviceBinding = true,
                tamperEvidence = true
            ),
            thresholds = SecurityConfig.Thresholds(
                emulatorSignalsToBlock = 3, // More lenient
                rootSignalsToBlock = 3
            ),
            overrides = SecurityConfig.Overrides(
                // Allow specific devices to bypass security checks
                allowedModels = listOf(
                    "Redmi Note 9",
                    "motion_phone_arm64", // Genymotion emulator
                    "Pixel 6",
                    "Galaxy S21"
                ),
                allowedBrands = listOf("Xiaomi", "Google", "Samsung"),
                allowedManufacturers = listOf("Genymobile"), // For emulators
                allowedProducts = listOf("motion_phone_arm64"),
                deniedModels = listOf("Unknown Device"),
                deniedBrands = listOf("HackerPhone")
            ),
            policy = SecurityConfig.PolicyRules(
                onRoot = SecurityConfig.Action.BLOCK,
                onEmulator = SecurityConfig.Action.WARN, // More lenient for emulators
                onDebugger = SecurityConfig.Action.WARN,
                onUsbDebug = SecurityConfig.Action.WARN,
                onVpn = SecurityConfig.Action.WARN,
                onMitm = SecurityConfig.Action.BLOCK,
                onPlayIntegrityFailure = SecurityConfig.Action.WARN,
                onAppIntegrityFailure = SecurityConfig.Action.BLOCK,
                onConfigTampering = SecurityConfig.Action.BLOCK,
                onStrongBoxUnavailable = SecurityConfig.Action.WARN
            ),
            appIntegrity = SecurityConfig.AppIntegrity(
                expectedPackageName = context.packageName,
                expectedSignatureSha256 = emptyList(), // Add your signature hashes
                allowedInstallers = listOf(
                    "com.android.vending",
                    "com.huawei.appmarket",
                    "com.samsung.android.galaxyapps"
                )
            )
        )
        
        // Build SecurityModule with custom config
        val securityModule = SecurityModule.Builder(context)
            .setConfig(config)
            .build()
        
        // Run security checks
        val report = securityModule.runAllChecksBlocking()
        
        // Handle results based on severity
        when (report.overallSeverity) {
            Severity.OK -> {
                // All checks passed - continue normally
                println("Security checks passed - device is secure")
            }
            Severity.WARN -> {
                // Warnings detected - show warning but allow continuation
                println("Security warnings detected: ${report.findings.size} issues")
                report.findings.forEach { finding ->
                    println("- ${finding.title}: ${finding.severity}")
                }
                // Continue with app flow but log warnings
            }
            Severity.BLOCK -> {
                // Critical security issues - redirect to warning page
                println("Critical security issues detected - redirecting to warning page")
                ReportActivity.start(context, report, config)
                return
            }
            Severity.INFO -> {
                // Info level - continue normally
                println("Security info: ${report.findings.size} findings")
            }
        }
    }

    /**
     * Example 3: Simple usage with error handling
     */
    fun simpleUsage(context: Context) {
        try {
            val config = SecurityConfigLoader.fromAsset(context, "security_config.json")
            val securityModule = SecurityModule.Builder(context)
                .setConfig(config)
                .build()
            
            // Run checks
            val report = securityModule.runAllChecksBlocking()
            
            if (report.overallSeverity == Severity.BLOCK) {
                ReportActivity.start(context, report, config)
            } else {
                // Continue with app flow
                println("Security checks completed successfully")
            }
        } catch (e: Exception) {
            println("Security check failed: ${e.message}")
            // Handle error appropriately
        }
    }

    /**
     * Example 4: Device-specific configuration for the Xiaomi Redmi Note 9 from the image
     */
    fun xiaomiRedmiNote9Configuration(context: Context): SecurityConfig {
        return SecurityConfig(
            features = SecurityConfig.Features(
                rootDetection = true,
                emulatorDetection = true,
                debuggerDetection = true,
                usbDebugDetection = true,
                vpnDetection = true,
                mitmDetection = true,
                screenCaptureProtection = true,
                appSignatureVerification = true,
                repackagingDetection = true,
                playIntegrityCheck = false,
                advancedAppIntegrity = true,
                strongBoxKeys = true,
                deviceBinding = true,
                tamperEvidence = true
            ),
            overrides = SecurityConfig.Overrides(
                // Specific configuration for Xiaomi Redmi Note 9
                allowedModels = listOf("Redmi Note 9"),
                allowedBrands = listOf("Xiaomi"),
                allowedManufacturers = listOf("Xiaomi", "Genymobile"), // Allow both Xiaomi and Genymotion
                allowedProducts = listOf("motion_phone_arm64"), // From the image
                allowedDevices = listOf("motion_phone_arm64")
            ),
            policy = SecurityConfig.PolicyRules(
                onRoot = SecurityConfig.Action.BLOCK,
                onEmulator = SecurityConfig.Action.WARN, // Allow emulator with warning
                onDebugger = SecurityConfig.Action.WARN,
                onUsbDebug = SecurityConfig.Action.WARN,
                onVpn = SecurityConfig.Action.WARN,
                onMitm = SecurityConfig.Action.BLOCK,
                onPlayIntegrityFailure = SecurityConfig.Action.WARN,
                onAppIntegrityFailure = SecurityConfig.Action.BLOCK,
                onConfigTampering = SecurityConfig.Action.BLOCK,
                onStrongBoxUnavailable = SecurityConfig.Action.WARN
            ),
            appIntegrity = SecurityConfig.AppIntegrity(
                expectedPackageName = context.packageName,
                expectedSignatureSha256 = emptyList(), // Add your actual signature hashes
                allowedInstallers = listOf(
                    "com.android.vending",
                    "com.xiaomi.mipicks", // Xiaomi App Store
                    "com.huawei.appmarket"
                )
            )
        )
    }

    /**
     * Example 5: How to check if current device is in allowed list
     */
    fun isDeviceAllowed(context: Context, config: SecurityConfig): Boolean {
        val model = android.os.Build.MODEL ?: ""
        val brand = android.os.Build.BRAND ?: ""
        val manufacturer = android.os.Build.MANUFACTURER ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        val device = android.os.Build.DEVICE ?: ""
        val board = android.os.Build.BOARD ?: ""
        
        return config.overrides.allowedModels.contains(model) ||
               config.overrides.allowedBrands.contains(brand) ||
               config.overrides.allowedManufacturers.contains(manufacturer) ||
               config.overrides.allowedProducts.contains(product) ||
               config.overrides.allowedDevices.contains(device) ||
               config.overrides.allowedBoards.contains(board)
    }

    /**
     * Example 6: Complete usage with error handling
     */
    fun completeUsageExample(context: Context) {
        try {
            // Load configuration
            val config = SecurityConfigLoader.fromAsset(context, "security_config.json")
            
            // Check if device is in allowed list
            if (isDeviceAllowed(context, config)) {
                println("Device is in allowed list - bypassing security checks")
                // Continue with app flow
                return
            }
            
            // Build SecurityModule
            val securityModule = SecurityModule.Builder(context)
                .setConfig(config)
                .build()
            
            // Run security checks
            val report = securityModule.runAllChecksBlocking()
            
            // Handle results
            when (report.overallSeverity) {
                Severity.OK -> {
                    println("✅ All security checks passed")
                    // Continue with normal app flow
                }
                Severity.WARN -> {
                    println("⚠️ Security warnings detected:")
                    report.findings.forEach { finding ->
                        println("  - ${finding.title}: ${finding.severity}")
                    }
                    // Show warnings but allow continuation
                }
                Severity.BLOCK -> {
                    println("🚫 Critical security issues detected:")
                    report.findings.forEach { finding ->
                        println("  - ${finding.title}: ${finding.severity}")
                    }
                    // Redirect to warning page and exit
                    ReportActivity.start(context, report, config)
                    return
                }
                Severity.INFO -> {
                    println("ℹ️ Security info:")
                    report.findings.forEach { finding ->
                        println("  - ${finding.title}: ${finding.severity}")
                    }
                    // Continue with normal app flow
                }
            }
            
        } catch (e: Exception) {
            println("❌ Security check failed: ${e.message}")
            // Handle error - you might want to show an error dialog
            // or fallback to a basic security check
        }
    }

    /**
     * Example: Secure HMAC usage with Android Keystore
     * This demonstrates the new secure HMAC implementation
     */
    fun secureHmacUsage(context: Context) {
        // Check StrongBox availability
        val strongBoxAvailable = SecureHmacHelper.isStrongBoxAvailableForHmac()
        println("StrongBox available for HMAC: $strongBoxAvailable")

        // Load configuration with secure HMAC
        val config = SecureHmacExample.loadProductionConfig(context)
        
        // Build SecurityModule with secure configuration
        val securityModule = SecurityModule.Builder(context)
            .setConfig(config!!)
            .build()
        
        // Run security checks
        val report = securityModule.runAllChecksBlocking()
        
        // Check for configuration tampering
        val configTampering = report.findings.any { it.id == "config_tampering" }
        if (configTampering) {
            println("Configuration tampering detected!")
        }
        
        // Test HMAC generation and verification
        val testData = "test configuration data"
        val signature = SecureHmacExample.generateHmacForTesting(context, testData)
        if (signature != null) {
            val isValid = SecureHmacExample.verifyConfigManually(context, testData, signature)
            println("HMAC verification result: $isValid")
        }
    }

    /**
     * Example: Migration from legacy HMAC to secure HMAC
     */
    fun migrateToSecureHmac(context: Context) {
        // Test migration
        val migrationSuccess = SecureHmacExample.migrateToSecureHmac(context)
        if (migrationSuccess) {
            println("Successfully migrated to secure HMAC")
            
            // Now use secure HMAC for configuration loading
            val config = SecurityConfigLoader.fromAssetPreferSigned(
                context = context,
                useSecureHmac = true
            )
            
            val securityModule = SecurityModule.Builder(context)
                .setConfig(config)
                .build()
            
            val report = securityModule.runAllChecksBlocking()
            println("Security report with secure HMAC: ${report.overallSeverity}")
        } else {
            println("Migration to secure HMAC failed, using fallback")
        }
    }
}
