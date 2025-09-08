package com.miaadrajabi.securitysample

import android.app.Activity
import android.os.Bundle
import com.miaadrajabi.securitymodule.SecurityModule
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
        val config: SecurityConfig = SecurityConfigLoader.fromAsset(this)
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

        val root = renderDetailedReport(report, config)
        renderCryptoDemo(root)
        setContentView(root)
    }
}


