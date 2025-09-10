# App Integrity

## Signature verification
1) Extract signing SHA-256 (see /installation and /configuration pages for commands).
2) Put hashes (lowercase, no colons) into `appIntegrity.expectedSignatureSha256`.
3) Enable `features.appSignatureVerification = true`.
4) Policy: `policy.onAppIntegrityFailure`.

## Repackaging detection
- Set `appIntegrity.expectedPackageName` to your package name.
- Enable `features.repackagingDetection = true`.
- Policy: `policy.onAppIntegrityFailure`.

## DEX checksums
- Compute with Python helper and populate `appIntegrity.expectedDexChecksums`.

## Native libraries (.so) checksums
- If APK includes .so, compute combined SHA-256 per ABI and set `appIntegrity.expectedSoChecksums`.
- If none, keep `{}`.
