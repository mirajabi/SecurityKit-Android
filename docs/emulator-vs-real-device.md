# Android Keystore: Emulator vs Real Device

## 📱 Key questions

### ❓ Can we test Secure HMAC on an emulator?

**✅ Yes, with limitations:**

#### **🟢 What works on emulators:**
- ✅ Android Keystore API (software-backed)
- ✅ Standard AES keys
- ✅ HMAC computation and verification
- ✅ Configuration loading with secure HMAC
- ✅ Basic security tests

#### **🔴 What is limited on emulators:**
- ❌ TEE (Trusted Execution Environment) — hardware
- ❌ StrongBox — hardware
- ❌ Hardware-backed keys
- ❌ Real device binding
- ❌ User authentication binding

### ❓ Do some real devices lack these capabilities?

**⚠️ Yes, absolutely:**

#### **📊 Compatibility summary:**

| Feature | API Level | Coverage | Notes |
|--------|-----------|----------|-------|
| **Android Keystore** | 18+ | 99% | Almost all devices |
| **TEE Support** | 23+ | 85% | Most modern devices |
| **StrongBox** | 28+ | 15% | Mostly flagships |
| **User Auth Binding** | 23+ | 90% | If biometric available |
| **Device Binding** | 23+ | 80% | Manufacturer dependent |

#### **🔍 Devices likely to have issues:**

1. **Legacy devices (API < 23):**
   - Android 5.1 and earlier
   - Fallback to software keys

2. **Low-cost devices:**
   - May lack TEE
   - Software keystore only

3. **Rooted devices:**
   - Keystore may be compromised
   - Requires robust fallbacks

4. **Custom ROMs:**
   - Keystore implementations may vary
   - Requires extra testing

## 🛡️ Implemented solutions

### **1. Fallback Strategy:**
```kotlin
@JvmStatic
fun getOrCreateSecureHmacKey(): SecretKey {
    return if (Build.VERSION.SDK_INT >= 23) {
        try {
            // Try Android Keystore first
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            // ... key generation
        } catch (e: Exception) {
            // Fallback to software-generated key
            generateSoftwareHmacKey()
        }
    } else {
        // Old Android versions
        generateSoftwareHmacKey()
    }
}
```

### **2. Capability Detection:**
```kotlin
@JvmStatic
fun getKeystoreCapabilities(): Map<String, Any> {
    val capabilities = mutableMapOf<String, Any>()
    capabilities["android_version"] = Build.VERSION.SDK_INT
    capabilities["is_emulator"] = isRunningOnEmulator()
    capabilities["keystore_available"] = try {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
        ks.load(null)
        true
    } catch (_: Exception) { false }
    capabilities["tee_support"] = testTeeSupport()
    capabilities["strongbox_support"] = isStrongBoxAvailableForHmac()
    return capabilities
}
```

### **3. Emulator Detection:**
```kotlin
private fun isRunningOnEmulator(): Boolean {
    return try {
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val product = Build.PRODUCT.lowercase()
        val device = Build.DEVICE.lowercase()
        model.contains("google_sdk") ||
        model.contains("emulator") ||
        model.contains("android sdk") ||
        manufacturer.contains("genymotion") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        device.contains("generic") ||
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu")
    } catch (_: Exception) { false }
}
```

## 📋 Recommended tests

### **1. Emulator testing:**
- Android Studio Emulator
- Genymotion
- BlueStacks (testing only)

### **2. Real device testing:**
- Multiple OEMs (Samsung, Huawei, Xiaomi, etc.)
- Various Android versions (7–14)
- Low-end and high-end devices

### **3. Compatibility test:**
```kotlin
private fun runCompatibilityTest() {
    val capabilities = SecureHmacHelper.getKeystoreCapabilities()
    when {
        capabilities["strongbox_support"] == true -> log("🟢 Full security features available")
        capabilities["tee_support"] == true -> log("🟡 TEE available, StrongBox not supported")
        capabilities["keystore_available"] == true -> log("🟡 Software Keystore available")
        else -> log("🔴 Fallback to software keys")
    }
}
```

## 🎯 Practical recommendations

### **For development:**
1. ✅ Use emulators for basic testing
2. ✅ Implement capability detection
3. ✅ Test fallback mechanisms

### **For production:**
1. ✅ Always test on real devices
2. ✅ Test across multiple OEMs and Android versions
3. ✅ Implement robust error handling and logging

### **For maximum compatibility:**
1. ✅ Always have software key fallback
2. ✅ Implement graceful degradation
3. ✅ Provide good user feedback

## 🔧 Production sample code:
```kotlin
class SecureHmacManager(private val context: Context) {
    fun getBestAvailableKey(): SecretKey {
        val capabilities = SecureHmacHelper.getKeystoreCapabilities()
        return when {
            capabilities["strongbox_support"] == true -> SecureHmacHelper.getOrCreateStrongBoxHmacKey()
            capabilities["tee_support"] == true -> SecureHmacHelper.getOrCreateSecureHmacKey()
            capabilities["keystore_available"] == true -> SecureHmacHelper.getOrCreateSecureHmacKey()
            else -> SecureHmacHelper.generateSoftwareHmacKey()
        }
    }
    fun computeSecureHmac(data: ByteArray): String =
        try { SecureHmacHelper.computeHmacSha256(data, getBestAvailableKey()) }
        catch (e: Exception) { computeBasicHmac(data) }
}
```

## 📊 Summary:

**🎯 Takeaways:** 
- Emulators are fine for development
- Real devices are mandatory for production
- Always implement fallback mechanisms
- Perform capability detection before use
