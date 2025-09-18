# SecurityModule — Comprehensive User Guide (v1.2.0)

## 1. Introduction

SecurityModule is a production-grade Android security toolkit for banking/fintech/POS apps, offering device integrity, app integrity, secure HMAC, runtime protections, and more.

## 2. Installation

### 2.1 Add repository (JitPack)
```groovy
// settings.gradle
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}
```

### 2.2 Add dependency
```groovy
// app/build.gradle
dependencies {
  implementation 'com.github.mirajabi:SecurityKit-Android:1.2.0'
}
```

## 3. Modules and Features

- Device Integrity (root, emulator, ADB, busybox)
- App Integrity (signature, repackaging, hooking)
- Secure HMAC (Keystore, StrongBox, TEE, device-binding)
- Screen Capture Protection (FLAG_SECURE)
- Runtime Protections (debugger/tracing/proxy/MITM)
- Tamper Evidence Store
- Play Integrity (optional)
- Telemetry

## 4. Getting Started (Secure HMAC)

### 4.1 Generate APK signature at build time
```bash
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk -a app/src/main/assets/
```

### 4.2 Verify integrity at runtime
```kotlin
lifecycleScope.launch {
  val info = ApkHmacProtector.verifyApkIntegrity(this@MainActivity)
  if (!info.isIntegrityValid) finish()
}
```

### 4.3 Fallback strategy
- StrongBox → TEE → Software keys
- Automatic capability detection
- Graceful degradation

## 5. API Overview

### 5.1 SecureHmacHelper
- `getOrCreateSecureHmacKey()`
- `getOrCreateStrongBoxHmacKey()`
- `getOrCreateDeviceBoundHmacKey(context)`
- `computeHmacSha256(data, key)`
- `verifyHmacSignature(data, sig, key)`
- `getKeystoreCapabilities()`
- `getBestAvailableHmacKey()` → `(SecretKey, String)`

### 5.2 ApkHmacProtector
- `generateApkHmacSignature(apkPath, context?)`
- `verifyApkIntegrity(context)` → `ApkIntegrityInfo`
- `detectRepackaging(context)` → `Boolean`
- `storeHmacSignatureInAssets(apkPath, context, assetFileName)`

### 5.3 SecurityConfigLoader / ConfigIntegrity
- `fromAssetPreferSigned(context, useSecureHmac = true)`
- `verifyHmacSignature(config, signature, context)`

## 6. Sample Workflows

### 6.1 App Startup Security
```kotlin
class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    lifecycleScope.launch {
      val ok = ApkHmacProtector.verifyApkIntegrity(this@MainActivity).isIntegrityValid
      if (!ok) finish()
    }
  }
}
```

### 6.2 Periodic Monitoring
```kotlin
lifecycleScope.launch(Dispatchers.IO) {
  while (isActive) {
    delay(60000)
    if (ApkHmacProtector.detectRepackaging(this@MainActivity)) {
      withContext(Dispatchers.Main) { finishAffinity() }
      break
    }
  }
}
```

## 7. Emulator vs Real Device
- Emulators: software keystore only, no TEE/StrongBox
- Real devices: TEE widely available, StrongBox on high-end
- Always implement fallbacks

## 8. Best Practices
- Verify integrity on startup
- Prefer hardware-backed keys
- Implement periodic checks
- Log and handle errors gracefully
- Test across OEMs/Android versions

## 9. Troubleshooting
- Signature missing: ensure assets file exists
- Verification fails: re-generate signature for the built APK
- StrongBox not available: fallback to TEE or software

## 10. Performance & Compatibility
- Startup overhead: +50–100ms
- Memory: +1–2MB
- API levels: Keystore 18+, TEE 23+, StrongBox 28+

## 11. Appendix
- CLI scripts: `scripts/generate_apk_hmac.py`, `scripts/sign_apk_with_hmac.sh`
- Docs: see `/docs` for deep dives (fallback strategy, emulator vs real device, technical reference)

---

© 2025 SecurityModule — MIT License
