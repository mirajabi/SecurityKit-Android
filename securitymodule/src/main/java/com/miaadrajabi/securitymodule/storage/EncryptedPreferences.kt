package com.miaadrajabi.securitymodule.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.miaadrajabi.securitymodule.crypto.CryptoUtils
import javax.crypto.SecretKey

class EncryptedPreferences private constructor(
    private val delegate: SharedPreferences,
    private val key: SecretKey
) {
    companion object {
        @JvmStatic fun create(context: Context, name: String = "secure_prefs"): EncryptedPreferences {
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val key = CryptoUtils.generateAesKeyGcm()
            return EncryptedPreferences(prefs, key)
        }
    }

    fun putString(keyName: String, value: String) {
        val (iv, ct) = CryptoUtils.encryptAesGcm(key, value.toByteArray())
        val stored = Base64.encodeToString(iv + ct, Base64.NO_WRAP)
        delegate.edit().putString(keyName, stored).apply()
    }

    fun getString(keyName: String, defValue: String? = null): String? {
        val data = delegate.getString(keyName, null) ?: return defValue
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        if (bytes.size < 12) return defValue
        val iv = bytes.copyOfRange(0, 12)
        val ct = bytes.copyOfRange(12, bytes.size)
        return try {
            String(CryptoUtils.decryptAesGcm(key, iv, ct))
        } catch (t: Throwable) {
            defValue
        }
    }
}


