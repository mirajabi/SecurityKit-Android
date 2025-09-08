package com.miaadrajabi.securitymodule.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

object CryptoUtils {
    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    class AesGcmResult(val iv: ByteArray, val ciphertext: ByteArray)

    // Hashing
    @JvmStatic fun sha256(data: ByteArray): ByteArray = hash("SHA-256", data)
    @JvmStatic fun sha384(data: ByteArray): ByteArray = hash("SHA-384", data)
    @JvmStatic fun sha512(data: ByteArray): ByteArray = hash("SHA-512", data)

    private fun hash(algorithm: String, data: ByteArray): ByteArray = MessageDigest.getInstance(algorithm).digest(data)

    @JvmStatic fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    // AES-GCM
    @JvmStatic fun generateAesKeyGcm(): SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256)
        return kg.generateKey()
    }

    @JvmStatic fun encryptAesGcm(key: SecretKey, plaintext: ByteArray, aad: ByteArray? = null): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // For Android Keystore-backed AES-GCM keys, provider must generate a random IV
        cipher.init(Cipher.ENCRYPT_MODE, key)
        if (aad != null) cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv
        return iv to ciphertext
    }

    @JvmStatic fun encryptAesGcmResult(key: SecretKey, plaintext: ByteArray, aad: ByteArray? = null): AesGcmResult {
        val (iv, ct) = encryptAesGcm(key, plaintext, aad)
        return AesGcmResult(iv, ct)
    }

    @JvmStatic fun decryptAesGcm(key: SecretKey, iv: ByteArray, ciphertext: ByteArray, aad: ByteArray? = null): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        if (aad != null) cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }

    // AES-CBC with HMAC-SHA256 integrity (legacy)
    @JvmStatic fun encryptAesCbc(key: SecretKey, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv to ciphertext
    }

    @JvmStatic fun decryptAesCbc(key: SecretKey, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return cipher.doFinal(ciphertext)
    }

    // RSA (OAEP recommended)
    @JvmStatic fun generateRsaKeyPair(bits: Int = 2048): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(bits)
        return kpg.generateKeyPair()
    }

    @JvmStatic fun rsaEncryptOaep(publicKeyBytes: ByteArray, plaintext: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("RSA")
        val publicKey = kf.generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(plaintext)
    }

    @JvmStatic fun rsaDecryptOaep(privateKeyBytes: ByteArray, ciphertext: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(ciphertext)
    }
}


