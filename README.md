# SecurityModule - Android Security Library

Production-grade Android security toolkit for banking/fintech/POS apps.

## JitPack (online implementation)

Add JitPack repository and dependency:

settings.gradle:
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

app/build.gradle:
```gradle
dependencies {
    implementation 'com.github.mirajabi:SecurityKit-Android:1.0.0'
}
```

---

Production-grade Android security toolkit for banking/fintech/POS apps.

## Features

### 🛡️ Device Integrity
- Root detection (multi-signal)
- Emulator detection (Android Studio, Genymotion, BlueStacks)
- Debugger detection
- USB debug detection
- VPN detection
- Developer options

### 🔐 App Integrity
- Signature verification
- Repackaging detection
- File integrity (pluggable)

### 🚫 Anti-Tamper
- Anti-hooking (Frida, Xposed)
- Anti-debugging
- Screen capture protection (FLAG_SECURE + active monitoring + white overlay)

### 🔒 Cryptography
- Hashing: SHA-256/384/512, constant-time compare
- Symmetric: AES-GCM, AES-CBC
- Asymmetric: RSA-OAEP
- Android Keystore helpers
- Certificate pinning (OkHttp)

### ⚙️ Configuration
- JSON configuration (runtime)
- Policy engine (Allow/Warn/Block/Terminate)
- Model/brand overrides
- Telemetry hooks (no PII)

## Setup

### 1) Add dependency (module include)
```gradle
dependencies {
    implementation project(':securitymodule')
}
```

### 2) Proguard/R8
Consumer rules are shipped. For additional keep rules:
```proguard
-keep class com.miaadrajabi.securitymodule.** { *; }
```

### 3) Permissions
The library manifest adds:
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

## Configuration

### Create config JSON (assets/security_config.json)
```json
{
  "features": {
    "rootDetection": true,
    "emulatorDetection": true,
    "debuggerDetection": true,
    "usbDebugDetection": true,
    "vpnDetection": true,
    "mitmDetection": true,
    "screenCaptureProtection": true,
    "appSignatureVerification": true,
    "repackagingDetection": true
  },
  "thresholds": {
    "emulatorSignalsToBlock": 2,
    "rootSignalsToBlock": 2
  },
  "overrides": {
    "allowedModels": ["SM-G973F"],
    "deniedModels": [],
    "allowedBrands": ["samsung"],
    "deniedBrands": ["unknown"]
  },
  "policy": {
    "onRoot": "BLOCK",
    "onEmulator": "BLOCK",
    "onDebugger": "WARN",
    "onUsbDebug": "WARN",
    "onVpn": "WARN",
    "onMitm": "BLOCK"
  },
  "appIntegrity": {
    "expectedPackageName": "com.yourcompany.yourapp",
    "expectedSignatureSha256": [
      "A1:B2:C3:D4:E5:F6:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90"
    ]
  },
  "telemetry": {
    "enabled": true
  }
}
```

### Policy
- Actions: ALLOW / WARN / DEGRADE / BLOCK / TERMINATE
- Thresholds: `emulatorSignalsToBlock`, `rootSignalsToBlock`

## Signature Verification

### 1) Get current signature(s)
```kotlin
val signatures = SignatureVerifier.currentSigningSha256(context)
signatures.forEach { Log.d("Signature", "SHA256: $it") }
```

### 2) Put into config
```json
{
  "appIntegrity": {
    "expectedPackageName": "com.yourcompany.yourapp",
    "expectedSignatureSha256": [
      "1A2B3C4D5E6F7890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890",
      "FEDCBA0987654321FEDCBA0987654321FEDCBA0987654321FEDCBA0987654321"
    ]
  },
  "features": {
    "appSignatureVerification": true
  }
}
```

