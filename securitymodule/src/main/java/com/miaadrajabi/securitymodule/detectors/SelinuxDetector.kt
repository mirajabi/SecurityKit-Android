package com.miaadrajabi.securitymodule.detectors

import java.io.File

object SelinuxDetector {
    fun isEnforced(): Boolean {
        return try {
            val f = File("/sys/fs/selinux/enforce")
            if (!f.exists() || !f.canRead()) return true
            val content = f.readText().trim()
            content == "1"
        } catch (t: Throwable) {
            true
        }
    }
}


