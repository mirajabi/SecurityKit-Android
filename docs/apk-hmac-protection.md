# APK HMAC Protection System

## Overview

The APK HMAC Protection System provides comprehensive protection against APK repackaging attacks by using cryptographic HMAC signatures to verify APK integrity at runtime.

## Features

- **APK Integrity Verification**: Verify APK files haven't been tampered with
- **Repackaging Detection**: Detect if APK has been repackaged or modified
- **Hardware-backed Security**: Uses Android Keystore with StrongBox/TEE support
- **Build-time Integration**: Automated signature generation during build process
- **Runtime Verification**: Continuous APK integrity checking

## How It Works

### 1. Build-time Process

```bash
# Generate HMAC signature for APK
./scripts/sign_apk_with_hmac.sh app-release.apk

# Or use Python script directly
python3 scripts/generate_apk_hmac.py app-release.apk apk_signature.txt
```

### 2. Runtime Verification

```kotlin
// Verify APK integrity
val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)

// Detect repackaging
val isRepackaged = ApkHmacProtector.detectRepackaging(context)
```

## Security Levels

### StrongBox (Maximum Security)
- Hardware Security Module (HSM)
- Key never leaves secure hardware
- Highest protection against attacks

### TEE (High Security)
- Trusted Execution Environment
- Hardware-backed security
- Good protection against software attacks

### Software (Medium Security)
- Standard software implementation
- Basic protection against casual tampering

## Implementation Guide

### Step 1: Generate APK Signature

```bash
# During build process
./scripts/sign_apk_with_hmac.sh app-release.apk -a src/main/assets/
```

### Step 2: Embed Signature in App

The signature will be automatically embedded in your app's assets directory.

### Step 3: Runtime Verification

```kotlin
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Verify APK integrity on startup
        lifecycleScope.launch {
            val integrityInfo = ApkHmacProtector.verifyApkIntegrity(this@MainActivity)
            
            if (!integrityInfo.isIntegrityValid) {
                // Handle compromised APK
                handleSecurityBreach()
            }
        }
    }
}
```

### Step 4: Continuous Monitoring

```kotlin
// Check for repackaging periodically
lifecycleScope.launch {
    val isRepackaged = ApkHmacProtector.detectRepackaging(context)
    
    if (isRepackaged) {
        // Handle repackaging detection
        handleRepackagingDetected()
    }
}
```

## API Reference

### ApkHmacProtector

#### `generateApkHmacSignature(apkFilePath: String, context: Context?): String?`
Generates HMAC signature for APK file.

#### `verifyApkIntegrity(context: Context): ApkIntegrityInfo`
Verifies APK integrity and returns detailed information.

#### `detectRepackaging(context: Context): Boolean`
Detects if APK has been repackaged.

#### `storeHmacSignatureInAssets(apkFilePath: String, context: Context, assetFileName: String): Boolean`
Stores HMAC signature in app assets.

### ApkIntegrityInfo

```kotlin
data class ApkIntegrityInfo(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val signatureHash: String,
    val apkHash: String,
    val hmacSignature: String,
    val keyType: String,
    val timestamp: Long,
    val isIntegrityValid: Boolean
)
```

## Build Integration

### Gradle Integration

```gradle
// In your app's build.gradle
android {
    // ... other configurations
    
    buildTypes {
        release {
            // ... other configurations
            
            // Add APK signing task
            doLast {
                exec {
                    commandLine 'bash', '../scripts/sign_apk_with_hmac.sh', 
                        "${buildDir}/outputs/apk/release/app-release.apk",
                        '-a', 'src/main/assets/'
                }
            }
        }
    }
}
```

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Sign APK with HMAC
  run: |
    ./scripts/sign_apk_with_hmac.sh app-release.apk -a src/main/assets/
```

## Security Considerations

### Key Management
- HMAC keys are generated using secure hardware when available
- Keys are device-bound and cannot be extracted
- Fallback to software keys for compatibility

### Signature Storage
- Signatures are embedded in app assets
- Multiple storage locations for redundancy
- Encrypted storage for sensitive data

### Attack Prevention
- **Repackaging**: Detected through signature verification
- **Tampering**: Detected through hash comparison
- **Key Extraction**: Prevented through hardware security
- **Signature Forgery**: Prevented through cryptographic strength

## Testing

### Manual Testing

```kotlin
// Test APK protection
val testResult = ApkHmacProtector.verifyApkIntegrity(context)
assert(testResult.isIntegrityValid) { "APK integrity check failed" }

val repackagingDetected = ApkHmacProtector.detectRepackaging(context)
assert(!repackagingDetected) { "Repackaging detected" }
```

### Automated Testing

```kotlin
@Test
fun testApkIntegrity() {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
    assertTrue(integrityInfo.isIntegrityValid)
}

@Test
fun testRepackagingDetection() {
    val isRepackaged = ApkHmacProtector.detectRepackaging(context)
    assertFalse(isRepackaged)
}
```

## Troubleshooting

### Common Issues

1. **Signature Generation Fails**
   - Check APK file path
   - Verify file permissions
   - Ensure sufficient disk space

2. **Integrity Check Fails**
   - Verify signature is embedded correctly
   - Check HMAC key generation
   - Ensure APK hasn't been modified

3. **Repackaging False Positives**
   - Check installer package name
   - Verify signature certificates
   - Review additional checks

### Debug Mode

```kotlin
// Enable debug logging
ApkHmacProtector.setDebugMode(true)
```

## Best Practices

1. **Always verify APK integrity on startup**
2. **Use hardware-backed keys when available**
3. **Implement continuous monitoring**
4. **Store signatures securely**
5. **Handle security breaches gracefully**
6. **Test on various devices and Android versions**
7. **Keep signatures updated with each build**

## Performance Impact

- **Startup Time**: +50-100ms for integrity check
- **Memory Usage**: +1-2MB for signature storage
- **Battery Impact**: Minimal (only during verification)
- **Network Usage**: None (all operations are local)

## Compatibility

- **Android API**: 21+ (Android 5.0+)
- **StrongBox**: API 28+ (Android 9.0+)
- **TEE**: API 23+ (Android 6.0+)
- **Fallback**: All Android versions

## License

This APK HMAC Protection System is part of the PosSecurity library and is licensed under the same terms.
