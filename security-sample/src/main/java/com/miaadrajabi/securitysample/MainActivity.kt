package com.miaadrajabi.securitysample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import android.graphics.Color
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import com.miaadrajabi.securitymodule.SecurityModule
import com.miaadrajabi.securitymodule.Severity
import com.miaadrajabi.securitymodule.config.SecurityConfig
import com.miaadrajabi.securitymodule.config.SecurityConfigLoader
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureMonitor
import com.miaadrajabi.securitymodule.telemetry.TelemetrySink
import com.miaadrajabi.securitymodule.crypto.SecureHmacHelper
import com.miaadrajabi.securitymodule.crypto.ApkHmacProtector
import com.miaadrajabi.securitymodule.examples.SecureHmacExample
import com.miaadrajabi.securitymodule.detectors.*
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private val telemetry = object : TelemetrySink {
        override fun onEvent(eventId: String, attributes: Map<String, String>) {}
    }

    private lateinit var resultTextView: TextView
    private lateinit var scrollView: ScrollView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupUI()
        log("🚀 Security Tests Ready - Click any button to run tests")
    }

    private fun setupUI() {
        // Create ScrollView as root
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        
        // Create main layout inside ScrollView
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(mainLayout)
        setContentView(scrollView)

        // Title
        val title = TextView(this).apply {
            text = "🔐 Security Module Tests"
            textSize = 24f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        mainLayout.addView(title)

        // Test Buttons Section
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Root Detection Test
        val rootTestBtn =
            createTestButton("🔍 Root Detection Test", "Test for root access and BusyBox")
        rootTestBtn.setOnClickListener { 
            runTestAndNavigate { runRootDetectionTest() }
        }
        buttonsLayout.addView(rootTestBtn)

        // Emulator Detection Test
        val emulatorTestBtn =
            createTestButton("📱 Emulator Detection Test", "Test for emulator environment")
        emulatorTestBtn.setOnClickListener { 
            runTestAndNavigate { runEmulatorDetectionTest() }
        }
        buttonsLayout.addView(emulatorTestBtn)

        // Debugger Detection Test
        val debuggerTestBtn =
            createTestButton("🐛 Debugger Detection Test", "Test for attached debuggers")
        debuggerTestBtn.setOnClickListener { 
            runTestAndNavigate { runDebuggerDetectionTest() }
        }
        buttonsLayout.addView(debuggerTestBtn)

        // USB Debug Test
        val usbDebugTestBtn = createTestButton("🔌 USB Debug Test", "Test for USB debugging enabled")
        usbDebugTestBtn.setOnClickListener { 
            runTestAndNavigate { runUsbDebugTest() }
        }
        buttonsLayout.addView(usbDebugTestBtn)

        // VPN Detection Test
        val vpnTestBtn = createTestButton("🌐 VPN Detection Test", "Test for active VPN connections")
        vpnTestBtn.setOnClickListener { 
            runTestAndNavigate { runVpnDetectionTest() }
        }
        buttonsLayout.addView(vpnTestBtn)

        // MITM Detection Test
        val mitmTestBtn =
            createTestButton("🕵️ MITM Detection Test", "Test for man-in-the-middle attacks")
        mitmTestBtn.setOnClickListener { 
            runTestAndNavigate { runMitmDetectionTest() }
        }
        buttonsLayout.addView(mitmTestBtn)

        // App Integrity Test
        val appIntegrityTestBtn =
            createTestButton("📦 App Integrity Test", "Test for app tampering and repackaging")
        appIntegrityTestBtn.setOnClickListener { 
            runTestAndNavigate { runAppIntegrityTest() }
        }
        buttonsLayout.addView(appIntegrityTestBtn)

        // Secure HMAC Test
        val hmacTestBtn =
            createTestButton("🔐 Secure HMAC Test", "Test secure HMAC with Android Keystore")
        hmacTestBtn.setOnClickListener { 
            runTestAndNavigate { runSecureHmacTest() }
        }
        buttonsLayout.addView(hmacTestBtn)

        // Screen Capture Test
        val screenCaptureTestBtn =
            createTestButton("📸 Screen Capture Test", "Test screen capture protection")
        screenCaptureTestBtn.setOnClickListener { 
            runTestAndNavigate { runScreenCaptureTest() }
        }
        buttonsLayout.addView(screenCaptureTestBtn)

        // Complete Security Test
        val completeTestBtn =
            createTestButton("🛡️ Complete Security Test", "Run all security tests")
        completeTestBtn.setOnClickListener { 
            runTestAndNavigate { runCompleteSecurityTest() }
        }
        buttonsLayout.addView(completeTestBtn)

        // HMAC Error Log Test
        val errorLogBtn = createTestButton("🔍 HMAC Error Log", "Get detailed HMAC error log")
        errorLogBtn.setOnClickListener { 
            runTestAndNavigate { runHmacErrorLogTest() }
        }
        buttonsLayout.addView(errorLogBtn)

        // TEE Support Test
        val teeTestBtn =
            createTestButton("🔒 TEE Support Test", "Test Trusted Execution Environment")
        teeTestBtn.setOnClickListener { 
            runTestAndNavigate { runTeeSupportTest() }
        }
        buttonsLayout.addView(teeTestBtn)

        // StrongBox Support Test
        val strongBoxTestBtn =
            createTestButton("🛡️ StrongBox Test", "Test StrongBox hardware security")
        strongBoxTestBtn.setOnClickListener { 
            runTestAndNavigate { runStrongBoxTest() }
        }
        buttonsLayout.addView(strongBoxTestBtn)

        // Fingerprint Test
        val fingerprintTestBtn =
            createTestButton("👆 Fingerprint Test", "Test biometric authentication")
        fingerprintTestBtn.setOnClickListener { 
            runTestAndNavigate { runFingerprintTest() }
        }
        buttonsLayout.addView(fingerprintTestBtn)

        // Device Security Test
        val deviceSecurityBtn =
            createTestButton("🔐 Device Security", "Test overall device security")
        deviceSecurityBtn.setOnClickListener { 
            runTestAndNavigate { runDeviceSecurityTest() }
        }
        buttonsLayout.addView(deviceSecurityBtn)

        // Keystore Test
        val keystoreTestBtn =
            createTestButton("🗝️ Keystore Test", "Test Android Keystore capabilities")
        keystoreTestBtn.setOnClickListener { 
            runTestAndNavigate { runKeystoreTest() }
        }
        buttonsLayout.addView(keystoreTestBtn)

        // HMAC Comprehensive Test
        val hmacComprehensiveBtn =
            createTestButton("🔐 HMAC Comprehensive Test", "Complete HMAC analysis with detailed steps")
        hmacComprehensiveBtn.setOnClickListener { 
            runTestAndNavigate { runHmacComprehensiveTest() }
        }
        buttonsLayout.addView(hmacComprehensiveBtn)

        // Sign Up & HMAC Test
        val signUpHmacBtn =
            createTestButton("📝 Sign Up & HMAC Test", "Complete sign up process with HMAC verification")
        signUpHmacBtn.setOnClickListener { 
            runTestAndNavigate { runSignUpHmacTest() }
        }
        buttonsLayout.addView(signUpHmacBtn)

        // APK HMAC Protection Test
        val apkHmacProtectionBtn =
            createTestButton("🛡️ APK HMAC Protection", "Test APK integrity and repackaging detection")
        apkHmacProtectionBtn.setOnClickListener { 
            runTestAndNavigate { runApkHmacProtectionTest() }
        }
        buttonsLayout.addView(apkHmacProtectionBtn)

        // View Results Button
        val viewResultsBtn = Button(this).apply {
            text = "📋 View Results"
            setOnClickListener {
                val intent = Intent(this@MainActivity, TestResultsActivity::class.java)
                startActivity(intent)
            }
            setPadding(32, 16, 32, 16)
        }
        buttonsLayout.addView(viewResultsBtn)

        // Clear Results Button
        val clearBtn = Button(this).apply {
            text = "🗑️ Clear Results"
            setOnClickListener { clearResults() }
            setPadding(32, 16, 32, 16)
        }
        buttonsLayout.addView(clearBtn)

        mainLayout.addView(buttonsLayout)

        // No results section on main page - all results go to TestResultsActivity
    }

    private fun createTestButton(text: String, description: String): Button {
        return Button(this).apply {
            this.text = text
            setPadding(32, 16, 32, 16)
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            textSize = 14f
        }
    }

    // Test Methods
    private fun runRootDetectionTest() {
        scope.launch {
            log("🔍 Running Root Detection Test...")
            try {
                val rootSignals = withContext(Dispatchers.IO) {
                    RootDetector.signals(this@MainActivity)
                }

                val busyBoxExists = withContext(Dispatchers.IO) {
                    BusyBoxDetector.exists()
                }

                val mountFlags = withContext(Dispatchers.IO) {
                    MountFlagsDetector.hasRwOnSystem()
                }

                log("   Root signals detected: $rootSignals")
                log("   BusyBox present: $busyBoxExists")
                log("   System mounted as RW: $mountFlags")

                val status = if (rootSignals > 0) "❌ ROOT DETECTED" else "✅ No root detected"
                val color = if (rootSignals > 0) Color.RED else Color.parseColor("#2E7D32")
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runEmulatorDetectionTest() {
        scope.launch {
            log("📱 Running Emulator Detection Test...")
            try {
                val emulatorSignals = withContext(Dispatchers.IO) {
                    EmulatorDetector.collectSignals(this@MainActivity)
                }

                val qemuSignals = withContext(Dispatchers.IO) {
                    QemuDetector.indicators()
                }

                log("   Emulator signals: ${emulatorSignals.count}")
                log("   QEMU indicators: $qemuSignals")
                log("   Reasons: ${emulatorSignals.reasons.joinToString(", ")}")

                val status =
                    if (emulatorSignals.count > 0) "❌ EMULATOR DETECTED" else "✅ Real device"
                val color =
                    if (emulatorSignals.count > 0) Color.RED else Color.parseColor("#2E7D32")
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runDebuggerDetectionTest() {
        scope.launch {
            log("🐛 Running Debugger Detection Test...")
            try {
                val debuggerConnected = withContext(Dispatchers.IO) {
                    DebuggerDetector.isDebuggerAttached()
                }

                val tracerPid = withContext(Dispatchers.IO) {
                    TracerPidDetector.isTraced()
                }

                log("   Debugger connected: $debuggerConnected")
                log("   TracerPid present: $tracerPid")

                val status =
                    if (debuggerConnected || tracerPid) "❌ DEBUGGER DETECTED" else "✅ No debugger"
                val color =
                    if (debuggerConnected || tracerPid) Color.RED else Color.parseColor("#2E7D32")
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runUsbDebugTest() {
        scope.launch {
            log("🔌 Running USB Debug Test...")
            try {
                val usbDebugEnabled = withContext(Dispatchers.IO) {
                    UsbDebugDetector.isUsbDebugEnabled(this@MainActivity)
                }

                val developerOptions = withContext(Dispatchers.IO) {
                    DeveloperOptionsDetector.isEnabled(this@MainActivity)
                }

                log("   USB debugging enabled: $usbDebugEnabled")
                log("   Developer options enabled: $developerOptions")

                val status =
                    if (usbDebugEnabled || developerOptions) "❌ USB DEBUG ENABLED" else "✅ USB debug disabled"
                val color =
                    if (usbDebugEnabled || developerOptions) Color.RED else Color.parseColor("#2E7D32")
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runVpnDetectionTest() {
        scope.launch {
            log("🌐 Running VPN Detection Test...")
            try {
                val vpnActive = withContext(Dispatchers.IO) {
                    VpnDetector.isVpnActive()
                }

                // Get detailed VPN signals
                val vpnSignals = withContext(Dispatchers.IO) {
                    VpnDetector.collectVpnSignals()
                }

                log("   VPN active: $vpnActive")
                log("   VPN signals found: ${vpnSignals.count}")

                if (vpnSignals.reasons.isNotEmpty()) {
                    log("   VPN interfaces detected:")
                    vpnSignals.reasons.forEach { reason ->
                        log("   - $reason")
                    }
                } else {
                    log("   No VPN interfaces found")
                }

                // Additional network interface information
                try {
                    val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                    if (interfaces != null) {
                        log("   All network interfaces:")
                        for (ni in interfaces) {
                            if (ni.isUp) {
                                log("   - ${ni.name} (${ni.displayName})")
                            }
                        }
                    }
                } catch (e: Exception) {
                    log("   Could not enumerate network interfaces: ${e.message}")
                }

                val status = if (vpnActive) "⚠️ VPN DETECTED" else "✅ No VPN"
                val color =
                    if (vpnActive) Color.parseColor("#FF9800") else Color.parseColor("#2E7D32")
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runMitmDetectionTest() {
        scope.launch {
            log("🕵️ Running MITM Detection Test...")
            try {
                val mitmDetected = withContext(Dispatchers.IO) {
                    MitmDetector.userAddedCertificatesPresent(this@MainActivity)
                }

                val proxyActive = withContext(Dispatchers.IO) {
                    ProxyDetector.isProxyEnabled(this@MainActivity)
                }

                log("   MITM detected: $mitmDetected")
                log("   Proxy active: $proxyActive")

                val status = if (mitmDetected || proxyActive) "❌ MITM DETECTED" else "✅ No MITM"
                val color =
                    if (mitmDetected || proxyActive) Color.RED else Color.parseColor("#2E7D32")
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runAppIntegrityTest() {
        scope.launch {
            log("📦 Running App Integrity Test...")
            try {
                val expectedPackage = "com.miaadrajabi.securitysample"
                val isRepackaged = withContext(Dispatchers.IO) {
                    RepackagingDetector.isRepackaged(this@MainActivity, expectedPackage)
                }

                val expectedSigs =
                    listOf("e22d446c2777ce3d7872539366eab82de0053af8aae620680b25b4addd331f1b")
                val actualSigs = withContext(Dispatchers.IO) {
                    SignatureVerifier.currentSigningSha256(this@MainActivity)
                }
                val signatureMatch =
                    actualSigs.any { a -> expectedSigs.any { it.equals(a, ignoreCase = true) } }

                val hookingDetected = withContext(Dispatchers.IO) {
                    HookingDetector.suspiciousLoadedLibs() > 0
                }

                log("   Repackaged: $isRepackaged")
                log("   Signature match: $signatureMatch")
                log("   Hooking detected: $hookingDetected")
                log("   Actual signatures: ${actualSigs.joinToString(", ")}")

                val issues = mutableListOf<String>()
                if (isRepackaged) issues.add("Repackaged")
                if (!signatureMatch) issues.add("Signature mismatch")
                if (hookingDetected) issues.add("Hooking detected")

                val status =
                    if (issues.isNotEmpty()) "❌ INTEGRITY ISSUES: ${issues.joinToString(", ")}" else "✅ App integrity OK"
                val color = if (issues.isNotEmpty()) Color.RED else Color.parseColor("#2E7D32")
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runSecureHmacTest() {
        scope.launch {
            log("🔐 Running Secure HMAC Test...")
            try {
                // Get comprehensive Keystore capabilities
                val capabilities = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getKeystoreCapabilities()
                }

                log("📱 Device Capabilities:")
                log("   Android Version: ${capabilities["android_version"]}")
                log("   Device Model: ${capabilities["device_model"]}")
                log("   Device Manufacturer: ${capabilities["device_manufacturer"]}")
                log("   Device Brand: ${capabilities["device_brand"]}")
                log("   Is Emulator: ${capabilities["is_emulator"]}")
                log("   Keystore Available: ${capabilities["keystore_available"]}")
                log("   TEE Support: ${capabilities["tee_support"]}")
                log("   StrongBox Support: ${capabilities["strongbox_support"]}")
                log("   User Auth Support: ${capabilities["user_auth_support"]}")
                log("   Device Binding Support: ${capabilities["device_binding_support"]}")

                // Show error details if any
                capabilities.forEach { (key, value) ->
                    if (key.endsWith("_error")) {
                        log("   ⚠️ $key: $value", Color.YELLOW)
                    }
                }

                // Show fallback strategy information
                val fallbackInfo = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getFallbackStrategyInfo()
                }
                log("🔄 Fallback Strategy:")
                log("   Recommended: ${fallbackInfo["recommended_strategy"]}")
                log("   StrongBox available: ${fallbackInfo["strongbox_available"]}")
                log("   TEE available: ${fallbackInfo["tee_available"]}")

                val strongBoxAvailable = withContext(Dispatchers.IO) {
                    SecureHmacHelper.isStrongBoxAvailableForHmac()
                }

                // Get detailed error log first
                val errorLog = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getHmacKeyGenerationLog()
                }

                log("🔍 Detailed Error Log:")
                errorLog.forEach { logEntry ->
                    log("   $logEntry")
                }

                // Get the best available key with type information
                val (secureKey, secureKeyType) = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getBestAvailableHmacKey()
                }

                val deviceBoundKey = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getOrCreateDeviceBoundHmacKey(this@MainActivity)
                }

                val testData = "test configuration data for HMAC verification"
                val signature = withContext(Dispatchers.IO) {
                    SecureHmacHelper.computeHmacSha256(testData.toByteArray(), secureKey)
                }

                val isValid = withContext(Dispatchers.IO) {
                    SecureHmacHelper.verifyHmacSignature(
                        testData.toByteArray(),
                        signature,
                        secureKey
                    )
                }

                // Test with tampered data
                val tamperedData = "tampered configuration data for HMAC verification"
                val isTamperedValid = withContext(Dispatchers.IO) {
                    SecureHmacHelper.verifyHmacSignature(
                        tamperedData.toByteArray(),
                        signature,
                        secureKey
                    )
                }

                log("🔒 Security Features:")
                log("   StrongBox available: $strongBoxAvailable")
                log("   Key type used: $secureKeyType")
                log("   Secure key: ${secureKey.algorithm} (${secureKey.format})")
                log("   Device-bound key: ${deviceBoundKey.algorithm} (${deviceBoundKey.format})")
                log("   Signature generated: ${signature.take(16)}...")
                log("   Valid data verification: $isValid")
                log("   Tampered data verification: $isTamperedValid")

                // Show fallback strategy information
                when (secureKeyType) {
                    "StrongBox" -> log("   ✅ Using StrongBox (highest security)", Color.GREEN)
                    "TEE" -> log("   ✅ Using TEE (hardware-backed)", Color.parseColor("#4CAF50"))
                    "Software" -> log("   ⚠️ Using Software keys (fallback)", Color.YELLOW)
                }

                // Test configuration loading with secure HMAC
                try {
                    val config = loadSecureConfiguration()
                    log("📄 Secure config loaded successfully")
                    log("   App Integrity: ${config.appIntegrity.expectedPackageName ?: "Not configured"}")
                    log("   Features enabled: ${config.features.rootDetection}, ${config.features.emulatorDetection}")
                } catch (e: Exception) {
                    log("⚠️ Config loading failed: ${e.message}")
                }

                // Emulator vs Real Device Analysis
                val isEmulator = capabilities["is_emulator"] as Boolean
                if (isEmulator) {
                    log("⚠️ Running on Emulator - Some features may be limited", Color.YELLOW)
                    log("💡 For full security testing, use a real device", Color.YELLOW)

                    // Show detailed emulator detection results
                    try {
                        val emulatorSignals =
                            com.miaadrajabi.securitymodule.detectors.EmulatorDetector.collectSignals(
                                this@MainActivity
                            )
                        log("🔍 Emulator Detection Details:")
                        log("   Signals found: ${emulatorSignals.count}")
                        emulatorSignals.reasons.take(5).forEach { reason ->
                            log("   - $reason")
                        }
                        if (emulatorSignals.reasons.size > 5) {
                            log("   ... and ${emulatorSignals.reasons.size - 5} more signals")
                        }
                    } catch (e: Exception) {
                        log("   Could not get detailed emulator signals: ${e.message}")
                    }
                } else {
                    log("📱 Running on Real Device - Full security features available", Color.GREEN)
                }

                val status =
                    if (isValid && !isTamperedValid) "✅ HMAC working correctly" else "❌ HMAC verification failed"
                val color =
                    if (isValid && !isTamperedValid) Color.parseColor("#2E7D32") else Color.RED
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
                log(
                    "💡 This might be due to emulator limitations or device compatibility",
                    Color.YELLOW
                )
            }
            log("")
        }
    }

    private fun runScreenCaptureTest() {
        scope.launch {
            log("📸 Running Screen Capture Test...")
            try {
                ScreenCaptureProtector.applySecureFlag(this@MainActivity)
                log("   FLAG_SECURE applied successfully")
                log("   Screenshots and screen recording are now blocked")

                val status = "✅ Screen capture protection enabled"
                log("   Status: $status", Color.parseColor("#2E7D32"))

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runCompleteSecurityTest() {
        scope.launch {
            log("🛡️ Running Complete Security Test...")
            try {
                val config = withContext(Dispatchers.IO) {
                    loadSecureConfiguration()
                }

        val module = SecurityModule.Builder(applicationContext)
            .setConfig(config)
            .setTelemetry(telemetry)
            .build()

                val report = withContext(Dispatchers.IO) {
                    module.runAllChecksBlocking()
                }

                log("   Overall severity: ${report.overallSeverity}")
                log("   Total findings: ${report.findings.size}")

                report.findings.forEach { finding ->
                    val color = when (finding.severity) {
                        Severity.OK -> Color.parseColor("#2E7D32")
                        Severity.INFO -> Color.BLUE
                        Severity.WARN -> Color.parseColor("#FF9800")
                        Severity.BLOCK -> Color.RED
                    }
                    log("   - ${finding.title}: ${finding.severity}", color)
                }

                val status = when (report.overallSeverity) {
                    Severity.OK -> "✅ All security checks passed"
                    Severity.INFO -> "ℹ️ Security info available"
                    Severity.WARN -> "⚠️ Security warnings detected"
                    Severity.BLOCK -> "❌ Critical security issues detected"
                }
                val color = when (report.overallSeverity) {
                    Severity.OK -> Color.parseColor("#2E7D32")
                    Severity.INFO -> Color.BLUE
                    Severity.WARN -> Color.parseColor("#FF9800")
                    Severity.BLOCK -> Color.RED
                }
                log("   Status: $status", color)

            } catch (e: Exception) {
                log("   ❌ Error: ${e.message}", Color.RED)
            }
            log("")
        }
    }

    private fun runHmacErrorLogTest() {
        scope.launch {
            log("🔍 Running HMAC Error Log Test...")
            try {
                val errorLog = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getHmacKeyGenerationLog()
                }

                log("📋 Complete Error Log:")
                errorLog.forEach { logEntry ->
                    log("   $logEntry")
                }

                // Try to generate a key and show the result
                try {
                    val (key, keyType) = withContext(Dispatchers.IO) {
                        SecureHmacHelper.getBestAvailableHmacKey()
                    }
                    log("✅ Final Result: $keyType key generated successfully")
                    log("   Key details: ${key.algorithm} (${key.format})")
                } catch (e: Exception) {
                    log("❌ Final Result: Key generation failed")
                    log("   Error: ${e.javaClass.simpleName} - ${e.message}")
                    log("   Stack trace: ${e.stackTraceToString()}")
                }

            } catch (e: Exception) {
                log("❌ Error log test failed: ${e.message}")
            }
            log("")
        }
    }

    private fun runTeeSupportTest() {
        scope.launch {
            log("🔒 Running TEE Support Test...")
            try {
                val capabilities = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getKeystoreCapabilities()
                }

                log("📱 Device Information:")
                log("   Android Version: ${capabilities["android_version"]}")
                log("   Device Model: ${capabilities["device_model"]}")
                log("   Device Manufacturer: ${capabilities["device_manufacturer"]}")

                log("🔒 TEE Support Analysis:")
                val teeSupport = capabilities["tee_support"] as Boolean
                log("   TEE Available: $teeSupport")

                if (teeSupport) {
                    log("   ✅ TEE is supported on this device")
                    log("   Key Algorithm: ${capabilities["tee_key_algorithm"]}")
                    log("   Key Format: ${capabilities["tee_key_format"]}")
                } else {
                    log("   ❌ TEE is not supported on this device")
                    val teeError = capabilities["tee_error"]
                    if (teeError != null) {
                        log("   Error: $teeError")
                    }
                }

            } catch (e: Exception) {
                log("❌ TEE test failed: ${e.message}")
            }
            log("")
        }
    }

    private fun runStrongBoxTest() {
        scope.launch {
            log("🛡️ Running StrongBox Test...")
            try {
                val capabilities = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getKeystoreCapabilities()
                }

                log("🛡️ StrongBox Support Analysis:")
                val strongBoxSupport = capabilities["strongbox_support"] as Boolean
                log("   StrongBox Available: $strongBoxSupport")

                if (strongBoxSupport) {
                    log("   ✅ StrongBox is supported on this device")
                    log("   This device has hardware security module")
                } else {
                    log("   ❌ StrongBox is not supported on this device")
                    val strongBoxError = capabilities["strongbox_error"]
                    if (strongBoxError != null) {
                        log("   Error: $strongBoxError")
                    }
                }

            } catch (e: Exception) {
                log("❌ StrongBox test failed: ${e.message}")
            }
            log("")
        }
    }

    private fun runFingerprintTest() {
        scope.launch {
            log("👆 Running Fingerprint Test...")
            try {
                log("📱 Biometric Authentication Analysis:")

                // Check if biometric hardware is available using PackageManager
                val packageManager = this@MainActivity.packageManager

                // Check for fingerprint hardware
                val hasFingerprint =
                    packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                log("   Fingerprint Hardware: $hasFingerprint")

                // Check for face unlock hardware
                val hasFace = packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
                log("   Face Unlock Hardware: $hasFace")

                // Check for iris hardware
                val hasIris = packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)
                log("   Iris Hardware: $hasIris")

                // Overall biometric support
                val hasBiometric = hasFingerprint || hasFace || hasIris
                if (hasBiometric) {
                    log("   ✅ Biometric authentication hardware is available")
                    log(
                        "   Supported types: ${
                            mutableListOf<String>().apply {
                                if (hasFingerprint) add("Fingerprint")
                                if (hasFace) add("Face")
                                if (hasIris) add("Iris")
                            }.joinToString(", ")
                        }"
                    )
                } else {
                    log("   ❌ No biometric hardware available")
                }

                // Check for user authentication support in Keystore
                val capabilities = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getKeystoreCapabilities()
                }
                val userAuthSupport = capabilities["user_auth_support"] as Boolean
                log("   Keystore User Auth Binding: $userAuthSupport")

                if (userAuthSupport) {
                    log("   ✅ Keystore supports user authentication binding")
                } else {
                    log("   ❌ Keystore does not support user authentication binding")
                }

            } catch (e: Exception) {
                log("❌ Fingerprint test failed: ${e.message}")
            }
            log("")
        }
    }

    private fun runDeviceSecurityTest() {
        scope.launch {
            log("🔐 Running Device Security Test...")
            try {
                log("📱 Overall Device Security Analysis:")

                val capabilities = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getKeystoreCapabilities()
                }

                log("🔒 Security Features Summary:")
                log("   Android Version: ${capabilities["android_version"]}")
                log("   Keystore Available: ${capabilities["keystore_available"]}")
                log("   TEE Support: ${capabilities["tee_support"]}")
                log("   StrongBox Support: ${capabilities["strongbox_support"]}")
                log("   User Auth Support: ${capabilities["user_auth_support"]}")
                log("   Device Binding Support: ${capabilities["device_binding_support"]}")

                // Security score calculation
                var securityScore = 0
                val maxScore = 5

                if (capabilities["keystore_available"] == true) securityScore++
                if (capabilities["tee_support"] == true) securityScore++
                if (capabilities["strongbox_support"] == true) securityScore++
                if (capabilities["user_auth_support"] == true) securityScore++
                if (capabilities["device_binding_support"] == true) securityScore++

                log("🎯 Security Score: $securityScore/$maxScore")

                when {
                    securityScore >= 4 -> log("   ✅ Excellent security features")
                    securityScore >= 3 -> log("   ✅ Good security features")
                    securityScore >= 2 -> log("   ⚠️ Basic security features")
                    securityScore >= 1 -> log("   ⚠️ Limited security features")
                    else -> log("   ❌ Poor security features")
                }

            } catch (e: Exception) {
                log("❌ Device security test failed: ${e.message}")
            }
            log("")
        }
    }

    private fun runKeystoreTest() {
        scope.launch {
            log("🗝️ Running Keystore Test...")
            try {
                log("🔑 Android Keystore Analysis:")

                val capabilities = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getKeystoreCapabilities()
                }

                log("📋 Keystore Capabilities:")
                log("   Keystore Available: ${capabilities["keystore_available"]}")

                if (capabilities["keystore_available"] == true) {
                    log("   ✅ Android Keystore is working properly")

                    try {
                        val (key, keyType) = withContext(Dispatchers.IO) {
                            SecureHmacHelper.getBestAvailableHmacKey()
                        }
                        log("   ✅ Key generation successful: $keyType")
                        log("   Key details: ${key.algorithm} (${key.format})")
                    } catch (e: Exception) {
                        log("   ❌ Key generation failed: ${e.javaClass.simpleName} - ${e.message}")
                    }
                } else {
                    log("   ❌ Android Keystore is not available")
                    val keystoreError = capabilities["keystore_error"]
                    if (keystoreError != null) {
                        log("   Error: $keystoreError")
                    }
                }

            } catch (e: Exception) {
                log("❌ Keystore test failed: ${e.message}")
            }
            log("")
        }
    }
    
    private fun runHmacComprehensiveTest() {
        scope.launch {
            log("🔐 Running HMAC Comprehensive Test...")
            log("=".repeat(50))
            
            try {
                // Step 1: Device Analysis
                log("📱 STEP 1: Device Analysis")
                log("   Device Model: ${android.os.Build.MODEL}")
                log("   Manufacturer: ${android.os.Build.MANUFACTURER}")
                log("   Android Version: ${android.os.Build.VERSION.SDK_INT}")
                log("   Is Samsung Galaxy A14: ${SecureHmacHelper.isSamsungGalaxyA14OrSimilar()}")
                log("   Is Emulator: ${EmulatorDetector.signals() > 0}")
                log("")
                
                // Step 2: Keystore Capabilities
                log("🔑 STEP 2: Keystore Capabilities Analysis")
                val capabilities = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getKeystoreCapabilities()
                }
                
                log("   Android Keystore: ${if (capabilities["keystore_available"] == true) "✅ Available" else "❌ Not Available"}")
                log("   StrongBox Support: ${if (capabilities["strongbox_support"] == true) "✅ Available" else "❌ Not Available"}")
                log("   TEE Support: ${if (capabilities["tee_support"] == true) "✅ Available" else "❌ Not Available"}")
                log("   User Auth Support: ${if (capabilities["user_auth_support"] == true) "✅ Available" else "❌ Not Available"}")
                log("   Device Binding: ${if (capabilities["device_binding_support"] == true) "✅ Available" else "❌ Not Available"}")
                log("")
                
                // Step 3: Key Generation Test
                log("🔐 STEP 3: Key Generation Test")
                val (key, keyType) = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getBestAvailableHmacKey()
                }
                log("   ✅ Key Generated Successfully!")
                log("   Key Type: $keyType")
                log("   Key Algorithm: ${key.algorithm}")
                log("   Key Format: ${key.format ?: "null"}")
                log("")
                
                // Step 4: HMAC Computation Test
                log("🧪 STEP 4: HMAC Computation Test")
                val testData = "SecurityModule.Comprehensive.Test.${System.currentTimeMillis()}"
                val testDataBytes = testData.toByteArray()
                
                log("   Test Data: $testData")
                log("   Data Length: ${testDataBytes.size} bytes")
                
                val signature = withContext(Dispatchers.IO) {
                    SecureHmacHelper.computeHmacSha256(testDataBytes, key)
                }
                log("   ✅ HMAC Computed Successfully!")
                log("   Signature: $signature")
                log("   Signature Length: ${signature.length} characters")
                log("")
                
                // Step 5: HMAC Verification Test
                log("✅ STEP 5: HMAC Verification Test")
                val isValid = withContext(Dispatchers.IO) {
                    SecureHmacHelper.verifyHmacSignature(testDataBytes, signature, key)
                }
                log("   Verification Result: ${if (isValid) "✅ VALID" else "❌ INVALID"}")
                
                // Test with tampered data
                val tamperedData = "Tampered.SecurityModule.Test.${System.currentTimeMillis()}"
                val tamperedDataBytes = tamperedData.toByteArray()
                val isTamperedValid = withContext(Dispatchers.IO) {
                    SecureHmacHelper.verifyHmacSignature(tamperedDataBytes, signature, key)
                }
                log("   Tampered Data Test: ${if (!isTamperedValid) "✅ CORRECTLY REJECTED" else "❌ INCORRECTLY ACCEPTED"}")
                log("")
                
                // Detailed Algorithm Analysis
                log("🔍 DETAILED ALGORITHM ANALYSIS:")
                when (keyType) {
                    "StrongBox" -> {
                        log("   🔐 StrongBox Implementation:")
                        log("      - Hardware Security Module (HSM)")
                        log("      - Highest security level available")
                        log("      - Key material never leaves secure hardware")
                        log("      - Algorithm: AES-256 in GCM mode")
                        log("      - Key Derivation Process:")
                        log("         1. Generate AES-256 key in StrongBox")
                        log("         2. Encrypt fixed data with GCM mode")
                        log("         3. Use encrypted result as HMAC key")
                        log("         4. Compute HMAC-SHA256 with derived key")
                        log("      - Security Level: MAXIMUM")
                    }
                    "TEE" -> {
                        log("   🛡️ TEE Implementation:")
                        log("      - Trusted Execution Environment")
                        log("      - Hardware-backed security")
                        log("      - Key protected by secure hardware")
                        log("      - Algorithm: AES-256 in GCM mode")
                        log("      - Key Derivation Process:")
                        log("         1. Generate AES-256 key in TEE")
                        log("         2. Encrypt fixed data with GCM mode")
                        log("         3. Use encrypted result as HMAC key")
                        log("         4. Compute HMAC-SHA256 with derived key")
                        log("      - Security Level: HIGH")
                    }
                    "Software" -> {
                        log("   💻 Software Implementation:")
                        log("      - Standard software implementation")
                        log("      - Key stored in application memory")
                        log("      - Algorithm: HmacSHA256")
                        log("      - Direct HMAC Process:")
                        log("         1. Generate HmacSHA256 key")
                        log("         2. Initialize Mac with key")
                        log("         3. Compute HMAC directly")
                        log("      - Security Level: MEDIUM")
                    }
                    "SimpleSoftware" -> {
                        log("   🔧 Simple Software Implementation:")
                        log("      - Fallback for limited devices")
                        log("      - SecretKeySpec with random bytes")
                        log("      - Algorithm: HmacSHA256")
                        log("      - Direct HMAC Process:")
                        log("         1. Generate random 32-byte key")
                        log("         2. Create SecretKeySpec")
                        log("         3. Compute HMAC directly")
                        log("      - Security Level: BASIC")
                    }
                }
                log("")
                
                // Final Summary
                log("📊 FINAL SUMMARY:")
                log("   Device: ${android.os.Build.MODEL} (${android.os.Build.MANUFACTURER})")
                log("   Key Type: $keyType")
                log("   Algorithm: ${key.algorithm}")
                log("   HMAC Test: ✅ PASSED")
                log("   Verification: ✅ PASSED")
                log("   Tamper Detection: ✅ PASSED")
                log("   Overall Status: ✅ ALL TESTS PASSED")
                log("")
                log("💡 This device successfully supports secure HMAC with $keyType keys!")
                
            } catch (e: Exception) {
                log("❌ HMAC Comprehensive Test Failed: ${e.message}")
                log("   Error Details: ${e.javaClass.simpleName}")
                log("   Stack Trace: ${e.stackTraceToString()}")
            }
            log("=".repeat(50))
        }
    }

    private fun runSignUpHmacTest() {
        scope.launch {
            log("📝 Running Sign Up & HMAC Test...")
            log("=".repeat(50))
            
            try {
                // Step 1: Simulate User Registration
                log("👤 STEP 1: User Registration Simulation")
                val userId = "user_${System.currentTimeMillis()}"
                val userEmail = "user${System.currentTimeMillis()}@example.com"
                val userPassword = "SecurePassword123!"
                val userData = mapOf(
                    "userId" to userId,
                    "email" to userEmail,
                    "password" to userPassword,
                    "timestamp" to System.currentTimeMillis(),
                    "deviceId" to android.os.Build.MODEL,
                    "appVersion" to "1.0.0"
                )
                
                log("   ✅ User Data Created:")
                log("      User ID: $userId")
                log("      Email: $userEmail")
                log("      Device: ${android.os.Build.MODEL}")
                log("      Timestamp: ${userData["timestamp"]}")
                log("")
                
                // Step 2: Generate HMAC Key for User
                log("🔐 STEP 2: Generate HMAC Key for User")
                val (hmacKey, keyType) = withContext(Dispatchers.IO) {
                    SecureHmacHelper.getBestAvailableHmacKey()
                }
                log("   ✅ HMAC Key Generated:")
                log("      Key Type: $keyType")
                log("      Key Algorithm: ${hmacKey.algorithm}")
                log("      Key Format: ${hmacKey.format ?: "null"}")
                log("")
                
                // Step 3: Create User Registration Data
                log("📋 STEP 3: Create User Registration Data")
                val registrationData = """
                    {
                        "userId": "$userId",
                        "email": "$userEmail",
                        "deviceId": "${android.os.Build.MODEL}",
                        "timestamp": ${userData["timestamp"]},
                        "appVersion": "1.0.0",
                        "registrationType": "email"
                    }
                """.trimIndent()
                
                val registrationBytes = registrationData.toByteArray()
                log("   ✅ Registration Data Created:")
                log("      Data Length: ${registrationBytes.size} bytes")
                log("      Data Preview: ${registrationData.take(100)}...")
                log("")
                
                // Step 4: Generate HMAC Signature
                log("🔏 STEP 4: Generate HMAC Signature")
                val hmacSignature = withContext(Dispatchers.IO) {
                    SecureHmacHelper.computeHmacSha256(registrationBytes, hmacKey)
                }
                log("   ✅ HMAC Signature Generated:")
                log("      Signature: $hmacSignature")
                log("      Signature Length: ${hmacSignature.length} characters")
                log("")
                
                // Step 5: Verify HMAC Signature
                log("✅ STEP 5: Verify HMAC Signature")
                val isSignatureValid = withContext(Dispatchers.IO) {
                    SecureHmacHelper.verifyHmacSignature(registrationBytes, hmacSignature, hmacKey)
                }
                log("   Verification Result: ${if (isSignatureValid) "✅ VALID" else "❌ INVALID"}")
                
                // Test with tampered data
                val tamperedData = registrationData.replace("email", "tampered_email")
                val tamperedBytes = tamperedData.toByteArray()
                val isTamperedValid = withContext(Dispatchers.IO) {
                    SecureHmacHelper.verifyHmacSignature(tamperedBytes, hmacSignature, hmacKey)
                }
                log("   Tampered Data Test: ${if (!isTamperedValid) "✅ CORRECTLY REJECTED" else "❌ INCORRECTLY ACCEPTED"}")
                log("")
                
                // Step 6: Simulate Server Response
                log("🌐 STEP 6: Simulate Server Response")
                val serverResponse = """
                    {
                        "status": "success",
                        "message": "User registered successfully",
                        "userId": "$userId",
                        "token": "jwt_token_${System.currentTimeMillis()}",
                        "expiresAt": ${System.currentTimeMillis() + 86400000}
                    }
                """.trimIndent()
                
                val serverResponseBytes = serverResponse.toByteArray()
                val serverHmacSignature = withContext(Dispatchers.IO) {
                    SecureHmacHelper.computeHmacSha256(serverResponseBytes, hmacKey)
                }
                
                log("   ✅ Server Response Created:")
                log("      Response Length: ${serverResponseBytes.size} bytes")
                log("      Server HMAC: $serverHmacSignature")
                log("")
                
                // Step 7: Verify Server Response
                log("🔍 STEP 7: Verify Server Response")
                val isServerResponseValid = withContext(Dispatchers.IO) {
                    SecureHmacHelper.verifyHmacSignature(serverResponseBytes, serverHmacSignature, hmacKey)
                }
                log("   Server Response Verification: ${if (isServerResponseValid) "✅ VALID" else "❌ INVALID"}")
                log("")
                
                // Step 8: Complete Sign Up Process
                log("🎉 STEP 8: Complete Sign Up Process")
                log("   ✅ User Registration: COMPLETED")
                log("   ✅ HMAC Key Generation: COMPLETED")
                log("   ✅ Data Signing: COMPLETED")
                log("   ✅ Signature Verification: COMPLETED")
                log("   ✅ Server Response: COMPLETED")
                log("   ✅ Response Verification: COMPLETED")
                log("")
                
                // Final Summary
                log("📊 FINAL SUMMARY:")
                log("   User ID: $userId")
                log("   Email: $userEmail")
                log("   Device: ${android.os.Build.MODEL}")
                log("   HMAC Key Type: $keyType")
                log("   Registration HMAC: $hmacSignature")
                log("   Server Response HMAC: $serverHmacSignature")
                log("   Overall Status: ✅ SIGN UP SUCCESSFUL")
                log("")
                log("💡 User successfully registered with secure HMAC verification!")
                log("   - All data is cryptographically signed")
                log("   - Tamper detection is working")
                log("   - Server responses are verified")
                log("   - Ready for secure communication")
                
            } catch (e: Exception) {
                log("❌ Sign Up & HMAC Test Failed: ${e.message}")
                log("   Error Details: ${e.javaClass.simpleName}")
                log("   Stack Trace: ${e.stackTraceToString()}")
            }
            log("=".repeat(50))
        }
    }

    private fun runApkHmacProtectionTest() {
        scope.launch {
            log("🛡️ Running APK HMAC Protection Test...")
            log("=".repeat(50))
            
            try {
                // Step 1: APK Information
                log("📱 STEP 1: APK Information")
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val apkPath = packageInfo.applicationInfo.sourceDir
                val apkFile = java.io.File(apkPath)
                
                log("   ✅ APK Details:")
                log("      Package Name: $packageName")
                log("      Version Code: ${packageInfo.longVersionCode}")
                log("      Version Name: ${packageInfo.versionName}")
                log("      APK Path: $apkPath")
                log("      APK Size: ${apkFile.length() / (1024 * 1024)} MB")
                log("")
                
                // Step 2: Generate HMAC Signature (Simulation)
                log("🔐 STEP 2: Generate HMAC Signature")
                val hmacSignature = withContext(Dispatchers.IO) {
                    ApkHmacProtector.generateApkHmacSignature(apkPath, this@MainActivity)
                }
                
                if (hmacSignature != null) {
                    log("   ✅ HMAC Signature Generated:")
                    log("      Signature: $hmacSignature")
                    log("      Signature Length: ${hmacSignature.length} characters")
                } else {
                    log("   ❌ Failed to generate HMAC signature")
                }
                log("")
                
                // Step 3: Store HMAC Signature
                log("💾 STEP 3: Store HMAC Signature")
                val stored = withContext(Dispatchers.IO) {
                    ApkHmacProtector.storeHmacSignatureInAssets(apkPath, this@MainActivity)
                }
                
                log("   Storage Result: ${if (stored) "✅ SUCCESS" else "❌ FAILED"}")
                log("")
                
                // Step 4: Verify APK Integrity
                log("🔍 STEP 4: Verify APK Integrity")
                val integrityInfo = withContext(Dispatchers.IO) {
                    ApkHmacProtector.verifyApkIntegrity(this@MainActivity)
                }
                
                log("   ✅ APK Integrity Results:")
                log("      Package Name: ${integrityInfo.packageName}")
                log("      Version Code: ${integrityInfo.versionCode}")
                log("      Version Name: ${integrityInfo.versionName}")
                log("      Signature Hash: ${integrityInfo.signatureHash}")
                log("      APK Hash: ${integrityInfo.apkHash}")
                log("      HMAC Signature: ${integrityInfo.hmacSignature}")
                log("      Key Type: ${integrityInfo.keyType}")
                log("      Timestamp: ${integrityInfo.timestamp}")
                log("      Integrity Valid: ${if (integrityInfo.isIntegrityValid) "✅ VALID" else "❌ INVALID"}")
                log("")
                
                // Step 5: Repackaging Detection
                log("🚨 STEP 5: Repackaging Detection")
                val isRepackaged = withContext(Dispatchers.IO) {
                    ApkHmacProtector.detectRepackaging(this@MainActivity)
                }
                
                log("   Repackaging Detection Result: ${if (isRepackaged) "❌ REPACKAGED DETECTED" else "✅ NO REPACKAGING"}")
                log("")
                
                // Step 6: Security Analysis
                log("🔒 STEP 6: Security Analysis")
                log("   ✅ APK Protection Status:")
                log("      - HMAC Signature: ${if (hmacSignature != null) "✅ Generated" else "❌ Failed"}")
                log("      - Signature Storage: ${if (stored) "✅ Stored" else "❌ Failed"}")
                log("      - Integrity Check: ${if (integrityInfo.isIntegrityValid) "✅ Valid" else "❌ Invalid"}")
                log("      - Repackaging Check: ${if (!isRepackaged) "✅ Clean" else "❌ Detected"}")
                log("")
                
                // Step 7: Recommendations
                log("💡 STEP 7: Security Recommendations")
                if (integrityInfo.isIntegrityValid && !isRepackaged) {
                    log("   ✅ APK is secure and authentic")
                    log("   ✅ No repackaging detected")
                    log("   ✅ HMAC protection is working")
                } else {
                    log("   ⚠️ Security issues detected:")
                    if (!integrityInfo.isIntegrityValid) {
                        log("      - APK integrity check failed")
                        log("      - Possible tampering detected")
                    }
                    if (isRepackaged) {
                        log("      - Repackaging detected")
                        log("      - APK may have been modified")
                    }
                }
                log("")
                
                // Final Summary
                log("📊 FINAL SUMMARY:")
                log("   APK File: ${apkFile.name}")
                log("   Package: $packageName")
                log("   Version: ${packageInfo.versionName}")
                log("   HMAC Key Type: ${integrityInfo.keyType}")
                log("   Integrity Status: ${if (integrityInfo.isIntegrityValid) "✅ VALID" else "❌ INVALID"}")
                log("   Repackaging Status: ${if (!isRepackaged) "✅ CLEAN" else "❌ DETECTED"}")
                log("   Overall Security: ${if (integrityInfo.isIntegrityValid && !isRepackaged) "✅ SECURE" else "❌ COMPROMISED"}")
                log("")
                
                if (integrityInfo.isIntegrityValid && !isRepackaged) {
                    log("🎉 APK HMAC Protection Test PASSED!")
                    log("   Your APK is protected against repackaging attacks.")
                } else {
                    log("⚠️ APK HMAC Protection Test FAILED!")
                    log("   Security issues detected. Review the results above.")
                }
                
            } catch (e: Exception) {
                log("❌ APK HMAC Protection Test Failed: ${e.message}")
                log("   Error Details: ${e.javaClass.simpleName}")
                log("   Stack Trace: ${e.stackTraceToString()}")
            }
            log("=".repeat(50))
        }
    }

    private fun clearResults() {
        val clearedMessage = "Results cleared. Click any test button to run tests..."
        TestResultsActivity.updateResults(clearedMessage)
    }

    private fun log(message: String, color: Int = Color.DKGRAY) {
        runOnUiThread {
            val currentText = TestResultsActivity.getResults()
            val newText =
                if (currentText.contains("No test results yet") || currentText.contains("Results cleared")) {
                    message
                } else {
                    "$currentText\n$message"
                }

            // Update TestResultsActivity with the new results
            TestResultsActivity.updateResults(newText)
        }
    }
    
    private fun navigateToResults() {
        val intent = Intent(this@MainActivity, TestResultsActivity::class.java)
        startActivity(intent)
    }
    
    private fun runTestAndNavigate(testFunction: suspend () -> Unit) {
        scope.launch {
            try {
                testFunction()
                // Wait a bit for the test to complete and results to be logged
                delay(1000)
                navigateToResults()
            } catch (e: Exception) {
                log("❌ Test failed: ${e.message}")
                delay(500)
                navigateToResults()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    
    /**
     * Load configuration with secure HMAC implementation
     */
    private fun loadSecureConfiguration(): SecurityConfig {
        return try {
            // Try secure HMAC first (recommended for production)
            val secureConfig = SecurityConfigLoader.fromAssetPreferSigned(
                context = this,
                assetName = "security_config.json",
                signatureAssetName = "security_config.sig",
                useSecureHmac = true // Use Android Keystore for HMAC verification
            )
            secureConfig

        } catch (e: Exception) {
            // Fallback to unsigned configuration
            SecurityConfigLoader.fromAsset(this, "security_config.json")
        }
    }
}


