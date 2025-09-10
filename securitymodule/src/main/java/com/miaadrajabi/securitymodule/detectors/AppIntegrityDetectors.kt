package com.miaadrajabi.securitymodule.detectors

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.miaadrajabi.securitymodule.crypto.CryptoUtils

object SignatureVerifier {
    @JvmStatic fun currentSigningSha256(context: Context): List<String> {
        return try {
            val pm = context.packageManager
            val pkgName = context.packageName
            if (Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
                val sigs = info.signingInfo.apkContentsSigners
                sigs.map { bytesToHex(CryptoUtils.sha256(it.toByteArray())) }
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures.map { bytesToHex(CryptoUtils.sha256(it.toByteArray())) }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hex = CharArray(bytes.size * 2)
        val digits = "0123456789abcdef".toCharArray()
        var i = 0
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            hex[i++] = digits[v ushr 4]
            hex[i++] = digits[v and 0x0F]
        }
        return String(hex)
    }
}

object RepackagingDetector {
    fun isRepackaged(context: Context, expectedPackage: String?): Boolean {
        if (expectedPackage.isNullOrEmpty()) return false
        return context.packageName != expectedPackage
    }
}


