package com.miaadrajabi.securitysample

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.miaadrajabi.securitymodule.SecurityReport
import com.miaadrajabi.securitymodule.config.SecurityConfig
import com.miaadrajabi.securitymodule.detectors.BusyBoxDetector
import com.miaadrajabi.securitymodule.detectors.DebuggerDetector
import com.miaadrajabi.securitymodule.detectors.DeveloperOptionsDetector
import com.miaadrajabi.securitymodule.detectors.EmulatorDetector
import com.miaadrajabi.securitymodule.detectors.HookingDetector
import com.miaadrajabi.securitymodule.detectors.MitmDetector
import com.miaadrajabi.securitymodule.detectors.MountFlagsDetector
import com.miaadrajabi.securitymodule.detectors.ProxyDetector
import com.miaadrajabi.securitymodule.detectors.QemuDetector
import com.miaadrajabi.securitymodule.detectors.RepackagingDetector
import com.miaadrajabi.securitymodule.detectors.RootDetector
import com.miaadrajabi.securitymodule.detectors.SelinuxDetector
import com.miaadrajabi.securitymodule.detectors.SignatureVerifier
import com.miaadrajabi.securitymodule.detectors.TracerPidDetector
import com.miaadrajabi.securitymodule.detectors.UsbDebugDetector
import com.miaadrajabi.securitymodule.detectors.VpnDetector
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector
import com.miaadrajabi.securitymodule.policy.PolicyEngine

fun Activity.renderReport(report: SecurityReport): LinearLayout {
    val root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    root.setBackgroundColor(Color.WHITE)
    val header = TextView(this)
    header.textSize = 18f
    header.setTextColor(Color.BLACK)
    header.text = "Security Report: ${report.overallSeverity}"
    header.setPadding(32, 32, 32, 16)
    root.addView(header)

    if (report.findings.isEmpty()) {
        val ok = TextView(this)
        ok.setTextColor(Color.parseColor("#2E7D32"))
        ok.text = "No issues detected"
        ok.setPadding(32, 16, 32, 32)
        root.addView(ok)
    } else {
        for (f in report.findings) {
            val item = TextView(this)
            item.setTextColor(Color.DKGRAY)
            item.text = "- ${f.id}: ${f.title} (${f.severity})"
            item.setPadding(32, 8, 32, 8)
            root.addView(item)
        }
    }
    val lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    root.layoutParams = lp
    root.gravity = Gravity.TOP
    return root
}

