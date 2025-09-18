# APK HMAC Protection - Practical Example

## 📱 Scenario: Banking application

Assume you are building a banking app that must be protected against repackaging.

---

## 🏗️ Step 1: Project setup

### Project structure
```
BankingApp/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   ├── java/com/bank/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── LoginActivity.kt
│   │   │   └── SecurityManager.kt
│   │   └── res/
│   └── build.gradle
├── scripts/
└── docs/
```

### app/build.gradle
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.bank.app"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
            
            // Add task to generate HMAC
            doLast {
                exec {
                    commandLine 'bash', '../scripts/sign_apk_with_hmac.sh', 
                        "${buildDir}/outputs/apk/release/app-release.apk",
                        '-a', 'src/main/assets/',
                        '-v'
                }
            }
        }
    }
}

dependencies {
    implementation project(':securitymodule')
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
```

---

## 🔐 Step 2: Implement SecurityManager

### SecurityManager.kt
```kotlin
package com.bank.app

import android.content.Context
import android.util.Log
import com.miaadrajabi.securitymodule.crypto.ApkHmacProtector
import kotlinx.coroutines.*

class SecurityManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isSecurityVerified = false
    
    companion object {
        private const val TAG = "SecurityManager"
        private const val SECURITY_CHECK_INTERVAL = 60000L // 1 minute
    }
    
    /**
     * Verify APK security at startup
     */
    suspend fun verifyStartupSecurity(): Boolean {
        return try {
            Log.d(TAG, "Starting APK security verification...")
            
            // Verify APK integrity
            val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
            
            if (!integrityInfo.isIntegrityValid) {
                Log.e(TAG, "APK integrity check failed")
                return false
            }
            
            // Check repackaging
            val isRepackaged = ApkHmacProtector.detectRepackaging(context)
            
            if (isRepackaged) {
                Log.e(TAG, "Repackaging detected")
                return false
            }
            
            Log.d(TAG, "APK security verification passed")
            isSecurityVerified = true
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "APK security verification failed", e)
            false
        }
    }
    
    /**
     * Start periodic security checks
     */
    fun startPeriodicSecurityCheck() {
        scope.launch {
            while (isActive && isSecurityVerified) {
                delay(SECURITY_CHECK_INTERVAL)
                
                try {
                    val isRepackaged = ApkHmacProtector.detectRepackaging(context)
                    
                    if (isRepackaged) {
                        Log.e(TAG, "Repackaging detected during periodic check")
                        handleSecurityBreach()
                        break
                    }
                    
                    Log.d(TAG, "Periodic security check passed")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Periodic security check failed", e)
                    handleSecurityError()
                    break
                }
            }
        }
    }
    
    /**
     * Handle security breach
     */
    private fun handleSecurityBreach() {
        Log.e(TAG, "Security breach detected - terminating app")
        
        // Persist security event
        saveSecurityEvent("APK_REPACKAGING_DETECTED")
        
        // Notify backend
        notifySecurityBreach()
        
        // Close app
        scope.launch(Dispatchers.Main) {
            (context as? android.app.Activity)?.finish()
        }
    }
    
    /**
     * Handle security error
     */
    private fun handleSecurityError() {
        Log.e(TAG, "Security error occurred")
        
        // Persist event
        saveSecurityEvent("APK_SECURITY_ERROR")
        
        // Apply mitigation (limit functionality)
        limitAppFunctionality()
    }
    
    /**
     * Save security event
     */
    private fun saveSecurityEvent(event: String) {
        try {
            val prefs = context.getSharedPreferences("security_events", Context.MODE_PRIVATE)
            val events = prefs.getStringSet("events", mutableSetOf()) ?: mutableSetOf()
            events.add("${System.currentTimeMillis()}:$event")
            prefs.edit().putStringSet("events", events).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save security event", e)
        }
    }
    
    /**
     * Notify backend about breach
     */
    private fun notifySecurityBreach() {
        scope.launch {
            try {
                Log.d(TAG, "Notifying server about security breach")
                // TODO: Implement API call
            } catch (e: Exception) {
                Log.e(TAG, "Failed to notify server", e)
            }
        }
    }
    
    /**
     * Limit app functionality
     */
    private fun limitAppFunctionality() {
        Log.w(TAG, "Limiting app functionality due to security concerns")
    }
    
    /**
     * Current verification state
     */
    fun isSecurityVerified(): Boolean = isSecurityVerified
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }
}
```

---

## 🏠 Step 3: Implement MainActivity

### MainActivity.kt
```kotlin
package com.bank.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var securityManager: SecurityManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Create SecurityManager
        securityManager = SecurityManager(this)
        
        // Verify security at startup
        verifySecurityAndProceed()
    }
    
    private fun verifySecurityAndProceed() {
        lifecycleScope.launch {
            // Show loading
            showLoadingDialog("Verifying app security...")
            
            try {
                // Verify APK security
                val isSecure = securityManager.verifyStartupSecurity()
                
                if (isSecure) {
                    // Security OK
                    hideLoadingDialog()
                    proceedToMainApp()
                } else {
                    // Breach detected
                    hideLoadingDialog()
                    handleSecurityBreach()
                }
                
            } catch (e: Exception) {
                hideLoadingDialog()
                handleSecurityError(e)
            }
        }
    }
    
    private fun proceedToMainApp() {
        // Start periodic security checks
        securityManager.startPeriodicSecurityCheck()
        
        // Go to main screen
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    private fun handleSecurityBreach() {
        Toast.makeText(this, "Security breach detected. App will close.", Toast.LENGTH_LONG).show()
        
        // Close app
        finishAffinity()
    }
    
    private fun handleSecurityError(e: Exception) {
        Toast.makeText(this, "Security check failed. Please restart the app.", Toast.LENGTH_LONG).show()
        
        // Close app
        finishAffinity()
    }
    
    private fun showLoadingDialog(message: String) {
        // Implement loading dialog
        // TODO: show dialog
    }
    
    private fun hideLoadingDialog() {
        // Hide loading dialog
        // TODO: hide dialog
    }
    
    override fun onDestroy() {
        super.onDestroy()
        securityManager.cleanup()
    }
}
```

---

## 🔑 Step 4: Implement LoginActivity

### LoginActivity.kt
```kotlin
package com.bank.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var securityManager: SecurityManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Create SecurityManager
        securityManager = SecurityManager(this)
        
        // Verify security before login
        verifySecurityBeforeLogin()
    }
    
    private fun verifySecurityBeforeLogin() {
        lifecycleScope.launch {
            try {
                // Check current security state
                val isSecure = securityManager.isSecurityVerified()
                
                if (!isSecure) {
                    // Not verified yet
                    handleSecurityBreach()
                    return@launch
                }
                
                // Security OK — enable login form
                enableLoginForm()
                
            } catch (e: Exception) {
                handleSecurityError(e)
            }
        }
    }
    
    private fun enableLoginForm() {
        // Enable login form
        // TODO: enable UI elements
    }
    
    private fun handleSecurityBreach() {
        Toast.makeText(this, "Security breach detected. Please restart the app.", Toast.LENGTH_LONG).show()
        finishAffinity()
    }
    
    private fun handleSecurityError(e: Exception) {
        Toast.makeText(this, "Security check failed. Please restart the app.", Toast.LENGTH_LONG).show()
        finishAffinity()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        securityManager.cleanup()
    }
}
```

---

## 🏗️ Step 5: Build & Deploy

### Build Script
```bash
#!/bin/bash
# build_and_sign.sh

