package com.miaadrajabi.securitysample

import android.app.Activity
import android.os.Bundle
import com.miaadrajabi.securitymodule.SecurityModule
import com.miaadrajabi.securitymodule.Severity
import com.miaadrajabi.securitymodule.config.SecurityConfig
import com.miaadrajabi.securitymodule.config.SecurityConfigLoader
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureMonitor
import com.miaadrajabi.securitymodule.telemetry.TelemetrySink

class MainActivity : Activity() {
    private val telemetry = object : TelemetrySink {
        override fun onEvent(eventId: String, attributes: Map<String, String>) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        SecurityUsageExample.completeUsageExample(this);

        val hmacKey = BuildConfig.CONFIG_HMAC_KEY
        val config: SecurityConfig = SecurityConfigLoader.fromAssetPreferSigned(
            this,
            "security_config.json",
            "security_config.sig",
            if (hmacKey.isEmpty()) null else hmacKey
        )
        val module = SecurityModule.Builder(applicationContext)
            .setConfig(config)
            .setTelemetry(telemetry)
            .build()

        // Apply FLAG_SECURE and start capture monitor if enabled in config
        var captureMonitor: ScreenCaptureMonitor? = null
        if (config.features.screenCaptureProtection) {
            ScreenCaptureProtector.applySecureFlag(this)
            captureMonitor = ScreenCaptureMonitor(this)
            captureMonitor.start { type, uri ->
                // Show white overlay when screenshot/recording detected
                ScreenCaptureProtector.showWhiteOverlay(this@MainActivity)
            }
        }

        val report = module.runAllChecksBlocking()

        // Handle security check results based on severity
        when (report.overallSeverity) {
            Severity.OK -> {
                // All security checks passed - show normal UI
                val root = renderDetailedReport(report, config)
                renderCryptoDemo(root)
                setContentView(root)
            }
            Severity.WARN -> {
                // Security warnings detected - show warnings but allow continuation
                println("⚠️ Security warnings detected:")
                report.findings.forEach { finding ->
                    println("  - ${finding.title}: ${finding.severity}")
                }
                // Show normal UI with warnings
                val root = renderDetailedReport(report, config)
                renderCryptoDemo(root)
                setContentView(root)
            }
            Severity.BLOCK -> {
                // Critical security issues - redirect to warning page and exit
                println("🚫 Critical security issues detected:")
                report.findings.forEach { finding ->
                    println("  - ${finding.title}: ${finding.severity}")
                }
                ReportActivity.start(this, report, config)
                finish()
            }
            Severity.INFO -> {
                // Info level - show normal UI
                val root = renderDetailedReport(report, config)
                renderCryptoDemo(root)
                setContentView(root)
            }
        }
    }
}