fun Activity.renderDetailedReport(report: SecurityReport, config: SecurityConfig): LinearLayout {
    val policy = PolicyEngine(config)

    val root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    root.setBackgroundColor(Color.WHITE)
    val header = TextView(this)
    header.textSize = 18f
    header.setTextColor(Color.BLACK)
    header.text = "Security Report (Detailed): ${report.overallSeverity}"
    header.setPadding(32, 32, 32, 16)
    root.addView(header)

    fun addTitle(title: String) {
        val tv = TextView(this)
        tv.text = title
        tv.textSize = 16f
        tv.setTextColor(Color.BLACK)
        tv.setPadding(32, 24, 32, 8)
        root.addView(tv)
    }
    fun addLine(text: String, color: Int = Color.DKGRAY) {
        val tv = TextView(this)
        tv.setTextColor(color)
        tv.text = text
        tv.setPadding(48, 6, 32, 6)
        root.addView(tv)
    }

    // App Integrity
    addTitle("App Integrity")
    val expectedPkg = config.appIntegrity.expectedPackageName
    val isRepack = RepackagingDetector.isRepackaged(this, expectedPkg)
    addLine("Repackaged: ${if (isRepack) "DETECTED" else "OK"}", if (isRepack) Color.RED else Color.parseColor("#2E7D32"))

    val expectedSigs = config.appIntegrity.expectedSignatureSha256
    if (config.features.appSignatureVerification && expectedSigs.isNotEmpty()) {
        val actualSigs = SignatureVerifier.currentSigningSha256(this)
        val match = actualSigs.any { a -> expectedSigs.any { it.equals(a, ignoreCase = true) } }
        val color = if (match) Color.parseColor("#2E7D32") else Color.RED
        addLine("Signature match: ${if (match) "OK" else "MISMATCH"}", color)
    } else {
        addLine("Signature check: not configured")
    }

    // Device Integrity
    addTitle("Device Integrity")
    val rootSignals = RootDetector.signals()
    val emulatorSignals = EmulatorDetector.signals() + QemuDetector.indicators()
    val hooking = HookingDetector.suspiciousLoadedLibs()
    val traced = TracerPidDetector.isTraced()
    val busybox = BusyBoxDetector.exists()
    val mountRw = MountFlagsDetector.hasRwOnSystem()
    val selinux = SelinuxDetector.isEnforced()

    addLine("Root signals: $rootSignals", if (rootSignals > 0) Color.RED else Color.parseColor("#2E7D32"))
    addLine("Emulator signals: $emulatorSignals", if (emulatorSignals > 0) Color.RED else Color.parseColor("#2E7D32"))
    addLine("Hooking libs: ${if (hooking > 0) hooking else 0}", if (hooking > 0) Color.RED else Color.parseColor("#2E7D32"))
    addLine("Tracer PID: ${if (traced) "DETECTED" else "OK"}", if (traced) Color.RED else Color.parseColor("#2E7D32"))
    addLine("BusyBox: ${if (busybox) "PRESENT" else "ABSENT"}", if (busybox) Color.RED else Color.parseColor("#2E7D32"))
    addLine("/system RW: ${if (mountRw) "YES" else "NO"}", if (mountRw) Color.RED else Color.parseColor("#2E7D32"))
    addLine("SELinux enforced: ${if (selinux) "YES" else "NO"}", if (selinux) Color.parseColor("#2E7D32") else Color.RED)

    // Environment
    addTitle("Environment")
    val vpn = VpnDetector.isVpnActive()
    val proxy = ProxyDetector.isProxyEnabled(this)
    val usb = UsbDebugDetector.isUsbDebugEnabled(this)
    val dbg = DebuggerDetector.isDebuggerAttached()
    val dev = DeveloperOptionsDetector.isEnabled(this)
    val mitm = MitmDetector.userAddedCertificatesPresent(this)
    addLine("VPN: ${if (vpn) "ACTIVE" else "OFF"}", if (vpn) Color.rgb(230,156,0) else Color.parseColor("#2E7D32"))
    addLine("Proxy: ${if (proxy) "DETECTED" else "NONE"}", if (proxy) Color.rgb(230,156,0) else Color.parseColor("#2E7D32"))
    addLine("USB Debug: ${if (usb) "ON" else "OFF"}", if (usb) Color.rgb(230,156,0) else Color.parseColor("#2E7D32"))
    addLine("Debugger: ${if (dbg) "ATTACHED" else "NO"}", if (dbg) Color.rgb(230,156,0) else Color.parseColor("#2E7D32"))
    addLine("Dev Options: ${if (dev) "ON" else "OFF"}", if (dev) Color.rgb(230,156,0) else Color.parseColor("#2E7D32"))
    addLine("MITM indicators: ${if (mitm) "PRESENT" else "NONE"}", if (mitm) Color.RED else Color.parseColor("#2E7D32"))

    // Policy (what would happen)
    addTitle("Policy decisions")
    addLine("onRoot: ${policy.onRoot(rootSignals).action}")
    addLine("onEmulator: ${policy.onEmulator(emulatorSignals).action}")
    addLine("onDebugger: ${policy.onDebugger(dbg).action}")
    addLine("onUsbDebug: ${policy.onUsbDebug(usb).action}")
    addLine("onVpn: ${policy.onVpn(vpn).action}")
    addLine("onMitm: ${policy.onMitm(mitm).action}")

    // Helper actions
    val secureBtn = Button(this)
    secureBtn.text = "Apply FLAG_SECURE (block screenshots)"
    secureBtn.setOnClickListener {
        ScreenCaptureProtector.applySecureFlag(this)
    }
    secureBtn.setPadding(32, 16, 32, 16)
    root.addView(secureBtn)

    val hmacDemoBtn = Button(this)
    hmacDemoBtn.text = "🔐 Secure HMAC Demo"
    hmacDemoBtn.setOnClickListener {
        startActivity(android.content.Intent(this, SecureHmacDemoActivity::class.java))
    }
    hmacDemoBtn.setPadding(32, 16, 32, 16)
    root.addView(hmacDemoBtn)

    val lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    root.layoutParams = lp
    root.gravity = Gravity.TOP
    return root
}


