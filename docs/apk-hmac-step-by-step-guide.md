# APK HMAC Protection - Step-by-step Guide

## 📋 Table of contents
1. [Introduction and goals](#introduction-and-goals)
2. [Step 1: Project preparation](#step-1-project-preparation)
3. [Step 2: Installation and configuration](#step-2-installation-and-configuration)
4. [Step 3: Build the APK](#step-3-build-the-apk)
5. [Step 4: Generate HMAC signature](#step-4-generate-hmac-signature)
6. [Step 5: App integration](#step-5-app-integration)
7. [Step 6: Test and verification](#step-6-test-and-verification)
8. [Step 7: Production rollout](#step-7-production-rollout)
9. [Troubleshooting](#troubleshooting)
10. [Best practices](#best-practices)

---

## Introduction and goals

The **APK HMAC Protection System** protects your APK against **repackaging** and **tampering**.

### 🎯 Goals
- Prevent unauthorized APK modification
- Detect unknown or untrusted installer sources
- Verify app authenticity at runtime
- Leverage hardware-backed security (StrongBox/TEE) when available

---

## Step 1: Project preparation

### 1.1 Prerequisites

```bash
# Check Java version
java -version
# باید Java 8 یا بالاتر باشد

# Check Python version
python3 --version
# باید Python 3.6 یا بالاتر باشد

# Ensure scripts directory exists
ls -la scripts/
```

### 1.2 Project structure

```
your-project/
├── app/
│   ├── src/main/
│   │   ├── assets/          # signature will be stored here
│   │   ├── java/
│   │   └── res/
│   └── build.gradle
├── scripts/                 # HMAC scripts
└── docs/                    # documentation
```

### 1.3 Add dependency

In `app/build.gradle`:

```gradle
dependencies {
    implementation 'com.miaadrajabi.securitymodule:securitymodule:1.0.0'
    // or if using local module:
    implementation project(':securitymodule')
}
```

---

## Step 2: Installation and configuration

### 2.1 Copy scripts

```bash
# Copy scripts into your project
cp -r /path/to/PosSecurity/scripts/ ./scripts/
chmod +x scripts/*.sh
chmod +x scripts/*.py
```

### 2.2 Test scripts

```bash
# Python script
python3 scripts/generate_apk_hmac.py --help

# Shell script
./scripts/sign_apk_with_hmac.sh --help
```

### 2.3 Gradle configuration

In `app/build.gradle`:

```gradle
android {
    // ... other settings
    
    buildTypes {
        release {
            // ... other settings
            
            // Add a task to generate HMAC post-build
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
```

---

## Step 3: Build the APK

### 3.1 Build project

```bash
# Clean
./gradlew clean

# Build APK
./gradlew assembleRelease

# Or debug
./gradlew assembleDebug
```

### 3.2 Verify built APK

```bash
# Ensure APK exists
ls -la app/build/outputs/apk/release/

# Check APK size
du -h app/build/outputs/apk/release/app-release.apk
```

### 3.3 APK path

Your APK will be at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## Step 4: Generate HMAC signature

### 4.1 Manual

```bash
# Generate with shell script
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk

# Or with Python
python3 scripts/generate_apk_hmac.py app/build/outputs/apk/release/app-release.apk
```

### 4.2 Automated (recommended)

```bash
# Build with automated signature generation
./gradlew assembleRelease
```

### 4.3 Verify generated signature

```bash
# Check signature file
ls -la app/src/main/assets/apk_hmac_signature.txt

# Inspect signature content
cat app/src/main/assets/apk_hmac_signature.txt
```

### 4.4 Expected output

```
✅ APK HMAC signing completed successfully!
📁 Output file: app-release_hmac_signature.txt
📊 APK hash: a1b2c3d4e5f6...
🔏 HMAC signature: 680ddcb08678f2932bf01672f9671ec0bdba1a6bdb161e989235c9981c3289c9
```

---

## Step 5: App integration

### 5.1 Imports

In `MainActivity.kt` or your entry activity:

```kotlin
import com.miaadrajabi.securitymodule.crypto.ApkHmacProtector
import kotlinx.coroutines.*
```

### 5.2 Add verification in onCreate

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Verify APK integrity at startup
        verifyApkIntegrity()
    }
    
    private fun verifyApkIntegrity() {
        lifecycleScope.launch {
            try {
                // Verify integrity
                val integrityInfo = ApkHmacProtector.verifyApkIntegrity(this@MainActivity)
                
                if (!integrityInfo.isIntegrityValid) {
                    // APK modified — take a security action
                    handleSecurityBreach()
                    return@launch
                }
                
                // Check repackaging
                val isRepackaged = ApkHmacProtector.detectRepackaging(this@MainActivity)
                
                if (isRepackaged) {
                    // APK repackaged — take a security action
                    handleRepackagingDetected()
                    return@launch
                }
                
                // OK
                Log.d("APK", "APK integrity verified successfully")
                
            } catch (e: Exception) {
                Log.e("APK", "APK integrity check failed", e)
                // On error, take a security action
                handleSecurityError()
            }
        }
    }
    
    private fun handleSecurityBreach() {
        // اقدامات امنیتی در صورت تغییر APK
        Toast.makeText(this, "Security breach detected!", Toast.LENGTH_LONG).show()
        // می‌توانید اپلیکیشن را ببندید یا به صفحه امنیتی بروید
        finish()
    }
    
    private fun handleRepackagingDetected() {
        // اقدامات امنیتی در صورت repackaging
        Toast.makeText(this, "Repackaging detected!", Toast.LENGTH_LONG).show()
        finish()
    }
    
    private fun handleSecurityError() {
        // اقدامات امنیتی در صورت خطا
        Toast.makeText(this, "Security check failed!", Toast.LENGTH_LONG).show()
        finish()
    }
}
```

### 5.3 Periodic checks

```kotlin
class MainActivity : AppCompatActivity() {
    
    private val securityCheckInterval = 30000L // 30 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initial verification
        verifyApkIntegrity()
        
        // Periodic check
        startPeriodicSecurityCheck()
    }
    
    private fun startPeriodicSecurityCheck() {
        lifecycleScope.launch {
            while (isActive) {
                delay(securityCheckInterval)
                
                try {
                    val isRepackaged = ApkHmacProtector.detectRepackaging(this@MainActivity)
                    if (isRepackaged) {
                        handleRepackagingDetected()
                        break
                    }
                } catch (e: Exception) {
                    Log.e("APK", "Periodic security check failed", e)
                }
            }
        }
    }
}
```

---

## Step 6: Test and verification

### 6.1 Initial test

```bash
# Build and install
./gradlew installRelease

# Or debug variant
./gradlew installDebug
```

### 6.2 Behavior tests

1. Success case:
   - App launches normally
   - Logs show "APK integrity verified successfully"

2. Tamper case:
   - Modify APK (e.g., apktool)
   - App should block and show a security error

### 6.3 Sample app button test

In the sample app:
1. Tap "🛡️ APK HMAC Protection"
2. Review results in `TestResultsActivity`

### 6.4 Logs

```bash
# App logs
adb logcat | grep "ApkHmacProtector"

# Or
adb logcat | grep "APK"
```

---

## Step 7: Production rollout

### 7.1 Final build

```bash
# Clean and build
./gradlew clean
./gradlew assembleRelease

# Verify final APK
ls -la app/build/outputs/apk/release/
```

### 7.2 Verify signature

```bash
# Ensure signature exists
cat app/src/main/assets/apk_hmac_signature.txt

# Inspect APK contents
unzip -l app/build/outputs/apk/release/app-release.apk | grep signature
```

### 7.3 Final test

```bash
# Install and test
adb install -r app/build/outputs/apk/release/app-release.apk

# Launch and verify behavior
adb shell am start -n com.yourpackage/.MainActivity
```

### 7.4 Publish

```bash
# Upload to Google Play or your store
# APK with protected signature is ready
```

---

## Troubleshooting

### Issue 1: Script does not run

```bash
# Check permissions
ls -la scripts/sign_apk_with_hmac.sh

# Grant execute permission
chmod +x scripts/sign_apk_with_hmac.sh

# Test run
./scripts/sign_apk_with_hmac.sh --help
```

### Issue 2: Python script errors

```bash
# Check Python version
python3 --version

# Install dependencies
pip3 install hashlib hmac

# Help
python3 scripts/generate_apk_hmac.py --help
```

### Issue 3: Signature not generated

```bash
# Check APK path
ls -la app/build/outputs/apk/release/app-release.apk

# File type
file app/build/outputs/apk/release/app-release.apk

# Manual run (verbose)
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk -v
```

### Issue 4: App crashes

```kotlin
// Add try/catch
try {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
    // ...
} catch (e: Exception) {
    Log.e("APK", "Integrity check failed", e)
    // take appropriate action
}
```

### Issue 5: Signature not found

```kotlin
// Check signature file exists
val signatureFile = File("${context.filesDir}/apk_hmac_signature.txt")
if (!signatureFile.exists()) {
    Log.e("APK", "Signature file not found")
    // regenerate signature or use a fallback
}
```

---

## Best practices

### 1. Security

```kotlin
// Always run on background thread
lifecycleScope.launch(Dispatchers.IO) {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
    // ...
}

// Use hardware-backed keys
val key = SecureHmacHelper.getBestAvailableHmacKey()
```

### 2. Performance

```kotlin
// Tune periodic checks
private val securityCheckInterval = 60000L // 1 دقیقه

// Use caches
private var lastIntegrityCheck = 0L
private val integrityCheckInterval = 300000L // 5 دقیقه
```

### 3. Error handling

```kotlin
// Graceful error handling
private fun handleSecurityError() {
    // لاگ کردن
    Log.e("APK", "Security check failed")
    
    // notify the user
    showSecurityWarning()
    
    // security action
    // finish() or redirect to a security screen
}
```

### 4. Testing

```kotlin
// Unit tests
@Test
fun testApkIntegrity() {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
    assertTrue(integrityInfo.isIntegrityValid)
}

// Integration tests
@Test
fun testRepackagingDetection() {
    val isRepackaged = ApkHmacProtector.detectRepackaging(context)
    assertFalse(isRepackaged)
}
```

### 5. CI/CD

```yaml
# GitHub Actions example
- name: Build APK
  run: ./gradlew assembleRelease

- name: Generate HMAC Signature
  run: ./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk

- name: Test APK
  run: ./gradlew test
```

---

## Summary

1. ✅ Preparation: dependencies and scripts
2. ✅ Configuration: Gradle and paths
3. ✅ Build: produce APK
4. ✅ Signature: run HMAC script
5. ✅ Integration: add verification code
6. ✅ Testing: verify behavior
7. ✅ Deployment: publish

---

## Support

If you encounter issues:
1. Check logs
2. Read docs
3. Run sample tests
4. Contact the team

**🎉 Your APK is now protected by HMAC and hardened against repackaging!**
