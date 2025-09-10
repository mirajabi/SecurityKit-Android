## 1.1.0 - 2025-09-10

### Added
- Signed configuration support (HMAC-SHA256 over RAW JSON) with simple loader:
  - `SecurityConfigLoader.fromAssetPreferSigned(context, "security_config.json", "security_config.sig", hmacKey)`
  - `ConfigIntegrity.verifyHmacSignatureRaw`, `loadSignedConfigFromAssetRaw`
- Cross-platform signing scripts:
  - `scripts/sign_config_hmac.py` (Python)
  - `scripts/sign-config-hmac.sh` (Bash wrapper)
- Device override fields: `allowedManufacturers`, `allowedProducts`, `allowedDevices`, `allowedBoards`
- Sample wired to prefer signed config via `BuildConfig.CONFIG_HMAC_KEY`

### Improved
- README with step-by-step Kotlin and Java integration, and usage guide `USAGE_GUIDE.md`
- Advanced deprecation handling and API-level fallbacks

### Fixed
- BuildConfig flag configuration (`buildFeatures.buildConfig true`)
- Minor compilation issues and sample app flow

# Changelog

All notable changes to this project will be documented here.

## [1.0.1] - 2025-09-08
- English-only clean up (removed Persian comments/strings)
- Expanded emulator detection (more props/files/proc/sensors network heuristics)
- Java interop: added @JvmStatic for easier usage from Java

## [1.0.0] - 2025-09-08
- Initial public release
- Device integrity: root/emulator/debugger/USB/VPN/dev-options/proxy/MITM
- App integrity: signature verification, repackaging detection
- Runtime protections: anti-hooking, tracer PID, screen-capture protection (FLAG_SECURE + white overlay + monitoring)
- Crypto: SHA-2, AES-GCM/CBC, RSA-OAEP, Keystore helper, encrypted preferences
- Config system: JSON + policy engine + overrides + telemetry hooks
- Sample app: detailed report UI, crypto demo, automatic screen protection
- CI: Android build/test; Release: JitPack publish via maven-publish

[1.0.1]: https://github.com/mirajabi/SecurityKit-Android/releases/tag/1.0.1
[1.0.0]: https://github.com/mirajabi/SecurityKit-Android/releases/tag/1.0.0
