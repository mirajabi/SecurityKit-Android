# Secure HMAC Implementation

## Overview

This document describes the secure HMAC implementation using Android Keystore for configuration integrity verification. This implementation provides enhanced security by storing HMAC keys in hardware-backed secure storage.

## Features

### 🔐 Hardware-Backed Security
- **Android Keystore Integration**: HMAC keys are stored in TEE (Trusted Execution Environment)
- **StrongBox Support**: Enhanced security on devices with dedicated security hardware
- **Device Binding**: Keys are bound to device properties for uniqueness

### 🛡️ Tamper Detection
- **Constant-Time Comparison**: Prevents timing attacks
- **Automatic Verification**: Built-in signature verification
- **Fallback Support**: Graceful degradation on older devices

### 🔄 Backward Compatibility
- **Legacy Support**: Old HMAC methods still work
- **Migration Tools**: Easy transition from string-based keys
- **Deprecation Warnings**: Clear migration path

## Usage Examples

### Basic Usage (Recommended)

```kotlin
// Load configuration with secure HMAC
val config = SecurityConfigLoader.fromAssetPreferSigned(
    context = context,
    assetName = "security_config.json",
    signatureAssetName = "security_config.sig",
    useSecureHmac = true // Use Android Keystore
)
```

### Advanced Usage

```kotlin
// Check StrongBox availability
val strongBoxAvailable = SecureHmacHelper.isStrongBoxAvailableForHmac()

// Generate device-bound HMAC key
val deviceBoundKey = SecureHmacHelper.getOrCreateDeviceBoundHmacKey(context)

// Compute HMAC signature
val signature = SecureHmacHelper.computeHmacSha256(data.toByteArray(), deviceBoundKey)

// Verify HMAC signature
val isValid = SecureHmacHelper.verifyHmacSignature(data.toByteArray(), signature, deviceBoundKey)
```

### Migration from Legacy HMAC

```kotlin
// Test migration
val migrationSuccess = SecureHmacExample.migrateToSecureHmac(context)
if (migrationSuccess) {
    // Use secure HMAC
    val config = SecurityConfigLoader.fromAssetPreferSigned(context, useSecureHmac = true)
} else {
    // Fallback to legacy method
    val config = SecurityConfigLoader.fromAssetPreferSigned(context, hmacKey = legacyKey)
}
```

## Security Benefits

### Before (Legacy HMAC)
```kotlin
// ❌ Security Issues:
val hmacKey = BuildConfig.CONFIG_HMAC_KEY  // Visible in code
val signature = CryptoUtils.hmacSha256(data, hmacKey.toByteArray())  // Key in memory
```

### After (Secure HMAC)
```kotlin
// ✅ Secure Implementation:
val hmacKey = SecureHmacHelper.getOrCreateDeviceBoundHmacKey(context)  // Hardware-backed
val signature = SecureHmacHelper.computeHmacSha256(data, hmacKey)  // Key never exposed
```

## Implementation Details

### Key Generation
- **TEE Storage**: Keys stored in Trusted Execution Environment
- **Device Binding**: Keys incorporate device-specific properties
- **StrongBox**: Enhanced security on supported devices

### Signature Verification
- **Constant-Time**: Prevents timing attacks
- **Hardware-Backed**: Keys never leave secure hardware
- **Automatic Fallback**: Software keys on older devices

### Configuration Loading
- **Secure by Default**: Uses Android Keystore when available
- **Fallback Support**: Graceful degradation
- **Error Handling**: Comprehensive error management

## Demo Application

The sample application includes a comprehensive demo:

1. **MainActivity**: Shows secure HMAC in action
2. **SecureHmacDemoActivity**: Interactive demo with real-time testing
3. **Log Output**: Detailed logging of all operations

### Running the Demo

1. Build and install the sample app
2. Open the app - secure HMAC tests run automatically
3. Click "🔐 Secure HMAC Demo" for interactive testing
4. Check logcat for detailed output

### Expected Log Output

