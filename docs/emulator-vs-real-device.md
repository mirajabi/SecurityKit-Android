# Android Keystore: Emulator vs Real Device

## 📱 پاسخ به سوالات مهم

### ❓ آیا می‌توان Secure HMAC را روی Emulator تست کرد؟

**✅ بله، اما با محدودیت‌ها:**

#### **🟢 آنچه روی Emulator کار می‌کند:**
- ✅ Android Keystore API (نرم‌افزاری)
- ✅ کلیدهای AES معمولی
- ✅ HMAC computation و verification
- ✅ Configuration loading با secure HMAC
- ✅ Basic security tests

#### **🔴 آنچه روی Emulator محدود است:**
- ❌ TEE (Trusted Execution Environment) - سخت‌افزاری
- ❌ StrongBox - سخت‌افزاری
- ❌ Hardware-backed keys
- ❌ Device binding واقعی
- ❌ User authentication binding

### ❓ آیا ممکن است بعضی دستگاه‌ها این قابلیت‌ها را نداشته باشند؟

**⚠️ بله، کاملاً ممکن است:**

#### **📊 آمار سازگاری:**

| ویژگی | API Level | پشتیبانی | توضیحات |
|--------|-----------|----------|---------|
| **Android Keystore** | 18+ | 99% | تقریباً همه دستگاه‌ها |
| **TEE Support** | 23+ | 85% | اکثر دستگاه‌های جدید |
| **StrongBox** | 28+ | 15% | فقط دستگاه‌های پرچم‌دار |
| **User Auth Binding** | 23+ | 90% | اگر biometric موجود باشد |
| **Device Binding** | 23+ | 80% | بستگی به manufacturer |

#### **🔍 دستگاه‌هایی که ممکن است مشکل داشته باشند:**

1. **دستگاه‌های قدیمی (API < 23):**
   - Android 5.1 و پایین‌تر
   - Fallback به software keys

2. **دستگاه‌های ارزان قیمت:**
   - ممکن است TEE نداشته باشند
   - Keystore نرم‌افزاری

3. **دستگاه‌های Root شده:**
   - ممکن است Keystore آسیب دیده باشد
   - نیاز به fallback mechanisms

4. **دستگاه‌های Custom ROM:**
   - ممکن است Keystore implementation متفاوت باشد
   - نیاز به testing بیشتر

## 🛡️ راه‌حل‌های پیاده‌سازی شده

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
    
    // Check Android version
    capabilities["android_version"] = Build.VERSION.SDK_INT
    
    // Check if running on emulator
    capabilities["is_emulator"] = isRunningOnEmulator()
    
    // Check Keystore availability
    capabilities["keystore_available"] = try {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
        ks.load(null)
        true
    } catch (e: Exception) {
        false
    }
    
    // Check TEE support
    capabilities["tee_support"] = testTeeSupport()
    
    // Check StrongBox support
    capabilities["strongbox_support"] = isStrongBoxAvailableForHmac()
    
    return capabilities
}
```

### **3. Emulator Detection:**
```kotlin
private fun isRunningOnEmulator(): Boolean {
    return try {
        val buildModel = Build.MODEL.lowercase()
        val buildManufacturer = Build.MANUFACTURER.lowercase()
        val buildProduct = Build.PRODUCT.lowercase()
        val buildDevice = Build.DEVICE.lowercase()
        
        buildModel.contains("google_sdk") ||
        buildModel.contains("emulator") ||
        buildModel.contains("android sdk") ||
        buildManufacturer.contains("genymotion") ||
        buildProduct.contains("sdk") ||
        buildProduct.contains("emulator") ||
        buildDevice.contains("generic") ||
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu")
    } catch (e: Exception) {
        false
    }
}
```

## 📋 تست‌های پیشنهادی

### **1. تست روی Emulator:**
```bash
# برای development و basic testing
- Android Studio Emulator
- Genymotion
- BlueStacks (برای testing)
```

### **2. تست روی Real Device:**
```bash
# برای production testing
- دستگاه‌های مختلف (Samsung, Huawei, Xiaomi, etc.)
- Android versions مختلف (7, 8, 9, 10, 11, 12, 13, 14)
- دستگاه‌های ارزان و گران
```

### **3. تست Compatibility:**
```kotlin
// در MainActivity
private fun runCompatibilityTest() {
    val capabilities = SecureHmacHelper.getKeystoreCapabilities()
    
    when {
        capabilities["strongbox_support"] == true -> {
            log("🟢 Full security features available")
        }
        capabilities["tee_support"] == true -> {
            log("🟡 TEE available, StrongBox not supported")
        }
        capabilities["keystore_available"] == true -> {
            log("🟡 Software Keystore available")
        }
        else -> {
            log("🔴 Fallback to software keys")
        }
    }
}
```

## 🎯 توصیه‌های عملی

### **برای Development:**
1. ✅ از Emulator برای basic testing استفاده کنید
2. ✅ Capability detection را implement کنید
3. ✅ Fallback mechanisms را تست کنید

### **برای Production:**
1. ✅ حتماً روی real devices تست کنید
2. ✅ دستگاه‌های مختلف را test کنید
3. ✅ Error handling جامع پیاده‌سازی کنید
4. ✅ Logging برای debugging اضافه کنید

### **برای Maximum Compatibility:**
1. ✅ همیشه fallback به software keys داشته باشید
2. ✅ Capability detection قبل از استفاده
3. ✅ Graceful degradation
4. ✅ User feedback مناسب

## 🔧 کد نمونه برای Production:

```kotlin
class SecureHmacManager(private val context: Context) {
    
    fun getBestAvailableKey(): SecretKey {
        val capabilities = SecureHmacHelper.getKeystoreCapabilities()
        
        return when {
            capabilities["strongbox_support"] == true -> {
                log("Using StrongBox-backed key")
                SecureHmacHelper.getOrCreateStrongBoxHmacKey()
            }
            capabilities["tee_support"] == true -> {
                log("Using TEE-backed key")
                SecureHmacHelper.getOrCreateSecureHmacKey()
            }
            capabilities["keystore_available"] == true -> {
                log("Using software Keystore key")
                SecureHmacHelper.getOrCreateSecureHmacKey()
            }
            else -> {
                log("Using software-generated key")
                SecureHmacHelper.generateSoftwareHmacKey()
            }
        }
    }
    
    fun computeSecureHmac(data: ByteArray): String {
        return try {
            val key = getBestAvailableKey()
            SecureHmacHelper.computeHmacSha256(data, key)
        } catch (e: Exception) {
            log("HMAC computation failed: ${e.message}")
            // Fallback to basic HMAC
            computeBasicHmac(data)
        }
    }
}
```

## 📊 خلاصه:

| محیط | Keystore | TEE | StrongBox | توصیه |
|------|----------|-----|-----------|--------|
| **Emulator** | ✅ نرم‌افزاری | ❌ | ❌ | Development |
| **Real Device** | ✅ سخت‌افزاری | ✅ | ⚠️ | Production |
| **Old Device** | ✅ نرم‌افزاری | ❌ | ❌ | Fallback |

**🎯 نتیجه‌گیری:** 
- Emulator برای development مناسب است
- Real device برای production ضروری است
- همیشه fallback mechanisms داشته باشید
- Capability detection قبل از استفاده انجام دهید
