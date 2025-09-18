## 1.2.0 - 2025-01-20

### Added
- **APK HMAC Protection System** - Complete protection against APK repackaging
  - `ApkHmacProtector` class for APK integrity verification
  - Hardware-backed HMAC signatures (StrongBox/TEE/Software)
  - Repackaging detection and prevention
  - Build-time integration with automated signature generation
- **APK Signing Scripts**:
  - `scripts/generate_apk_hmac.py` - Python script for HMAC generation
  - `scripts/sign_apk_with_hmac.sh` - Shell script for automated signing
- **Comprehensive Documentation**:
  - `docs/apk-hmac-step-by-step-guide.md` - Complete step-by-step guide
  - `docs/apk-hmac-quick-start.md` - Quick start guide
  - `docs/apk-hmac-practical-example.md` - Real-world banking app example
  - `docs/apk-hmac-protection.md` - Technical documentation
- **Enhanced MainActivity**:
  - APK HMAC Protection test button
  - Comprehensive HMAC testing with detailed algorithm analysis
  - Sign Up & HMAC test for complete user registration flow
- **Security Features**:
  - Device-bound HMAC keys
  - Tamper detection and evidence storage
  - Continuous APK integrity monitoring
  - Graceful security breach handling

### Improved
- **SecureHmacHelper**:
  - Enhanced TEE detection using Google's official `KeyInfo.isInsideSecureHardware()`
  - Improved fallback strategy (StrongBox → TEE → Software → SimpleSoftware)
  - Better error handling and logging
  - Samsung Galaxy A14 specific optimizations
- **Emulator Detection**:
  - Enhanced Genymotion detection
  - Comprehensive emulator signal collection
  - Better false positive handling
- **VPN Detection**:
  - Improved Outline VPN detection
  - Enhanced VPN interface detection
  - Better network interface analysis

### Fixed
- TEE detection inconsistencies on Samsung Galaxy A14
- HMAC computation issues with StrongBox keys
- GCM cipher mode compatibility problems
- Test key cleanup in Keystore capabilities
- Various linter errors and compilation issues

### Technical Details
- **APK Integrity Verification**: SHA-256 hash + HMAC-SHA256 signature
- **Key Types**: StrongBox (HSM), TEE (Hardware), Software (Fallback)
- **Performance**: +50-100ms startup time, +1-2MB memory usage
- **Compatibility**: Android API 21+ (Android 5.0+)
- **Security Level**: Maximum (Hardware-backed) to Basic (Software fallback)

## 1.0.0 - 2025-09-10

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
