package com.miaadrajabi.securitymodule.detectors

import android.content.Context
import android.net.ConnectivityManager
import android.net.Proxy
import android.os.Build

object ProxyDetector {
    fun isProxyEnabled(context: Context): Boolean {
        return try {
            val host = System.getProperty("http.proxyHost") ?: run {
                @Suppress("DEPRECATION")
                Proxy.getHost(context)
            }
            val port = System.getProperty("http.proxyPort") ?: run {
                @Suppress("DEPRECATION")
                Proxy.getPort(context).toString()
            }
            !host.isNullOrEmpty() && port != "-1"
        } catch (t: Throwable) {
            false
        }
    }
}


