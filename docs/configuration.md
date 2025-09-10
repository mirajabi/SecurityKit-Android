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