echo "🏗️ Building Banking App..."

# Clean
./gradlew clean

# Build
./gradlew assembleRelease

# Check if APK was built
if [ ! -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo "❌ APK build failed"
    exit 1
fi

echo "✅ APK built successfully"

# Generate HMAC signature
echo "🔐 Generating HMAC signature..."
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk -a src/main/assets/ -v

# Check if signature was generated
if [ ! -f "app/src/main/assets/apk_hmac_signature.txt" ]; then
    echo "❌ HMAC signature generation failed"
    exit 1
fi

echo "✅ HMAC signature generated successfully"

# Final verification
echo "🔍 Verifying final APK..."
ls -la app/build/outputs/apk/release/app-release.apk
ls -la app/src/main/assets/apk_hmac_signature.txt

echo "🎉 Banking App is ready for deployment!"
```

### Run the build
```bash
chmod +x build_and_sign.sh
./build_and_sign.sh
```

---

## 🧪 Step 6: Tests

### Test 1: Normal flow
```bash
# Install APK
adb install app/build/outputs/apk/release/app-release.apk

# Launch app
adb shell am start -n com.bank.app/.MainActivity

# Logs
adb logcat | grep "SecurityManager"
```

### Test 2: Repackaging
```bash
# Modify APK with apktool
apktool d app/build/outputs/apk/release/app-release.apk
# Change files
apktool b app-release -o modified.apk
# Install modified APK
adb install modified.apk

# Expected: security error
adb shell am start -n com.bank.app/.MainActivity
```

### Test 3: Performance
```bash
# Measure startup
adb shell am start -W com.bank.app/.MainActivity

# Memory usage
adb shell dumpsys meminfo com.bank.app
```

---

## 📊 Expected results

### Success
```
D/SecurityManager: Starting APK security verification...
D/SecurityManager: APK security verification passed
D/SecurityManager: Periodic security check passed
```

### Repackaging
```
E/SecurityManager: APK integrity check failed
E/SecurityManager: Repackaging detected
E/SecurityManager: Security breach detected - terminating app
```

### Performance
```
Startup time: +50-100ms
Memory usage: +1-2MB
Battery impact: Minimal
```

---

## 🔧 Troubleshooting

### Issue 1: App crashes
```kotlin
try {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
} catch (e: Exception) {
    Log.e("Security", "Error", e)
    // take appropriate action
}
```

### Issue 2: Signature not found
```bash
# Check file exists
ls -la app/src/main/assets/apk_hmac_signature.txt

# Inspect APK contents
unzip -l app/build/outputs/apk/release/app-release.apk | grep signature
```

### Issue 3: Performance
```kotlin
// Tune periodic interval
private const val SECURITY_CHECK_INTERVAL = 300000L // 5 minutes

// Use cache
private var lastCheck = 0L
```

---

## 🎯 Summary

This walkthrough demonstrates:

1. ✅ Using a SecurityManager to orchestrate checks
2. ✅ Verifying security at startup
3. ✅ Periodic monitoring for repackaging
4. ✅ Robust error and breach handling
5. ✅ Automated build-time HMAC signature
6. ✅ Thorough testing of behavior and performance

**🏦 Your banking app is now well protected!**
