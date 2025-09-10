# SecurityModule Usage Guide

## Overview

This guide explains how to use the SecurityModule library to implement comprehensive security checks in your Android application, including device-specific overrides and security policy handling.

## Key Features

### 🔒 **Security Checks**
- **Root Detection**: Multi-signal root detection
- **Emulator Detection**: Comprehensive emulator detection (Genymotion, BlueStacks, Nox, LDPlayer, etc.)
- **Debugger Detection**: Anti-debugging protection
- **USB Debug Detection**: USB debugging status check
- **VPN Detection**: VPN and proxy detection
- **Screen Capture Protection**: Prevents screenshots and screen recording
- **App Integrity**: Signature verification and repackaging detection
- **Play Integrity API**: Google Play Integrity verification (optional)

### 🎯 **Device Overrides**
- **Allowed Models**: Whitelist specific device models
- **Allowed Brands**: Whitelist device brands
- **Allowed Manufacturers**: Whitelist manufacturers (including emulators)
- **Allowed Products**: Whitelist product names
- **Allowed Devices**: Whitelist device names
- **Allowed Boards**: Whitelist board names

### ⚙️ **Security Policies**
- **ALLOW**: Allow the app to continue
- **WARN**: Show warning but allow continuation
- **BLOCK**: Block the app and show warning page
- **TERMINATE**: Terminate the app immediately

## Basic Usage

### 1. Simple Implementation

```kotlin
import com.miaadrajabi.securitymodule.SecurityModule
import com.miaadrajabi.securitymodule.config.SecurityConfigLoader

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load configuration from assets
        val config = SecurityConfigLoader.fromAsset(this, "security_config.json")
        
        // Build SecurityModule
        val securityModule = SecurityModule.Builder(this)
            .setConfig(config)
            .build()
        
        // Run security checks
        val report = securityModule.runAllChecksBlocking()
        
        // Handle results
        when (report.overallSeverity) {
            Severity.OK -> {
                // All checks passed - show normal UI
                setContentView(R.layout.activity_main)
            }
            Severity.WARN -> {
                // Warnings detected - show warnings but allow continuation
                showWarnings(report.findings)
                setContentView(R.layout.activity_main)
            }
            Severity.BLOCK -> {
                // Critical issues - redirect to warning page and exit
                ReportActivity.start(this, report, config)
                finish()
            }
            Severity.INFO -> {
                // Info level - show normal UI
                setContentView(R.layout.activity_main)
            }
        }
    }
}
```

### 2. Device-Specific Configuration

For the **Xiaomi Redmi Note 9** device shown in your image, you can configure it to bypass security checks:

```json
{
  "overrides": {
    "allowedModels": [
      "Redmi Note 9",
      "motion_phone_arm64"
    ],
    "allowedBrands": ["Xiaomi"],
    "allowedManufacturers": ["Xiaomi", "Genymobile"],
    "allowedProducts": ["motion_phone_arm64"],
    "allowedDevices": ["motion_phone_arm64"]
  },
  "policy": {
    "onRoot": "BLOCK",
    "onEmulator": "WARN",
    "onDebugger": "WARN",
    "onUsbDebug": "WARN",
    "onVpn": "WARN",
    "onMitm": "BLOCK"
  }
}
```

## Configuration File Structure