```
I/SecureHMAC: StrongBox available for HMAC: true
I/SecureHMAC: Secure HMAC key generated: AES
I/SecureHMAC: Device-bound HMAC key generated: AES
I/SecureHMAC: HMAC signature generated: a1b2c3d4e5f6...
I/SecureHMAC: HMAC verification result: true
```

## Best Practices

### Production Deployment
1. **Enable Secure HMAC**: Use `useSecureHmac = true`
2. **Test Migration**: Verify migration works on target devices
3. **Monitor Logs**: Check for StrongBox availability
4. **Fallback Plan**: Ensure graceful degradation

### Development
1. **Use Demo**: Test with SecureHmacDemoActivity
2. **Check Compatibility**: Test on various Android versions
3. **Verify Signatures**: Use provided signature generation script
4. **Monitor Performance**: Measure impact on app startup

## Troubleshooting

### Common Issues

#### StrongBox Not Available
```
I/SecureHMAC: StrongBox available for HMAC: false
```
**Solution**: This is normal on older devices. The system will use TEE instead.

#### Migration Fails
```
I/SecureHMAC: Migration to secure HMAC failed
```
**Solution**: This is expected on first run. The system will use fallback methods.

#### Signature Verification Fails
```
I/SecureHMAC: HMAC verification result: false
```
**Solution**: Check that signature file matches the configuration file.

### Debug Steps

1. **Check Logs**: Look for "SecureHMAC" tag in logcat
2. **Verify Files**: Ensure signature file exists and is valid
3. **Test Manually**: Use SecureHmacDemoActivity for testing
4. **Check Compatibility**: Verify Android version and hardware support

## API Reference

### SecureHmacHelper

#### Key Generation
- `getOrCreateSecureHmacKey()`: Standard secure HMAC key
- `getOrCreateStrongBoxHmacKey()`: StrongBox-backed key
- `getOrCreateDeviceBoundHmacKey(context)`: Device-bound key

#### HMAC Operations
- `computeHmacSha256(data, key)`: Compute HMAC signature
- `verifyHmacSignature(data, signature, key)`: Verify signature
- `isStrongBoxAvailableForHmac()`: Check StrongBox support

#### Utility Methods
- `clearAllHmacKeys()`: Clear all stored keys
- `rotateMacKey(context)`: Rotate MAC keys

### SecurityConfigLoader

#### Secure Loading
- `fromAssetPreferSigned(context, useSecureHmac = true)`: Load with secure HMAC
- `fromAssetPreferSigned(context, hmacKey)`: Legacy method (deprecated)

### ConfigIntegrity

#### Verification Methods
- `verifyHmacSignature(config, signature, context)`: Secure verification
- `verifyHmacSignature(config, signature, hmacKey)`: Legacy method (deprecated)
- `loadSignedConfigFromAsset(context, useSecureHmac = true)`: Secure loading

## Migration Guide

### Step 1: Update Configuration Loading
```kotlin
// Old way
val config = SecurityConfigLoader.fromAssetPreferSigned(context, hmacKey = key)

// New way
val config = SecurityConfigLoader.fromAssetPreferSigned(context, useSecureHmac = true)
```

### Step 2: Test Migration
```kotlin
val migrationSuccess = SecureHmacExample.migrateToSecureHmac(context)
```

### Step 3: Update Build Configuration
```kotlin
// Remove hardcoded HMAC keys from BuildConfig
// The system will generate keys automatically
```

### Step 4: Verify Security
```kotlin
// Check that secure HMAC is working
val strongBoxAvailable = SecureHmacHelper.isStrongBoxAvailableForHmac()
Log.i("Security", "StrongBox available: $strongBoxAvailable")
```

## Conclusion

The secure HMAC implementation provides significant security improvements over legacy string-based keys:

- **Hardware Protection**: Keys stored in secure hardware
- **Device Binding**: Unique keys per device
- **Tamper Detection**: Automatic verification
- **Backward Compatibility**: Easy migration path

This implementation is recommended for all production applications requiring configuration integrity verification.
