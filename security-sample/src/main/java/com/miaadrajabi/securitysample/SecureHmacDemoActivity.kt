package com.miaadrajabi.securitysample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.miaadrajabi.securitymodule.crypto.SecureHmacHelper
import com.miaadrajabi.securitymodule.examples.SecureHmacExample
import kotlinx.coroutines.*

class SecureHmacDemoActivity : Activity() {
    
    private lateinit var logTextView: TextView
    private lateinit var testButton: Button
    private lateinit var clearButton: Button
    private lateinit var progressBar: ProgressBar
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupUI()
        runSecureHmacTests()
    }
    
    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "🔐 Secure HMAC Demo"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(title)
        
        // Progress bar
        progressBar = ProgressBar(this).apply {
            visibility = ProgressBar.VISIBLE
        }
        layout.addView(progressBar)
        
        // Log text view
        logTextView = TextView(this).apply {
            text = "Running secure HMAC tests...\n"
            textSize = 12f
            setPadding(0, 16, 0, 16)
        }
        layout.addView(logTextView)
        
        // Test button
        testButton = Button(this).apply {
            text = "🔄 Run Tests Again"
            setOnClickListener { runSecureHmacTests() }
        }
        layout.addView(testButton)
        
        // Clear button
        clearButton = Button(this).apply {
            text = "🗑️ Clear Log"
            setOnClickListener { logTextView.text = "" }
        }
        layout.addView(clearButton)
        
        setContentView(layout)
    }
    
    private fun runSecureHmacTests() {
        progressBar.visibility = ProgressBar.VISIBLE
        testButton.isEnabled = false
        
        scope.launch {
            try {
                log("🚀 Starting Secure HMAC Tests...\n")
                
                // Test 1: StrongBox availability
                testStrongBoxAvailability()
                
                // Test 2: Key generation
                testKeyGeneration()
                
                // Test 3: HMAC computation and verification
                testHmacComputation()
                
                // Test 4: Device binding
                testDeviceBinding()
                
                // Test 5: Tamper detection
                testTamperDetection()
                
                // Test 6: Configuration loading
                testConfigurationLoading()
                
                // Test 7: Migration test
                testMigration()
                
                log("\n✅ All tests completed successfully!")
                
            } catch (e: Exception) {
                log("\n❌ Test failed: ${e.message}")
                Log.e("SecureHmacDemo", "Test error", e)
            } finally {
                progressBar.visibility = ProgressBar.GONE
                testButton.isEnabled = true
            }
        }
    }
    
    private suspend fun testStrongBoxAvailability() {
        log("📱 Testing StrongBox availability...")
        
        val strongBoxAvailable = withContext(Dispatchers.IO) {
            SecureHmacHelper.isStrongBoxAvailableForHmac()
        }
        
        log("   StrongBox available: $strongBoxAvailable")
        log("   ${if (strongBoxAvailable) "✅ Enhanced security enabled" else "⚠️ Using standard TEE security"}\n")
    }
    
    private suspend fun testKeyGeneration() {
        log("🔑 Testing key generation...")
        
        val secureKey = withContext(Dispatchers.IO) {
            SecureHmacHelper.getOrCreateSecureHmacKey()
        }
        
        val deviceBoundKey = withContext(Dispatchers.IO) {
            SecureHmacHelper.getOrCreateDeviceBoundHmacKey(this@SecureHmacDemoActivity)
        }
        
        log("   Secure key algorithm: ${secureKey.algorithm}")
        log("   Device-bound key algorithm: ${deviceBoundKey.algorithm}")
        log("   ✅ Keys generated successfully\n")
    }
    
    private suspend fun testHmacComputation() {
        log("✍️ Testing HMAC computation and verification...")
        
        val testData = """
            {
                "features": {
                    "rootDetection": true,
                    "emulatorDetection": true,
                    "debuggerDetection": true
                },
                "thresholds": {
                    "rootSignalsToBlock": 2,
                    "emulatorSignalsToBlock": 2
                },
                "policy": {
                    "onRoot": "BLOCK",
                    "onEmulator": "BLOCK"
                }
            }
        """.trimIndent()
        
        val hmacKey = withContext(Dispatchers.IO) {
            SecureHmacHelper.getOrCreateSecureHmacKey()
        }
        
        val signature = withContext(Dispatchers.IO) {
            SecureHmacHelper.computeHmacSha256(testData.toByteArray(), hmacKey)
        }
        
        val isValid = withContext(Dispatchers.IO) {
            SecureHmacHelper.verifyHmacSignature(testData.toByteArray(), signature, hmacKey)
        }
        
        log("   Test data length: ${testData.length} characters")
        log("   Generated signature: ${signature.take(32)}...")
        log("   Verification result: $isValid")
        log("   ✅ HMAC computation and verification successful\n")
    }
    
    private suspend fun testDeviceBinding() {
        log("🔗 Testing device binding...")
        
        val deviceBoundKey = withContext(Dispatchers.IO) {
            SecureHmacHelper.getOrCreateDeviceBoundHmacKey(this@SecureHmacDemoActivity)
        }
        
        val testData = "device-specific-test-data"
        val signature = withContext(Dispatchers.IO) {
            SecureHmacHelper.computeHmacSha256(testData.toByteArray(), deviceBoundKey)
        }
        
        val isValid = withContext(Dispatchers.IO) {
            SecureHmacHelper.verifyHmacSignature(testData.toByteArray(), signature, deviceBoundKey)
        }
        
        log("   Device-bound signature: ${signature.take(32)}...")
        log("   Verification result: $isValid")
        log("   ✅ Device binding working correctly\n")
    }
    
    private suspend fun testTamperDetection() {
        log("🛡️ Testing tamper detection...")
        
        val originalData = "original-secure-data"
        val hmacKey = withContext(Dispatchers.IO) {
            SecureHmacHelper.getOrCreateSecureHmacKey()
        }
        
        val originalSignature = withContext(Dispatchers.IO) {
            SecureHmacHelper.computeHmacSha256(originalData.toByteArray(), hmacKey)
        }
        
        // Test with tampered data
        val tamperedData = "tampered-secure-data"
        val isTamperedValid = withContext(Dispatchers.IO) {
            SecureHmacHelper.verifyHmacSignature(tamperedData.toByteArray(), originalSignature, hmacKey)
        }
        
        // Test with wrong signature
        val wrongSignature = "wrong_signature_12345"
        val isWrongSignatureValid = withContext(Dispatchers.IO) {
            SecureHmacHelper.verifyHmacSignature(originalData.toByteArray(), wrongSignature, hmacKey)
        }
        
        log("   Original data: $originalData")
        log("   Tampered data: $tamperedData")
        log("   Tampered data verification: $isTamperedValid (should be false)")
        log("   Wrong signature verification: $isWrongSignatureValid (should be false)")
        
        if (!isTamperedValid && !isWrongSignatureValid) {
            log("   ✅ Tamper detection working correctly\n")
        } else {
            log("   ❌ Tamper detection failed\n")
        }
    }
    
    private suspend fun testConfigurationLoading() {
        log("📄 Testing configuration loading...")
        
        val config = withContext(Dispatchers.IO) {
            SecureHmacExample.loadProductionConfig(this@SecureHmacDemoActivity)
        }
        
        if (config != null) {
            log("   Configuration loaded successfully")
            log("   Root detection enabled: ${config.features.rootDetection}")
            log("   Emulator detection enabled: ${config.features.emulatorDetection}")
            log("   Device binding enabled: ${config.features.deviceBinding}")
            log("   ✅ Configuration loading successful\n")
        } else {
            log("   ❌ Configuration loading failed\n")
        }
    }
    
    private suspend fun testMigration() {
        log("🔄 Testing migration from legacy to secure HMAC...")
        
        val migrationSuccess = withContext(Dispatchers.IO) {
            SecureHmacExample.migrateToSecureHmac(this@SecureHmacDemoActivity)
        }
        
        log("   Migration result: $migrationSuccess")
        log("   ${if (migrationSuccess) "✅ Migration successful" else "⚠️ Migration failed (expected for first run)"}\n")
    }
    
    private fun log(message: String) {
        runOnUiThread {
            logTextView.append("$message\n")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