### Complete Configuration Example

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
    "repackagingDetection": true,
    "playIntegrityCheck": false,
    "advancedAppIntegrity": true,
    "strongBoxKeys": true,
    "userAuthBoundKeys": false,
    "deviceBinding": true,
    "configIntegrity": false,
    "tamperEvidence": true
  },
  "thresholds": {
    "emulatorSignalsToBlock": 2,
    "rootSignalsToBlock": 2,
    "playIntegritySignalsToBlock": 1,
    "appIntegritySignalsToBlock": 1
  },
  "overrides": {
    "allowedModels": [
      "Redmi Note 9",
      "motion_phone_arm64",
      "Pixel 6",
      "Galaxy S21"
    ],
    "deniedModels": [],
    "allowedBrands": ["Xiaomi", "Google", "Samsung"],
    "deniedBrands": ["unknown"],
    "allowedManufacturers": ["Xiaomi", "Google", "Samsung", "Genymobile"],
    "allowedProducts": ["motion_phone_arm64"],
    "allowedDevices": ["motion_phone_arm64"],
    "allowedBoards": []
  },
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
  },
  "appIntegrity": {
    "expectedPackageName": "com.yourcompany.yourapp",
    "expectedSignatureSha256": [
      "your_sha256_certificate_hash_here"
    ],
    "allowedInstallers": [
      "com.android.vending",
      "com.huawei.appmarket",
      "com.samsung.android.galaxyapps"
    ]
  }
}
```

## Device Override System

### How Device Overrides Work

1. **Early Check**: Before running any security checks, the system checks if the current device matches any override rules
2. **Allowed Devices**: If the device matches an allowed override, all security checks are bypassed and the app continues normally
3. **Denied Devices**: If the device matches a denied override, the app is immediately blocked
4. **Normal Flow**: If no overrides match, normal security checks are performed

### Device Information Used for Overrides

The system checks these device properties:
- `Build.MODEL` - Device model (e.g., "Redmi Note 9")
- `Build.BRAND` - Device brand (e.g., "Xiaomi")
- `Build.MANUFACTURER` - Manufacturer (e.g., "Xiaomi", "Genymobile")
- `Build.PRODUCT` - Product name (e.g., "motion_phone_arm64")
- `Build.DEVICE` - Device name (e.g., "motion_phone_arm64")
- `Build.BOARD` - Board name

### Example: Genymotion Emulator Configuration

For the Genymotion emulator shown in your image:

```json
{
  "overrides": {
    "allowedModels": ["motion_phone_arm64"],
    "allowedBrands": ["Xiaomi"],
    "allowedManufacturers": ["Genymobile"],
    "allowedProducts": ["motion_phone_arm64"],
    "allowedDevices": ["motion_phone_arm64"]
  }
}
```

This configuration will:
- Allow the Genymotion emulator to bypass all security checks
- Show the normal app UI without any security warnings
- Prevent the app from exiting due to emulator detection

## Security Policy Actions

### Policy Action Types

1. **ALLOW**: Continue with normal app flow
2. **WARN**: Show warning but allow continuation
3. **BLOCK**: Show warning page and exit app
4. **TERMINATE**: Immediately terminate the app

### Policy Configuration

```json
{
  "policy": {
    "onRoot": "BLOCK",           // Block if root detected
    "onEmulator": "WARN",        // Warn if emulator detected
    "onDebugger": "WARN",        // Warn if debugger detected
    "onUsbDebug": "WARN",        // Warn if USB debugging enabled
    "onVpn": "WARN",             // Warn if VPN detected
    "onMitm": "BLOCK",           // Block if MITM attack detected
    "onPlayIntegrityFailure": "WARN",
    "onAppIntegrityFailure": "BLOCK",
    "onConfigTampering": "BLOCK",
    "onStrongBoxUnavailable": "WARN"
  }
}
```

## Advanced Usage Examples

### 1. Custom Device Configuration

```kotlin
fun createCustomConfig(context: Context): SecurityConfig {
    return SecurityConfig(
        features = SecurityConfig.Features(
            rootDetection = true,
            emulatorDetection = true,
            debuggerDetection = true,
            usbDebugDetection = true,
            vpnDetection = true,
            mitmDetection = true,
            screenCaptureProtection = true,
            appSignatureVerification = false, // Disable for testing
            repackagingDetection = true
        ),
        overrides = SecurityConfig.Overrides(
            // Allow specific devices
            allowedModels = listOf("Redmi Note 9", "Pixel 6"),
            allowedBrands = listOf("Xiaomi", "Google"),
            allowedManufacturers = listOf("Xiaomi", "Google", "Genymobile"),
            allowedProducts = listOf("motion_phone_arm64"),
            // Deny specific devices
            deniedModels = listOf("Unknown Device"),
            deniedBrands = listOf("HackerPhone")
        ),
        policy = SecurityConfig.PolicyRules(
            onRoot = SecurityConfig.Action.BLOCK,
            onEmulator = SecurityConfig.Action.WARN,
            onDebugger = SecurityConfig.Action.WARN,
            onUsbDebug = SecurityConfig.Action.WARN,
            onVpn = SecurityConfig.Action.WARN,
            onMitm = SecurityConfig.Action.BLOCK
        )
    )
}
```

### 2. Check if Device is Allowed

```kotlin
fun isDeviceAllowed(context: Context, config: SecurityConfig): Boolean {
    val model = android.os.Build.MODEL ?: ""
    val brand = android.os.Build.BRAND ?: ""
    val manufacturer = android.os.Build.MANUFACTURER ?: ""
    val product = android.os.Build.PRODUCT ?: ""
    val device = android.os.Build.DEVICE ?: ""
    val board = android.os.Build.BOARD ?: ""
    
    return config.overrides.allowedModels.contains(model) ||
           config.overrides.allowedBrands.contains(brand) ||
           config.overrides.allowedManufacturers.contains(manufacturer) ||
           config.overrides.allowedProducts.contains(product) ||
           config.overrides.allowedDevices.contains(device) ||
           config.overrides.allowedBoards.contains(board)
}
```

### 3. Complete Usage with Error Handling

```kotlin
fun completeSecurityCheck(context: Context) {
    try {
        // Load configuration
        val config = SecurityConfigLoader.fromAsset(context, "security_config.json")
        
        // Check if device is in allowed list
        if (isDeviceAllowed(context, config)) {
            println("Device is in allowed list - bypassing security checks")
            // Continue with app flow
            return
        }
        
        // Build SecurityModule
        val securityModule = SecurityModule.Builder(context)
            .setConfig(config)
            .build()
        
        // Run security checks
        val report = securityModule.runAllChecksBlocking()
        
        // Handle results
        when (report.overallSeverity) {
            Severity.OK -> {
                println("✅ All security checks passed")
                // Continue with normal app flow
            }
            Severity.WARN -> {
                println("⚠️ Security warnings detected:")
                report.findings.forEach { finding ->
                    println("  - ${finding.title}: ${finding.severity}")
                }
                // Show warnings but allow continuation
            }
            Severity.BLOCK -> {
                println("🚫 Critical security issues detected:")
                report.findings.forEach { finding ->
                    println("  - ${finding.title}: ${finding.severity}")
                }
                // Redirect to warning page and exit
                ReportActivity.start(context, report, config)
                return
            }
            Severity.INFO -> {
                println("ℹ️ Security info:")
                report.findings.forEach { finding ->
                    println("  - ${finding.title}: ${finding.severity}")
                }
                // Continue with normal app flow
            }
        }
        
    } catch (e: Exception) {
        println("❌ Security check failed: ${e.message}")
        // Handle error - you might want to show an error dialog
        // or fallback to a basic security check
    }
}
```

## Java Usage

The library is fully compatible with Java projects:

```java
import com.miaadrajabi.securitymodule.SecurityModule;
import com.miaadrajabi.securitymodule.SecurityReport;
import com.miaadrajabi.securitymodule.Severity;
import com.miaadrajabi.securitymodule.config.SecurityConfig;
import com.miaadrajabi.securitymodule.config.SecurityConfigLoader;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load configuration
        SecurityConfig config = SecurityConfigLoader.fromAsset(this, "security_config.json");
        
        // Build SecurityModule
        SecurityModule securityModule = new SecurityModule.Builder(this)
            .setConfig(config)
            .build();
        
        // Run security checks
        SecurityReport report = securityModule.runAllChecksBlocking();
        
        // Handle results
        if (report.getOverallSeverity() == Severity.BLOCK) {
            // Critical issues - redirect to warning page
            ReportActivity.start(this, report, config);
            finish();
        } else {
            // Continue with normal app flow
            setContentView(R.layout.activity_main);
        }
    }
}
```

## Best Practices

### 1. Configuration Management
- Store your configuration in `assets/security_config.json`
- Use device overrides for testing and development
- Keep sensitive configuration server-side when possible

### 2. Error Handling
- Always wrap security checks in try-catch blocks
- Provide fallback behavior for configuration loading failures
- Log security events for monitoring

### 3. Testing
- Test on real devices and emulators
- Use device overrides for development environments
- Test all security policy actions

### 4. Performance
- Use `runAllChecksBlocking()` for synchronous checks
- Use `runAllChecks()` for asynchronous checks with coroutines
- Consider caching configuration for better performance

## Troubleshooting

### Common Issues

1. **Configuration Not Found**
   - Ensure `security_config.json` is in the `assets` folder
   - Check file permissions and encoding

2. **Device Not Recognized**
   - Add device information to overrides
   - Check device properties using `Build` class

3. **Security Checks Failing**
   - Review policy configuration
   - Check thresholds and feature flags
   - Verify device override rules

### Debug Information

To debug device information:

```kotlin
fun printDeviceInfo() {
    println("Model: ${Build.MODEL}")
    println("Brand: ${Build.BRAND}")
    println("Manufacturer: ${Build.MANUFACTURER}")
    println("Product: ${Build.PRODUCT}")
    println("Device: ${Build.DEVICE}")
    println("Board: ${Build.BOARD}")
}
```

## Conclusion

The SecurityModule provides comprehensive security protection for Android applications with flexible configuration options. By using device overrides, you can create whitelists for trusted devices while maintaining security for unknown devices. The system is designed to be easy to integrate while providing powerful security features for banking, fintech, and POS applications.
