# Configuration

SecurityModule is driven by a JSON file (typically `assets/security_config.json`).

## Structure
```json
{
  "features": { /* toggles */ },
  "thresholds": { /* sensitivities */ },
  "overrides": { /* allow/deny by device */ },
  "policy": { /* actions per signal */ },
  "appIntegrity": { /* package/signing/dex/so */ },
  "advanced": { /* playIntegrity/keystore/configIntegrity/tamperEvidence */ },
  "telemetry": { "enabled": true }
}
```

### features — enable/disable capabilities
Feature toggles that turn individual checks and protections on/off. Defaults are balanced for production, but you can tighten or loosen per your threat model.

- **rootDetection**: Detect root artifacts (su/busybox), mount flags, known packages.
- **emulatorDetection**: Detect emulator fingerprints (QEMU props, Build.* heuristics).
- **debuggerDetection**: Detect attached Java/Native debuggers.
- **usbDebugDetection**: Detect ADB/USB debugging enabled on device.
- **vpnDetection**: Detect active VPN network interfaces.
- **mitmDetection**: Detect basic TLS interception signals (user CAs/proxy), see `docs/crypto.md`.
- **screenCaptureProtection**: Apply `FLAG_SECURE` to prevent screenshots/recording.
- **appSignatureVerification**: Verify app signing certificate matches expected hash(es).
- **repackagingDetection**: Detect repackaging/tampering indicators.
- **playIntegrityCheck**: Use Play Integrity API signals (see `docs/play-integrity.md`).
- **advancedAppIntegrity**: Enable checksum-based validation of `dex`/`so` files.
- **strongBoxKeys**: Prefer StrongBox-backed keys when available.
- **userAuthBoundKeys**: Require user auth (biometric/PIN) to use keys.
- **deviceBinding**: Bind cryptographic material to device properties.
- **configIntegrity**: Verify configuration authenticity (HMAC/RSA).
- **tamperEvidence**: Persist tamper-evidence and cache secure decisions.

Example:
```json
{
  "features": {
    "rootDetection": true,
    "emulatorDetection": true,
    "screenCaptureProtection": true,
    "appSignatureVerification": true,
    "playIntegrityCheck": false,
    "strongBoxKeys": true,
    "deviceBinding": true
  }
}
```

### thresholds — sensitivity dials
Define how many signals must be observed before a condition is considered severe enough to block. Higher numbers reduce false positives; lower numbers are stricter.

- **emulatorSignalsToBlock** (default 2)
- **rootSignalsToBlock** (default 2)
- **playIntegritySignalsToBlock** (default 1)
- **appIntegritySignalsToBlock** (default 1)

Example:
```json
{
  "thresholds": {
    "emulatorSignalsToBlock": 2,
    "rootSignalsToBlock": 2,
    "playIntegritySignalsToBlock": 1,
    "appIntegritySignalsToBlock": 1
  }
}
```

Tips:
- Set `emulatorSignalsToBlock=1` only for strict environments (may affect some legitimate devices).
- Keep Play/App integrity thresholds at 1 unless you aggregate multiple sub-signals.

### overrides — allow/deny by device
Safelists or blocks specific devices early based on `Build.*` identifiers. Strings are exact matches.

- **allowedModels**, **deniedModels** map to `Build.MODEL`.
- **allowedBrands**, **deniedBrands** map to `Build.BRAND`.
- **allowedManufacturers** map to `Build.MANUFACTURER`.
- **allowedProducts** map to `Build.PRODUCT`.
- **allowedDevices** map to `Build.DEVICE`.
- **allowedBoards** map to `Build.BOARD`.

Behavior (summary): if a device matches an allowed rule, checks may be bypassed; matching a denied rule blocks early. See details in `USAGE_GUIDE.md` (Device Override System).

Example:
```json
{
  "overrides": {
    "allowedModels": ["Pixel 6", "Galaxy S21"],
    "allowedManufacturers": ["Google", "Samsung"],
    "deniedBrands": ["unknown"],
    "allowedProducts": ["motion_phone_arm64"]
  }
}
```

### policy — actions per signal
Map each detection category to an action. Allowed actions: `ALLOW`, `WARN`, `DEGRADE`, `BLOCK`, `TERMINATE`. For guidance, see `docs/policy.md`.

- **onRoot**, **onEmulator**, **onDebugger**, **onUsbDebug**, **onVpn**, **onMitm**
- **onPlayIntegrityFailure**, **onAppIntegrityFailure**, **onConfigTampering**, **onStrongBoxUnavailable**

Example:
```json
{
  "policy": {
    "onRoot": "BLOCK",
    "onEmulator": "WARN",
    "onDebugger": "WARN",
    "onUsbDebug": "WARN",
    "onVpn": "WARN",
    "onMitm": "BLOCK",
    "onPlayIntegrityFailure": "WARN",
    "onAppIntegrityFailure": "BLOCK",
    "onConfigTampering": "BLOCK",
    "onStrongBoxUnavailable": "WARN"
  }
}
```

Action semantics (typical):
- **ALLOW**: log and continue; no UI.
- **WARN**: continue with non-blocking warning.
- **DEGRADE**: restrict sensitive features, continue core flows.
- **BLOCK**: stop flow (e.g., logout/disable payments).
- **TERMINATE**: close the app/session.

### appIntegrity — package/signing/artifact checks
Ensure your app identity and shipped artifacts haven’t been tampered with. See also `docs/app-integrity.md`.

- **expectedPackageName**: fully-qualified package name (e.g., `com.example.app`).
- **expectedSignatureSha256**: array of SHA‑256 certificate hashes (supporting multi-signer/multi-flavor).
- **allowedInstallers**: package names that are allowed to install the app (Play Store, OEM stores, etc.).
- **expectedDexChecksums**: map of `dex` filename → SHA‑256.
- **expectedSoChecksums**: map of `so` filename → SHA‑256.

