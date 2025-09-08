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
