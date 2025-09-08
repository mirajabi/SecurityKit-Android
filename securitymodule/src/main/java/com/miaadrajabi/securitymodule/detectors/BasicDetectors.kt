package com.miaadrajabi.securitymodule.detectors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.NetworkInterface

object RootDetector {
    private val suspiciousPaths = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        "/system/etc/init.d/99SuperSUDaemon",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su-backup",
        "/sbin/magisk",
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/cache/.magisk",
        "/system/bin/magisk"
    )

    private fun getProp(name: String): String? {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("getprop", name))
            BufferedReader(InputStreamReader(proc.inputStream)).use { it.readLine() }
        } catch (t: Throwable) { null }
    }

    fun signals(context: Context? = null): Int {
        var count = 0

        // a) su/magisk files
        if (suspiciousPaths.any { File(it).exists() }) count++

        // b) test-keys in build tags
        if (Build.TAGS?.contains("test-keys") == true) count++

        // c) system properties typical for rooted builds
        val roDebug = getProp("ro.debuggable"); if (roDebug == "1") count++
        val roSecure = getProp("ro.secure"); if (roSecure == "0") count++

        // d) which su / su -c id
        try {
            val which = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val out = BufferedReader(InputStreamReader(which.inputStream)).use { it.readLine() }
            if (!out.isNullOrEmpty()) count++
        } catch (_: Throwable) { }
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val line = reader.readLine()
            if (line != null && line.contains("uid=0")) count++
            proc.destroy()
        } catch (_: Throwable) { }

        // e) BusyBox / Mount flags
        try { if (BusyBoxDetector.exists()) count++ } catch (_: Throwable) { }
        try { if (MountFlagsDetector.hasRwOnSystem()) count++ } catch (_: Throwable) { }

        // f) Known root packages
        if (context != null) {
            try {
                val pm = context.packageManager
                val pkgs = listOf(
                    "com.topjohnwu.magisk",
                    "eu.chainfire.supersu",
                    "com.koushikdutta.superuser",
                    "com.noshufou.android.su"
                )
                val found = pkgs.any { pkg ->
                    try { pm.getPackageInfo(pkg, 0); true } catch (_: Throwable) { false }
                }
                if (found) count++
            } catch (_: Throwable) { }
        }

        return count
    }
}

object EmulatorDetector {
    private fun getProp(name: String): String? {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("getprop", name))
            BufferedReader(InputStreamReader(proc.inputStream)).use { it.readLine() }
        } catch (t: Throwable) { null }
    }

    fun signals(context: Context? = null): Int {
        var count = 0
        // Build properties
        if (Build.FINGERPRINT?.startsWith("generic") == true || Build.FINGERPRINT?.contains("vbox") == true) count++
        if (Build.MODEL?.contains("Emulator", true) == true || Build.MODEL?.contains("Android SDK built for x86", true) == true) count++
        if ((Build.BRAND?.startsWith("generic") == true && Build.DEVICE?.startsWith("generic") == true) || Build.PRODUCT?.contains("sdk") == true) count++
        if (Build.HARDWARE?.contains("goldfish") == true || Build.HARDWARE?.contains("ranchu") == true || Build.HARDWARE?.contains("vbox") == true || Build.HARDWARE?.contains("vbox86") == true) count++
        if (Build.MANUFACTURER?.contains("Genymotion", true) == true || Build.MODEL?.contains("Genymotion", true) == true) count++

        // Files/Devices
        if (File("/dev/socket/qemud").exists() || File("/dev/qemu_pipe").exists()) count++
        if (File("/system/lib/libc_malloc_debug_qemu.so").exists() || File("/sys/qemu_trace").exists() || File("/init.goldfish.rc").exists()) count++
        if (File("/dev/vboxguest").exists() || File("/dev/vboxuser").exists()) count++

        // System properties
        val qemu = getProp("ro.kernel.qemu"); if (qemu == "1") count++
        val hardware = getProp("ro.hardware"); if (hardware?.contains("goldfish") == true || hardware?.contains("ranchu") == true || hardware?.contains("vbox") == true) count++

        // Emulator default IP
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                for (ni in interfaces) {
                    val addrs = ni.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        val host = addr.hostAddress ?: ""
                        if (host == "10.0.2.15") { count++; break }
                    }
                }
            }
        } catch (_: Throwable) { }

        // Sensors profile (very few)
        try {
            if (context != null) {
                val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                val sensors = sm?.getSensorList(Sensor.TYPE_ALL)?.size ?: 0
                if (sensors <= 5) count++
            }
        } catch (_: Throwable) { }

        return count
    }
}

object DebuggerDetector {
    fun isDebuggerAttached(): Boolean = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
}

object UsbDebugDetector {
    fun isUsbDebugEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (t: Throwable) {
            false
        }
    }
}

object VpnDetector {
    fun isVpnActive(): Boolean {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            interfaces.asSequence().any { it.isUp && (it.name == "tun0" || it.name == "ppp0" || it.name.startsWith("tun")) }
        } catch (t: Throwable) {
            false
        }
    }
}


