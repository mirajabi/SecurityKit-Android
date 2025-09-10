# SecurityModule - Android Security Library (v1.1.0)

Production-grade Android security toolkit for banking/fintech/POS apps.

---

## Features

### 🛡️ Device Integrity
- Root detection (multi-signal: su/magisk files, mount flags, test-keys, BusyBox, etc.)
- Emulator detection (Android Studio, Genymotion, BlueStacks, Nox, LDPlayer, MEmu, MuMu, VBox/QEMU variants)
- Debugger/ptrace detection, TracerPID
- USB debug detection, Developer Options
- VPN/Proxy/MITM indicators

### 🔐 App Integrity
- Signature verification (v2/v3/v4 aware via SigningInfo/legacy)
- Repackaging detection (package name, installer source)
- APK integrity: DEX checksums (`expectedDexChecksums`) and native libs checksums (`expectedSoChecksums`)

### 🚫 Runtime Protections
- Anti-hooking/instrumentation (Frida/Xposed markers, suspicious loaded libs)
- Screen capture protection: FLAG_SECURE + active monitor + white overlay workaround

### 🔒 Cryptography
- Hashing: SHA-256/384/512, constant-time equals
- Symmetric: AES-GCM/AES-CBC (Android Keystore backed)
- Asymmetric: RSA-OAEP
- Secure storage: EncryptedPreferences

### ⚙️ Configuration & Policy Engine
- JSON-based `security_config.json`
- Actions per signal: ALLOW, WARN, BLOCK, TERMINATE
- Thresholds and device overrides (model/brand/manufacturer/product/device/board)

### ✅ Signed Configuration (New)
- Optional signed-config loading (HMAC-SHA256 over RAW JSON) for tamper-evidence
- Simple loader: `SecurityConfigLoader.fromAssetPreferSigned(...)`
- Cross-platform signing scripts included

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
    implementation 'com.github.mirajabi:SecurityKit-Android:1.1.0'
}
```

---

## Supported emulators/simulators (detection)
- Android Studio/AOSP (QEMU: goldfish/ranchu)
- Genymotion (VirtualBox)
- BlueStacks (VBox/Hyper-V traces)
- Nox
- LDPlayer
- MEmu
- MuMu
- Other VBox/QEMU derivatives

Detection combines multiple signals: build/system properties, QEMU/VBox device files, /proc markers, default emulator IP (10.0.2.15), and low sensor count. Thresholds are configurable.

## Quick use (Java)

1) Add dependency via JitPack (above)
2) Put `assets/security_config.json` (see sample below)
3) In your Activity:
```java
import android.app.Activity;
import android.os.Bundle;
import com.miaadrajabi.securitymodule.SecurityModule;
import com.miaadrajabi.securitymodule.SecurityFinding;
import com.miaadrajabi.securitymodule.SecurityReport;
import com.miaadrajabi.securitymodule.Severity;
import com.miaadrajabi.securitymodule.config.SecurityConfig;
import com.miaadrajabi.securitymodule.config.SecurityConfigLoader;
import com.miaadrajabi.securitymodule.telemetry.TelemetrySink;

public class MainActivity extends Activity {
  private final TelemetrySink telemetry = new TelemetrySink() {
    @Override public void onEvent(String eventId, java.util.Map<String,String> attrs) { }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SecurityConfig config = SecurityConfigLoader.fromAsset(this);
    SecurityModule module = new SecurityModule.Builder(getApplicationContext())
        .setConfig(config)
        .setTelemetry(telemetry)
        .build();
    SecurityReport report = module.runAllChecksBlocking();
    if (report.getOverallSeverity() == Severity.BLOCK) {
      finish();
      return;
    }
    // TODO: render your UI and optionally show findings
  }
}
```

## Java usage examples

### Signature verification (populate config)
```java
java.util.List<String> sigs = com.miaadrajabi.securitymodule.detectors.SignatureVerifier
    .currentSigningSha256(getApplicationContext());
// Put these hashes into appIntegrity.expectedSignatureSha256 in your config JSON
```

### Screen capture protection
```java
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector;
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureMonitor;

// Apply FLAG_SECURE
ScreenCaptureProtector.applySecureFlag(this);
// Monitor and show white overlay on attempt
ScreenCaptureMonitor monitor = new ScreenCaptureMonitor(this);
monitor.start((type, uri) -> {
  ScreenCaptureProtector.showWhiteOverlay(this);
});
```

### Crypto utilities (AES-GCM, hashing)
```java
import javax.crypto.SecretKey;
import com.miaadrajabi.securitymodule.crypto.CryptoUtils;
import com.miaadrajabi.securitymodule.crypto.KeystoreHelper;

SecretKey key = KeystoreHelper.getOrCreateAesKey("my_secure_key");
byte[] data = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
CryptoUtils.AesGcmResult res = CryptoUtils.encryptAesGcmResult(key, data, null);
byte[] pt = CryptoUtils.decryptAesGcm(key, res.getIv(), res.getCiphertext());
byte[] hash = CryptoUtils.sha256(data);
boolean eq = CryptoUtils.constantTimeEquals(hash, hash);
```

### Certificate pinning (OkHttp)
```java
import okhttp3.OkHttpClient;
import com.miaadrajabi.securitymodule.network.SecurityHttp;

OkHttpClient client = SecurityHttp.createPinnedClient(
  "api.mybank.com",
  java.util.Arrays.asList("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="),
  10_000, 15_000, 15_000
);
```

### Encrypted preferences
```java
import com.miaadrajabi.securitymodule.storage.EncryptedPreferences;

