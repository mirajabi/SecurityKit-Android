# 📚 APK HMAC Protection System Documentation

## 🎯 Overview

The **APK HMAC Protection System** is an advanced security system to protect Android apps against **repackaging** and **tampering**.

## 📖 Documentation Index

### 🚀 Quick Start
- **[Quick Start](apk-hmac-quick-start.md)** — Get started in 5 minutes
- **[Step-by-step Guide](apk-hmac-step-by-step-guide.md)** — Full guide

### 📱 Practical Examples
- **[APK HMAC — Practical Example](apk-hmac-practical-example.md)** — Complete implementation
- **[APK HMAC — Technical Documentation](apk-hmac-protection.md)** — Technical details

### 🔧 Tools & Scripts
- **[Python Script](generate_apk_hmac.py)** — Generate HMAC signature
- **[Shell Script](sign_apk_with_hmac.sh)** — Automate signing process

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Build Time    │    │   Runtime       │    │   Security      │
│                 │    │                 │    │                 │
│ 1. Build APK    │───▶│ 1. Load App     │───▶│ 1. Verify      │
│ 2. Generate     │    │ 2. Check        │    │ 2. Detect       │
│    HMAC         │    │    Integrity    │    │    Repackaging  │
│ 3. Store        │    │ 3. Monitor      │    │ 3. Take Action  │
│    Signature    │    │    Continuously │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔐 Security Levels

### 🛡️ StrongBox (Maximum Security)
- Hardware Security Module (HSM)
- Keys never leave secure hardware
- Highest protection against attacks

### 🔒 TEE (High Security)
- Trusted Execution Environment
- Hardware-backed security
- Strong protection against software attacks

### 💻 Software (Medium Security)
- Standard software implementation
- Baseline protection against tampering

## 🚀 Quick Usage

### 1. Copy scripts
```bash
cp -r /path/to/PosSecurity/scripts/ ./scripts/
chmod +x scripts/*.sh scripts/*.py
```

### 2. Add dependency
```gradle
dependencies {
    implementation project(':securitymodule')
}
```

### 3. Build & Sign
```bash
./gradlew assembleRelease
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk
```

### 4. Add verification code
```kotlin
lifecycleScope.launch {
    val info = ApkHmacProtector.verifyApkIntegrity(context)
    if (!info.isIntegrityValid) {
        finish() // take security action
    }
}
```

## 📋 Features

- ✅ **APK Authenticity** — Detect APK modifications
- ✅ **Repackaging Detection** — Identify untrusted installers
- ✅ **Hardware-backed Security** — StrongBox/TEE support
- ✅ **Build Integration** — Automated in build pipeline
- ✅ **Periodic Checks** — Continuous monitoring
- ✅ **Robust Error Handling** — Graceful degradation

## 🔧 Tools

### Python Script
```bash
python3 scripts/generate_apk_hmac.py app-release.apk
```

### Shell Script
```bash
./scripts/sign_apk_with_hmac.sh app-release.apk -a src/main/assets/
```

### Gradle Integration
```gradle
buildTypes {
    release {
        doLast {
            exec {
                commandLine 'bash', '../scripts/sign_apk_with_hmac.sh', 
                    "${buildDir}/outputs/apk/release/app-release.apk"
            }
        }
    }
}
```

## 📊 Performance

| Metric | Impact |
|-------|-------|
| Startup time | +50–100ms |
| Memory usage | +1–2MB |
| Battery impact | Minimal |
| Network usage | None |

## 🔍 Testing

### Normal Test
```bash
adb install app-release.apk
adb shell am start -n com.yourpackage/.MainActivity
```

### Repackaging Test
```bash
apktool d app-release.apk
apktool b app-release -o modified.apk
adb install modified.apk
```

## 🛠️ Troubleshooting

### Common issue 1: Script does not run
```bash
chmod +x scripts/sign_apk_with_hmac.sh
```

### Common issue 2: Signature not found
```bash
ls -la app/src/main/assets/apk_hmac_signature.txt
```

### Common issue 3: App crashes
```kotlin
try {
    val info = ApkHmacProtector.verifyApkIntegrity(context)
} catch (e: Exception) {
    Log.e("APK", "Error", e)
}
```

## 📚 More Resources

- **[Step-by-step Guide](apk-hmac-step-by-step-guide.md)** — Full details
- **[Practical Example](apk-hmac-practical-example.md)** — Real implementation
- **[Technical Documentation](apk-hmac-protection.md)** — API and internals
- **[Emulator vs Real Device](emulator-vs-real-device.md)**
- **[Fallback Strategy](fallback-strategy.md)**
- **[Secure HMAC](secure-hmac.md)**
- **[Main Activity Test Hub UI](main-activity-ui.md)**

## 🤝 Contributing

1. Fork the repo
2. Create a new branch
3. Commit your changes
4. Open a Pull Request

## 📄 License

This project is released under the MIT License.

## 🆘 Support

If you face issues:
1. Read the docs
2. Review the examples
3. Search existing issues
4. Open a new issue

---

**🎉 Your APK is now protected by HMAC and hardened against repackaging!**
