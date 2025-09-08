package com.miaadrajabi.securitymodule.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SecurityHttp {
    fun createPinnedClient(
        hostname: String,
        sha256Pins: List<String>,
        connectTimeoutMs: Long = 10_000,
        readTimeoutMs: Long = 15_000,
        writeTimeoutMs: Long = 15_000
    ): OkHttpClient {
        val pinnerBuilder = CertificatePinner.Builder()
        for (pin in sha256Pins) {
            pinnerBuilder.add(hostname, "sha256/$pin")
        }
        val pinner = pinnerBuilder.build()
        return OkHttpClient.Builder()
            .certificatePinner(pinner)
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }
}