EncryptedPreferences prefs = EncryptedPreferences.create(getApplicationContext(), "secure_prefs");
prefs.putString("token", "abc");
String token = prefs.getString("token", null);
```

## Example security_config.json
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
      "... your sha256 cert hash ..."
    ]
  },
  "telemetry": { "enabled": true }
}
```

## Full Java example
```java
package com.example;

import android.app.Activity;
import android.os.Bundle;
import com.miaadrajabi.securitymodule.SecurityModule;
import com.miaadrajabi.securitymodule.SecurityReport;
import com.miaadrajabi.securitymodule.Severity;
import com.miaadrajabi.securitymodule.config.SecurityConfig;
import com.miaadrajabi.securitymodule.config.SecurityConfigLoader;
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureMonitor;
import com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector;
import com.miaadrajabi.securitymodule.telemetry.TelemetrySink;

public class SecureActivity extends Activity {
  private ScreenCaptureMonitor monitor;

  private final TelemetrySink telemetry = new TelemetrySink() {
    @Override public void onEvent(String id, java.util.Map<String,String> attrs) { }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SecurityConfig config = SecurityConfigLoader.fromAsset(this);
    if (config.getFeatures().isScreenCaptureProtection()) {
      ScreenCaptureProtector.applySecureFlag(this);
      monitor = new ScreenCaptureMonitor(this);
      monitor.start((type, uri) -> ScreenCaptureProtector.showWhiteOverlay(this));
    }

    SecurityModule module = new SecurityModule.Builder(getApplicationContext())
        .setConfig(config)
        .setTelemetry(telemetry)
        .build();

    SecurityReport report = module.runAllChecksBlocking();
    if (report.getOverallSeverity() == Severity.BLOCK) {
      finish();
      return;
    }

    // TODO render content
  }

  @Override protected void onDestroy() {
    if (monitor != null) monitor.stop();
    super.onDestroy();
  }
}
```

---

## Features

### 🛡️ Device Integrity
- Root detection (multi-signal)
- Emulator detection (Android Studio, Genymotion, BlueStacks, Nox, LDPlayer, MEmu, MuMu, VBox/QEMU variants)
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

(see example JSON above)

## Threat detections

### Root detection
- su/magisk files, test-keys, ro.debuggable/ro.secure, which su, su -c id
- BusyBox, mount RW, known root packages

### Emulator detection
- Build props, qemu/vbox files, ro.kernel.qemu, default IP 10.0.2.15
- Low sensor count, Genymotion/VirtualBox markers

### Hooking detection
- /proc/self/maps scan, TracerPID, Frida/Xposed/LSPosed markers

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

---

## Step-by-step integration (Kotlin)

1) Add dependency (JitPack or module include)
2) Create `assets/security_config.json` (copy sample above)
3) (Optional) Sign your config
   - macOS/Linux:
     ```bash
     CONFIG_HMAC_KEY="your-secret" ./scripts/sign-config-hmac.sh \
       --config security-sample/src/main/assets/security_config.json \
       --out    security-sample/src/main/assets/security_config.sig
     ```
   - Windows PowerShell:
     ```powershell
     $env:CONFIG_HMAC_KEY="your-secret"
     py scripts\sign_config_hmac.py --config security-sample\src\main\assets\security_config.json `
       --key-env CONFIG_HMAC_KEY --out security-sample\src\main\assets\security_config.sig
     ```
4) Provide HMAC key at runtime
   - Demo: `CONFIG_HMAC_KEY=your-secret ./gradlew :security-sample:assembleDebug`
   - Prod: fetch from secure source; pass to loader
5) Load config (prefers signed) and run checks:
   ```kotlin
   val hmacKey = BuildConfig.CONFIG_HMAC_KEY
   val config = SecurityConfigLoader.fromAssetPreferSigned(
       this, "security_config.json", "security_config.sig",
       hmacKey.ifEmpty { null }
   )
   val module = SecurityModule.Builder(applicationContext)
       .setConfig(config)
       .build()
   val report = module.runAllChecksBlocking()
   if (report.overallSeverity != Severity.OK) {
       ReportActivity.start(this, report, config)
       finish()
   }
   ```
6) Screen-capture protection:
   ```kotlin
   if (config.features.screenCaptureProtection) {
       ScreenCaptureProtector.applySecureFlag(this)
       ScreenCaptureMonitor(this).start { _, _ ->
           ScreenCaptureProtector.showWhiteOverlay(this)
       }
   }
   ```

## Step-by-step integration (Java)

1) Add dependency and config as above
2) Sign config (same commands)
3) Load securely and run checks:
```java
String key = BuildConfig.CONFIG_HMAC_KEY; // or from secure source
SecurityConfig config = SecurityConfigLoader.fromAssetPreferSigned(
    this, "security_config.json", "security_config.sig",
    (key != null && !key.isEmpty()) ? key : null
);
SecurityModule module = new SecurityModule.Builder(getApplicationContext())
    .setConfig(config)
    .build();
SecurityReport report = module.runAllChecksBlocking();
if (report.getOverallSeverity() != Severity.OK) {
  ReportActivity.start(this, report, config);
  finish();
}
```

### Populating integrity fields
- Signature SHA-256: extract with keytool/apksigner (see earlier)
- DEX/so checksums: use provided Python snippets; paste into `appIntegrity.expectedDexChecksums` and `expectedSoChecksums`.
- If no .so in APK, keep `expectedSoChecksums` as `{}`.