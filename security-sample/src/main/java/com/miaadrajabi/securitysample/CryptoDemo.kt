package com.miaadrajabi.securitysample

import android.app.Activity
import android.widget.LinearLayout
import android.widget.TextView
import com.miaadrajabi.securitymodule.crypto.CryptoUtils
import com.miaadrajabi.securitymodule.crypto.KeystoreHelper

fun Activity.renderCryptoDemo(container: LinearLayout) {
    val key = KeystoreHelper.getOrCreateAesKey()
    val message = "Hello Secure World".toByteArray()
    val (iv, ct) = CryptoUtils.encryptAesGcm(key, message)
    val pt = CryptoUtils.decryptAesGcm(key, iv, ct)

    val sha = CryptoUtils.sha256(message)

    val tv = TextView(this)
    tv.text = "AES-GCM roundtrip ok=${String(pt) == "Hello Secure World"}, SHA-256 len=${sha.size}"
    tv.setPadding(32, 16, 32, 16)
    container.addView(tv)
}


