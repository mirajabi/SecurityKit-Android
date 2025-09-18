# APK HMAC Protection - Quick Start

## 🚀 Get Started (5 minutes)

### Step 1: Copy scripts
```bash
cp -r /path/to/PosSecurity/scripts/ ./scripts/
chmod +x scripts/*.sh scripts/*.py
```

### Step 2: Add dependency
```gradle
// app/build.gradle
dependencies {
    implementation project(':securitymodule')
}
```

### Step 3: Build APK
```bash
./gradlew assembleRelease
```

### Step 4: Generate HMAC signature
```bash
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk
```

### Step 5: Add verification code
```kotlin
// In MainActivity.onCreate()
lifecycleScope.launch {
    val info = ApkHmacProtector.verifyApkIntegrity(this@MainActivity)
    if (!info.isIntegrityValid) {
        // Take a security action
        finish()
    }
}
```

## ✅ Quick test

```bash
# Install and launch
adb install app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.yourpackage/.MainActivity
```

## 🔧 Quick troubleshooting

### Script does not run
```bash
chmod +x scripts/sign_apk_with_hmac.sh
```

### Signature not found
```bash
ls -la app/src/main/assets/apk_hmac_signature.txt
```

### App crashes
```kotlin
try {
    val info = ApkHmacProtector.verifyApkIntegrity(context)
} catch (e: Exception) {
    Log.e("APK", "Error", e)
}
```

## 📋 Checklist

- [ ] Scripts copied
- [ ] Dependency added
- [ ] APK built
- [ ] HMAC signature generated
- [ ] Verification code added
- [ ] Test completed

## 🎯 Result

Your APK is now protected by HMAC and hardened against repackaging.

---

For full details, see: [apk-hmac-step-by-step-guide.md](apk-hmac-step-by-step-guide.md)
