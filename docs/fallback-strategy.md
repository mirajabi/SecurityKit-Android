# HMAC Fallback Strategy

## 🎯 Smart fallback strategy

### **📋 Key selection priority:**

```
1. StrongBox (API 28+) — Highest security
2. TEE (API 23+) — Hardware-backed security
3. Software Keys — Software fallback
```

## 🔄 How it works

### **1. StrongBox (top priority):**
```kotlin
// If API 28+ and StrongBox is available
if (Build.VERSION.SDK_INT >= 28) {
    try {
        return getOrCreateStrongBoxHmacKey()
    } catch (e: Exception) {
        // StrongBox failed, continue to TEE
    }
}
```

**✅ Pros:**
- Highest security level
- Keys in dedicated HSM
- Resistant to physical attacks

**❌ Cons:**
- Mostly on flagships (~15% of devices)
- Requires API 28+
- Potentially slower performance

### **2. TEE (second priority):**
```kotlin
// Fallback to TEE (hardware-backed)
val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
val spec = KeyGenParameterSpec.Builder(
    HMAC_KEY_ALIAS,
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .build()
```

**✅ Pros:**
- Hardware-backed security
- Supported by ~85% of modern devices
- Good performance

**❌ Cons:**
- Requires API 23+
- May be limited on some devices

### **3. Software Keys (final fallback):**
```kotlin
// Final fallback to software keys
private fun generateSoftwareHmacKey(): SecretKey {
    val keyGenerator = KeyGenerator.getInstance("AES")
    keyGenerator.init(256)
    return keyGenerator.generateKey()
}
```

**✅ Pros:**
- Always works
- 100% compatibility
- Fast

**❌ Cons:**
- Lower security
- Keys live in app memory

## 📊 Compatibility matrix

| Device | StrongBox | TEE | Software | Strategy |
|--------|-----------|-----|----------|----------|
| **Samsung S24** | ✅ | ✅ | ✅ | StrongBox → TEE → Software |
| **Samsung A14** | ❌ | ✅ | ✅ | TEE → Software |
| **Pixel 8** | ✅ | ✅ | ✅ | StrongBox → TEE → Software |
| **OnePlus Nord** | ❌ | ✅ | ✅ | TEE → Software |
| **Xiaomi Redmi** | ❌ | ⚠️ | ✅ | Software |
| **Android 5.1** | ❌ | ❌ | ✅ | Software |

## 🔍 Detecting key type

### **At runtime:**
```kotlin
val (key, keyType) = SecureHmacHelper.getBestAvailableHmacKey()
when (keyType) {
    "StrongBox" -> log("✅ Using StrongBox (highest security)")
    "TEE" -> log("✅ Using TEE (hardware-backed)")
    "Software" -> log("⚠️ Using Software keys (fallback)")
}
```

### **Fallback strategy info:**
```kotlin
val info = SecureHmacHelper.getFallbackStrategyInfo()
log("Recommended: ${info["recommended_strategy"]}")
log("StrongBox available: ${info["strongbox_available"]}")
log("TEE available: ${info["tee_available"]}")
```

## 🎯 Test results

### **Flagship devices:**
```
🔄 Fallback Strategy:
   Recommended: StrongBox → TEE → Software
   StrongBox available: true
   TEE available: true
   Key type used: StrongBox
   ✅ Using StrongBox (highest security)
```

### **Mid-range devices:**
```
🔄 Fallback Strategy:
   Recommended: TEE → Software
   StrongBox available: false
   TEE available: true
   Key type used: TEE
   ✅ Using TEE (hardware-backed)
```

### **Legacy devices:**
```
🔄 Fallback Strategy:
   Recommended: Software only
   StrongBox available: false
   TEE available: false
   Key type used: Software
   ⚠️ Using Software keys (fallback)
```

## 💡 Practical tips

### **For development:**
1. ✅ Always test the fallback path
2. ✅ Test on multiple device classes
3. ✅ Implement comprehensive error handling

### **For production:**
1. ✅ Prefer StrongBox when available
2. ✅ Use TEE as fallback
3. ✅ Use software keys as last resort

### **For maximum compatibility:**
1. ✅ Always keep fallback mechanisms
2. ✅ Implement graceful degradation
3. ✅ Provide user-friendly feedback

## 🔧 Production sample code
```kotlin
class SecureHmacManager {
    fun getSecureHmacKey(): SecretKey {
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                try { return SecureHmacHelper.getOrCreateStrongBoxHmacKey() }
                catch (e: Exception) { log("StrongBox failed: ${e.message}") }
            }
            if (Build.VERSION.SDK_INT >= 23) {
                try { return SecureHmacHelper.getOrCreateSecureHmacKey() }
                catch (e: Exception) { log("TEE failed: ${e.message}") }
            }
            SecureHmacHelper.generateSoftwareHmacKey()
        } catch (e: Exception) {
            log("All methods failed: ${e.message}")
            throw SecurityException("Unable to generate secure HMAC key")
        }
    }
    fun getKeyTypeInfo(): String {
        val (_, keyType) = SecureHmacHelper.getBestAvailableHmacKey()
        return when (keyType) {
            "StrongBox" -> "Maximum security with StrongBox"
            "TEE" -> "Hardware-backed security with TEE"
            "Software" -> "Software-based security (fallback)"
            else -> "Unknown key type"
        }
    }
}
```

## 📈 Summary

**🎯 Smart strategy:**
- **StrongBox** for highest security (~15% of devices)
- **TEE** for hardware-backed security (~85% of devices)
- **Software** for 100% compatibility

**✅ Benefits:**
- Optimal security per device
- Full compatibility
- Good performance
- Graceful degradation

**🚀 Result:** Each device gets the best possible security level!
