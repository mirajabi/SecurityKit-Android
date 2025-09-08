package com.miaadrajabi.securitymodule.detectors

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object BusyBoxDetector {
    private val paths = arrayOf(
        "/system/xbin/busybox",
        "/system/bin/busybox",
        "/sbin/busybox"
    )

    fun exists(): Boolean = paths.any { File(it).exists() }
}

object MountFlagsDetector {
    fun hasRwOnSystem(): Boolean {
        return try {
            val mounts = File("/proc/mounts")
            if (!mounts.canRead()) return false
            BufferedReader(FileReader(mounts)).use { br ->
                br.lineSequence().any { line ->
                    (line.contains(" /system ") || line.contains(" /vendor ")) &&
                        (line.contains(" rw,") || line.matches(Regex(".* rw .*")))
                }
            }
        } catch (t: Throwable) {
            false
        }
    }
}

object QemuDetector {
    fun indicators(): Int {
        var count = 0
        try {
            val drivers = File("/proc/tty/drivers")
            if (drivers.canRead()) {
                val content = drivers.readText()
                if (content.contains("goldfish") || content.contains("qemu")) count++
            }
        } catch (_: Throwable) { }
        try {
            val cpuinfo = File("/proc/cpuinfo")
            if (cpuinfo.canRead()) {
                val content = cpuinfo.readText()
                if (content.contains("goldfish") || content.contains("qemu")) count++
            }
        } catch (_: Throwable) { }
        return count
    }
}


