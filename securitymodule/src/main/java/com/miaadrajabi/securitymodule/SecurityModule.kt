package com.miaadrajabi.securitymodule

import android.content.Context
import com.miaadrajabi.securitymodule.config.ConfigIntegrity
import com.miaadrajabi.securitymodule.config.SecurityConfig
import com.miaadrajabi.securitymodule.crypto.KeystoreHelper
import com.miaadrajabi.securitymodule.detectors.AppIntegrityDetector
import com.miaadrajabi.securitymodule.detectors.DebuggerDetector
import com.miaadrajabi.securitymodule.detectors.DeveloperOptionsDetector
import com.miaadrajabi.securitymodule.detectors.HookingDetector
import com.miaadrajabi.securitymodule.detectors.MitmDetector
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector
import com.miaadrajabi.securitymodule.detectors.TracerPidDetector
import com.miaadrajabi.securitymodule.detectors.ProxyDetector
import com.miaadrajabi.securitymodule.detectors.RepackagingDetector
import com.miaadrajabi.securitymodule.detectors.SignatureVerifier
import com.miaadrajabi.securitymodule.detectors.EmulatorDetector
import com.miaadrajabi.securitymodule.detectors.RootDetector
import com.miaadrajabi.securitymodule.detectors.UsbDebugDetector
import com.miaadrajabi.securitymodule.detectors.VpnDetector
import com.miaadrajabi.securitymodule.detectors.BusyBoxDetector
import com.miaadrajabi.securitymodule.detectors.MountFlagsDetector
import com.miaadrajabi.securitymodule.detectors.QemuDetector
import com.miaadrajabi.securitymodule.integrity.PlayIntegrityClient
import com.miaadrajabi.securitymodule.policy.PolicyDecision
import com.miaadrajabi.securitymodule.policy.PolicyEngine
import com.miaadrajabi.securitymodule.policy.PolicyExecutor
import com.miaadrajabi.securitymodule.storage.TamperEvidenceStore
import com.miaadrajabi.securitymodule.telemetry.NoopTelemetry
import com.miaadrajabi.securitymodule.telemetry.TelemetryEvents
import com.miaadrajabi.securitymodule.telemetry.TelemetrySink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecurityModule private constructor(
    private val applicationContext: Context,
    private val config: SecurityConfig,
    private val telemetry: TelemetrySink
) {

    suspend fun runAllChecks(): SecurityReport = withContext(Dispatchers.Default) {
        runAllChecksBlocking()
    }

    fun runAllChecksBlocking(): SecurityReport {
        val findings = mutableListOf<SecurityFinding>()
        val policy = PolicyEngine(config)
        val executor = PolicyExecutor(applicationContext)

        // Model/brand overrides: early allowlist/denylist gates
        val model = android.os.Build.MODEL ?: ""
        val brand = android.os.Build.BRAND ?: ""
        val manufacturer = android.os.Build.MANUFACTURER ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        val device = android.os.Build.DEVICE ?: ""
        val board = android.os.Build.BOARD ?: ""
        
        // Check denied overrides first
        if (config.overrides.deniedModels.contains(model) || 
            config.overrides.deniedBrands.contains(brand)) {
            findings.add(SecurityFinding("override", "Denied model/brand", Severity.BLOCK))
            executor.execute(SecurityConfig.Action.BLOCK)
            return SecurityReport(findings, Severity.BLOCK)
        }
        
        // Check allowed overrides - if device matches, skip all security checks
        val isAllowedDevice = config.overrides.allowedModels.contains(model) ||
                             config.overrides.allowedBrands.contains(brand) ||
                             config.overrides.allowedManufacturers.contains(manufacturer) ||
                             config.overrides.allowedProducts.contains(product) ||
                             config.overrides.allowedDevices.contains(device) ||
                             config.overrides.allowedBoards.contains(board)
        
        if (isAllowedDevice) {
            findings.add(SecurityFinding("override", "Allowed device - bypassing security checks", Severity.OK))
            return SecurityReport(findings, Severity.OK)
        }

        // Advanced features checks
        if (config.features.playIntegrityCheck && config.advanced.playIntegrity.enabled) {
            try {
                val integrityResult = kotlinx.coroutines.runBlocking {
                    PlayIntegrityClient.checkIntegrity(applicationContext, config.advanced.playIntegrity.nonce)
                }
                val playIntegrityFindings = PlayIntegrityClient.toSecurityFindings(integrityResult)
                findings.addAll(playIntegrityFindings)
                
                val hasBlockingFindings = playIntegrityFindings.any { it.severity == Severity.BLOCK }
                if (hasBlockingFindings) {
                    executor.execute(config.policy.onPlayIntegrityFailure)
                }
            } catch (e: Exception) {
                findings.add(SecurityFinding("play_integrity_error", "Play Integrity check failed", Severity.WARN))
            }
        }

        if (config.features.advancedAppIntegrity) {
            try {
                val appIntegrityConfig = AppIntegrityDetector.AppIntegrityConfig(
                    expectedPackageName = config.appIntegrity.expectedPackageName ?: applicationContext.packageName,
                    expectedSigningHashes = config.appIntegrity.expectedSignatureSha256.toSet(),
                    allowedInstallers = config.appIntegrity.allowedInstallers.toSet(),
                    expectedDexChecksums = config.appIntegrity.expectedDexChecksums,
                    expectedSoChecksums = config.appIntegrity.expectedSoChecksums
                )
                val appIntegrityFindings = AppIntegrityDetector.checkAppIntegrity(applicationContext, appIntegrityConfig)
                findings.addAll(appIntegrityFindings)
                
                val hasBlockingFindings = appIntegrityFindings.any { it.severity == Severity.BLOCK }
                if (hasBlockingFindings) {
                    executor.execute(config.policy.onAppIntegrityFailure)
                }
            } catch (e: Exception) {
                findings.add(SecurityFinding("app_integrity_error", "Advanced app integrity check failed", Severity.WARN))
            }
        }

        // Keystore advanced features
        if (config.features.strongBoxKeys) {
            val strongBoxAvailable = KeystoreHelper.isStrongBoxAvailable()
            if (!strongBoxAvailable) {
                findings.add(SecurityFinding("strongbox_unavailable", "StrongBox not available", Severity.WARN))
                executor.execute(config.policy.onStrongBoxUnavailable)
            } else {
                findings.add(SecurityFinding("strongbox_available", "StrongBox available", Severity.OK))
            }
        }

        if (config.features.deviceBinding) {
            try {
                val deviceBindingId = KeystoreHelper.generateDeviceBindingId(applicationContext)
                findings.add(SecurityFinding("device_binding", "Device binding ID generated", Severity.OK, 
                    mapOf("binding_id" to deviceBindingId.take(16) + "...")))
            } catch (e: Exception) {
                findings.add(SecurityFinding("device_binding_error", "Device binding failed", Severity.WARN))
            }
        }

        // Tamper evidence check
        if (config.features.tamperEvidence) {
            try {
                val isConfigValid = kotlinx.coroutines.runBlocking {
                    TamperEvidenceStore.isDataValid(applicationContext, "security_config", "1.0")
                }
                if (!isConfigValid) {
                    findings.add(SecurityFinding("config_tampering", "Configuration tampering detected", Severity.BLOCK))
                    executor.execute(config.policy.onConfigTampering)
                }
            } catch (e: Exception) {
                findings.add(SecurityFinding("tamper_evidence_error", "Tamper evidence check failed", Severity.WARN))
            }
        }

        // Basic security checks
        if (config.features.rootDetection) {
            val count = RootDetector.signals(applicationContext)
            val decision: PolicyDecision = policy.onRoot(count)
            if (decision.action != SecurityConfig.Action.ALLOW) {
                findings.add(SecurityFinding("root", "Root indicators: $count", toSeverity(decision.action)))
                telemetry.onEvent(TelemetryEvents.ROOT_DETECTED, mapOf("count" to count.toString()))
                executor.execute(decision.action)
            }
        }

        if (config.features.emulatorDetection) {
            val count = EmulatorDetector.signals(applicationContext)
            val qemu = QemuDetector.indicators()
            val total = count + qemu
            val decision: PolicyDecision = policy.onEmulator(total)
            if (decision.action != SecurityConfig.Action.ALLOW) {
                findings.add(SecurityFinding("emulator", "Emulator indicators: $total", toSeverity(decision.action)))
                telemetry.onEvent(TelemetryEvents.EMULATOR_DETECTED, mapOf("count" to total.toString()))
                executor.execute(decision.action)
            }
        }

        if (config.features.debuggerDetection) {
            val attached = DebuggerDetector.isDebuggerAttached()
            val decision = policy.onDebugger(attached)
            if (decision.action != SecurityConfig.Action.ALLOW) {
                findings.add(SecurityFinding("debugger", "Debugger attached", toSeverity(decision.action)))
                telemetry.onEvent(TelemetryEvents.DEBUGGER_ATTACHED)
                executor.execute(decision.action)
            }
        }

        if (config.features.usbDebugDetection) {
            val enabled = UsbDebugDetector.isUsbDebugEnabled(applicationContext)
            val decision = policy.onUsbDebug(enabled)
            if (decision.action != SecurityConfig.Action.ALLOW) {
                findings.add(SecurityFinding("usb_debug", "USB debugging enabled", toSeverity(decision.action)))
                telemetry.onEvent(TelemetryEvents.USB_DEBUG_ENABLED)
                executor.execute(decision.action)
            }
        }

        if (config.features.vpnDetection) {
            val active = VpnDetector.isVpnActive()
            val decision = policy.onVpn(active)
            if (decision.action != SecurityConfig.Action.ALLOW) {
                findings.add(SecurityFinding("vpn", "VPN active", toSeverity(decision.action)))
                telemetry.onEvent(TelemetryEvents.VPN_ACTIVE)
                executor.execute(decision.action)
            }
        }

        if (config.features.mitmDetection) {
            val proxy = ProxyDetector.isProxyEnabled(applicationContext)
            if (proxy) {
                findings.add(SecurityFinding("proxy", "Proxy detected", Severity.WARN))
            }
        }

        if (config.features.appSignatureVerification && config.appIntegrity.expectedSignatureSha256.isNotEmpty()) {
            val actual = SignatureVerifier.currentSigningSha256(applicationContext)
            val match = actual.any { a -> config.appIntegrity.expectedSignatureSha256.any { it.equals(a, ignoreCase = true) } }
            if (!match) {
                findings.add(SecurityFinding("signature", "Signature mismatch", Severity.BLOCK))
                executor.execute(SecurityConfig.Action.BLOCK)
            }
        }

        if (config.features.repackagingDetection && config.appIntegrity.expectedPackageName != null) {
            if (RepackagingDetector.isRepackaged(applicationContext, config.appIntegrity.expectedPackageName)) {
                findings.add(SecurityFinding("repackaging", "Unexpected package name", Severity.BLOCK))
                executor.execute(SecurityConfig.Action.BLOCK)
            }
        }

        // Additional security indicators
        if (TracerPidDetector.isTraced()) {
            findings.add(SecurityFinding("tracer", "Process is traced", Severity.WARN))
        }
        if (HookingDetector.suspiciousLoadedLibs() > 0) {
            findings.add(SecurityFinding("hooking", "Suspicious libs loaded", Severity.WARN))
        }
        if (BusyBoxDetector.exists()) {
            findings.add(SecurityFinding("busybox", "BusyBox present", Severity.WARN))
        }
        if (MountFlagsDetector.hasRwOnSystem()) {
            findings.add(SecurityFinding("mount", "RW flags on system/vendor", Severity.WARN))
        }
        if (MitmDetector.userAddedCertificatesPresent(applicationContext)) {
            findings.add(SecurityFinding("mitm", "User-added CA indicators", Severity.WARN))
        }
        if (DeveloperOptionsDetector.isEnabled(applicationContext)) {
            findings.add(SecurityFinding("dev_options", "Developer options enabled", Severity.WARN))
        }

        val overall = findings.maxByOrNull { it.severity.ordinal }?.severity ?: Severity.OK
        telemetry.onEvent(TelemetryEvents.POLICY_DECISION, mapOf("severity" to overall.name))
        return SecurityReport(findings, overall)
    }

    private fun toSeverity(action: SecurityConfig.Action): Severity = when (action) {
        SecurityConfig.Action.ALLOW -> Severity.OK
        SecurityConfig.Action.WARN -> Severity.WARN
        SecurityConfig.Action.DEGRADE -> Severity.WARN
        SecurityConfig.Action.BLOCK -> Severity.BLOCK
        SecurityConfig.Action.TERMINATE -> Severity.BLOCK
    }

    class Builder(private val context: Context) {
        private var config: SecurityConfig = SecurityConfig()
        private var telemetry: TelemetrySink = NoopTelemetry

        fun setConfig(config: SecurityConfig) = apply { this.config = config }
        fun setTelemetry(telemetry: TelemetrySink) = apply { this.telemetry = telemetry }
        fun build(): SecurityModule = SecurityModule(context.applicationContext, config, telemetry)
    }
}

data class SecurityReport(
    val findings: List<SecurityFinding>,
    val overallSeverity: Severity
)

data class SecurityFinding(
    val id: String,
    val title: String,
    val severity: Severity,
    val metadata: Map<String, String> = emptyMap()
)

enum class Severity { OK, INFO, WARN, BLOCK }