Example:
```json
{
  "appIntegrity": {
    "expectedPackageName": "com.yourcompany.app",
    "expectedSignatureSha256": [
      "your_release_cert_sha256_here"
    ],
    "allowedInstallers": [
      "com.android.vending",
      "com.samsung.android.galaxyapps"
    ],
    "expectedDexChecksums": {
      "classes.dex": "<sha256>",
      "classes2.dex": "<sha256>"
    },
    "expectedSoChecksums": {
      "lib/arm64-v8a/libyour.so": "<sha256>"
    }
  }
}
```

How to get signing cert SHA‑256:
- Gradle: run `signingReport` task and copy SHA‑256 for release.
- APK: `apksigner verify --print-certs your.apk` or `keytool -printcert -jarfile your.apk`.

### advanced — specialized subsystems
Advanced settings for Play Integrity, Keystore, configuration integrity, and tamper-evidence.

Subsections:

#### advanced.playIntegrity
- **enabled**: turn on Play Integrity checks.
- **nonce**: default nonce string; prefer generating a per-request nonce at runtime.
- **fallbackOnUnavailable**: if true, do not hard-block when service unavailable (respect `policy`).
- **timeoutSeconds**: call timeout.

Example:
```json
{
  "advanced": {
    "playIntegrity": {
      "enabled": true,
      "nonce": "default_nonce",
      "fallbackOnUnavailable": true,
      "timeoutSeconds": 30
    }
  }
}
```

See `docs/play-integrity.md` for setup and server-side validation guidance.

#### advanced.keystore
- **preferStrongBox**: request StrongBox if available.
- **userAuthRequired**: require user authentication to use keys.
- **userAuthValiditySeconds**: grace period after auth.
- **deviceBindingEnabled**: bind secrets/derived keys to device properties.

Example:
```json
{
  "advanced": {
    "keystore": {
      "preferStrongBox": true,
      "userAuthRequired": true,
      "userAuthValiditySeconds": 300,
      "deviceBindingEnabled": true
    }
  }
}
```

See `docs/keystore.md` for platform caveats and best practices.

#### advanced.configIntegrity
- **enabled**: verify configuration authenticity.
- **verificationMethod**: `HMAC` or `RSA`.
- **hmacKey**: secret key for HMAC (do not hardcode in source; prefer `BuildConfig` or server-provided).
- **publicKeyPem**: PEM public key for RSA verification.
- **networkConfig**: optional remote config loader and cache settings.

Example (HMAC):
```json
{
  "advanced": {
    "configIntegrity": {
      "enabled": true,
      "verificationMethod": "HMAC",
      "hmacKey": "${CONFIG_HMAC_KEY}"
    }
  }
}
```

Example (RSA + network):
```json
{
  "advanced": {
    "configIntegrity": {
      "enabled": true,
      "verificationMethod": "RSA",
      "publicKeyPem": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----\n",
      "networkConfig": {
        "configUrl": "https://your.cdn/config.json",
        "signatureUrl": "https://your.cdn/config.json.sig",
        "timeoutSeconds": 15,
        "cacheMaxAgeSeconds": 3600
      }
    }
  }
}
```

See the sections "Signed configuration" and "Network loading" below for tooling and runtime loaders.

#### advanced.tamperEvidence
- **enabled**: enable tamper-evidence store.
- **maxCacheAgeSeconds**: TTL for cached secure decisions/entries.
- **autoRotateKeys**: automatically rotate internal store keys.
- **keyRotationIntervalSeconds**: rotation cadence.

Example:
```json
{
  "advanced": {
    "tamperEvidence": {
      "enabled": true,
      "maxCacheAgeSeconds": 86400,
      "autoRotateKeys": true,
      "keyRotationIntervalSeconds": 604800
    }
  }
}
```

See `docs/tamper-evidence.md` for usage patterns.

### telemetry — usage and signal reporting
Enable/disable telemetry reporting. When enabled, minimal, privacy-aware telemetry can be recorded. See `docs/telemetry.md`.

Example:
```json
{
  "telemetry": { "enabled": true }
}
```

## Signed configuration (HMAC RAW JSON)
Use provided scripts to sign RAW JSON bytes and verify at runtime.

### macOS/Linux
```bash
CONFIG_HMAC_KEY="your-secret" ./scripts/sign-config-hmac.sh \
  --config app/src/main/assets/security_config.json \
  --out    app/src/main/assets/security_config.sig
```

### Windows PowerShell
```powershell
$env:CONFIG_HMAC_KEY="your-secret"
py scripts\sign_config_hmac.py --config app\src\main\assets\security_config.json `
  --key-env CONFIG_HMAC_KEY --out app\src\main\assets\security_config.sig
```

### Loading (Kotlin)
```kotlin
val key = BuildConfig.CONFIG_HMAC_KEY
val config = com.miaadrajabi.securitymodule.config.SecurityConfigLoader.fromAssetPreferSigned(
  this, "security_config.json", "security_config.sig", key.ifEmpty { null }
)
```

### Loading (Java)
```java
String key = BuildConfig.CONFIG_HMAC_KEY;
SecurityConfig config = SecurityConfigLoader.fromAssetPreferSigned(
  this, "security_config.json", "security_config.sig",
  (key != null && !key.isEmpty()) ? key : null
);
```

## Network loading
Use `ConfigIntegrity.SecureLoader.fromNetworkWithHmac(...)` or `fromNetworkWithRsa(...)`, then cache with the tamper-evidence store.
