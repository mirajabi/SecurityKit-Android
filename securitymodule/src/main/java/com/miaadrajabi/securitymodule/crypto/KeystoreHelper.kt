package com.miaadrajabi.securitymodule.crypto

import android.os.Build
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object KeystoreHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DEFAULT_ALIAS = "SecurityModule.AESGCM"

    fun getOrCreateAesKey(alias: String = DEFAULT_ALIAS): SecretKey {
        return if (Build.VERSION.SDK_INT >= 23) {
            try {
                val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                ks.load(null)
                val existing = ks.getKey(alias, null) as? SecretKey
                if (existing != null) return existing
                val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    alias,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            } catch (t: Throwable) {
                CryptoUtils.generateAesKeyGcm()
            }
        } else {
            CryptoUtils.generateAesKeyGcm()
        }
    }
}


