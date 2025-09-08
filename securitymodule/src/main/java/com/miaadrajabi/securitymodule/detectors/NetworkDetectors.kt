package com.miaadrajabi.securitymodule.detectors

import android.content.Context
import android.net.ConnectivityManager
import android.net.Proxy
import android.os.Build

object ProxyDetector {
    fun isProxyEnabled(context: Context): Boolean {
        return try {
            val host = System.getProperty("http.proxyHost") ?: Proxy.getHost(context)
            val port = System.getProperty("http.proxyPort") ?: Proxy.getPort(context).toString()
            !host.isNullOrEmpty() && port != "-1"
        } catch (t: Throwable) {
            false
        }
    }
}


