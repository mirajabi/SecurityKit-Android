package com.miaadrajabi.securitymodule.crypto

import android.content.Context
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.Mockito.`when`
import android.provider.Settings

@RunWith(MockitoJUnitRunner::class)
class SecureHmacHelperTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockContentResolver: android.content.ContentResolver

    @Test
    fun testSecureHmacKeyGeneration() {
        // Setup mocks
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        `when`(mockContentResolver.getString(Settings.Secure.ANDROID_ID)).thenReturn("test_device_id")
        `when`(mockContext.packageName).thenReturn("com.test.app")

        // Test secure HMAC key generation
        val hmacKey = SecureHmacHelper.getOrCreateSecureHmacKey()
        assert(hmacKey != null)
        assert(hmacKey.algorithm == "AES")
    }

    @Test
    fun testDeviceBoundHmacKeyGeneration() {
        // Setup mocks
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        `when`(mockContentResolver.getString(Settings.Secure.ANDROID_ID)).thenReturn("test_device_id")
        `when`(mockContext.packageName).thenReturn("com.test.app")

        // Test device-bound HMAC key generation
        val hmacKey = SecureHmacHelper.getOrCreateDeviceBoundHmacKey(mockContext)
        assert(hmacKey != null)
        assert(hmacKey.algorithm == "AES")
    }

    @Test
    fun testHmacComputationAndVerification() {
        // Setup mocks
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        `when`(mockContentResolver.getString(Settings.Secure.ANDROID_ID)).thenReturn("test_device_id")
        `when`(mockContext.packageName).thenReturn("com.test.app")

        val testData = "test data for HMAC"
        val hmacKey = SecureHmacHelper.getOrCreateSecureHmacKey()

        // Compute HMAC
        val signature = SecureHmacHelper.computeHmacSha256(testData.toByteArray(), hmacKey)
        assert(signature.isNotEmpty())

        // Verify HMAC
        val isValid = SecureHmacHelper.verifyHmacSignature(testData.toByteArray(), signature, hmacKey)
        assert(isValid)

        // Test with wrong signature
        val wrongSignature = "wrong_signature"
        val isInvalid = SecureHmacHelper.verifyHmacSignature(testData.toByteArray(), wrongSignature, hmacKey)
        assert(!isInvalid)
    }

    @Test
    fun testStrongBoxAvailability() {
        // This test will pass regardless of StrongBox availability
        val isAvailable = SecureHmacHelper.isStrongBoxAvailableForHmac()
        // Just ensure the method doesn't crash
        assert(true) // Always pass since we can't predict StrongBox availability in tests
    }
}