### 3) Generate per environment
Debug keystore:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```
Release keystore:
```bash
keytool -list -v -keystore your-release-key.keystore -alias your-key-alias
```

### 4) Runtime verification
```kotlin
val actual = SignatureVerifier.currentSigningSha256(context)
val expected = config.appIntegrity.expectedSignatureSha256
val isValid = actual.any { a -> expected.any { it.equals(a, ignoreCase = true) } }
if (!isValid) {
    // take action (block/terminate)
}
```

## Usage

### Basic setup
```kotlin
class MainActivity : Activity() {
    private val telemetry = object : TelemetrySink {
        override fun onEvent(eventId: String, attributes: Map<String, String>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = SecurityConfigLoader.fromAsset(this)
        val securityModule = SecurityModule.Builder(applicationContext)
            .setConfig(config)
            .setTelemetry(telemetry)
            .build()
        val report = securityModule.runAllChecksBlocking()
        when (report.overallSeverity) {
            Severity.OK -> setupNormalUI()
            Severity.WARN -> { showWarningBanner(report.findings); setupNormalUI() }
            Severity.BLOCK -> return
        }
    }
}
```

### Async checks
```kotlin
lifecycleScope.launch {
    val report = securityModule.runAllChecks()
    handleSecurityReport(report)
}
```

### Screen capture protection
```kotlin
if (config.features.screenCaptureProtection) {
    ScreenCaptureProtector.applySecureFlag(this)
    val monitor = ScreenCaptureMonitor(this)
    monitor.start { type, uri ->
        ScreenCaptureProtector.showWhiteOverlay(this@Activity)
        telemetry.onEvent("screenshot_attempt", mapOf("type" to type.name))
    }
}
```

### Crypto utilities
```kotlin
val key = KeystoreHelper.getOrCreateAesKey("my_secure_key")
val plaintext = "sensitive data".toByteArray()
val (iv, ciphertext) = CryptoUtils.encryptAesGcm(key, plaintext)
val decrypted = CryptoUtils.decryptAesGcm(key, iv, ciphertext)
val hash = CryptoUtils.sha256(plaintext)
val isEqual = CryptoUtils.constantTimeEquals(hash, hash)
```

### Certificate pinning
```kotlin
val pinnedClient = SecurityHttp.createPinnedClient(
    hostname = "api.mybank.com",
    sha256Pins = listOf("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                       "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
)
val retrofit = Retrofit.Builder().client(pinnedClient).baseUrl("https://api.mybank.com/").build()
```

### Encrypted storage
```kotlin
val securePrefs = EncryptedPreferences.create(context, "secure_data")
securePrefs.putString("sensitive_token", userToken)
val token = securePrefs.getString("sensitive_token", null)
```

## Threat detections

### Root detection
- su/magisk files, test-keys, ro.debuggable/ro.secure, which su, su -c id
- BusyBox, mount RW, known root packages

### Emulator detection
- Build props, qemu/vbox files, ro.kernel.qemu, default IP 10.0.2.15
- Low sensor count, Genymotion/VirtualBox markers

### Hooking detection
- /proc/self/maps scan, TracerPID, Frida/Xposed/LSPosed markers

## Error handling
```kotlin
try {
    val report = securityModule.runAllChecksBlocking()
    handleReport(report)
} catch (e: SecurityException) {
    // handle failure
}
```

## Best practices
- Tune thresholds for your risk appetite
- Use model/brand overrides for known exceptions
- Wire telemetry to your SIEM/analytics
- Start/stop screenshot monitoring in lifecycle

## Troubleshooting
- Enable verbose telemetry in debug builds
- Dump current signatures and compare with config
- Test matrix: real device, emulator, rooted, VPN/proxy

## Limitations & notes
- Some emulators may bypass FLAG_SECURE (overlay/monitor helps)
- Root detection can be constrained in modern OS/device variations
- SYSTEM_ALERT_WINDOW requires user grant on Android 10+

## Support
- Min SDK: 21 (Android 5.0)
- Target SDK: 34 (Android 14)
- Language: Java 8 / Kotlin
- Architectures: ARM64, ARM, x86, x86_64

## License
MIT License.

For issues/questions, please open a GitHub issue.