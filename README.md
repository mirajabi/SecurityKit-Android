# SecurityModule - کتابخانه امنیت Android

کتابخانه‌ای حرفه‌ای برای محافظت از اپلیکیشن‌های بانکی، فین‌تک و POS در برابر تهدیدات امنیتی.

## ویژگی‌ها

### 🛡️ تشخیص دستگاه
- **Root Detection**: تشخیص حرفه‌ای روت با چندین روش
- **Emulator Detection**: شناسایی امولاتورها (Android Studio، Genymotion، BlueStacks)
- **Debugger Detection**: تشخیص دیباگر متصل
- **USB Debug**: بررسی فعال بودن USB Debugging
- **VPN Detection**: تشخیص اتصال VPN
- **Developer Options**: بررسی فعال بودن گزینه‌های توسعه‌دهنده

### 🔐 یکپارچگی اپلیکیشن
- **Signature Verification**: بررسی امضای دیجیتال اپلیکیشن
- **Repackaging Detection**: تشخیص بسته‌بندی مجدد اپ
- **File Integrity**: بررسی یکپارچگی فایل‌های حیاتی

### 🚫 ضد دستکاری
- **Anti-Hooking**: تشخیص ابزارهای hooking (Frida, Xposed)
- **Anti-Debugging**: محافظت در برابر تحلیل‌گرهای پویا
- **Screen Capture Protection**: جلوگیری از اسکرین‌شات و ضبط صفحه

### 🔒 رمزنگاری
- **Hashing**: SHA-256/384/512، مقایسه constant-time
- **Symmetric Encryption**: AES-GCM، AES-CBC
- **Asymmetric Encryption**: RSA-OAEP
- **Android Keystore**: یکپارچگی با Keystore سیستم
- **Certificate Pinning**: پینینگ گواهی برای HTTPS

### ⚙️ سیستم پیکربندی
- **JSON Configuration**: تنظیمات قابل تغییر در runtime
- **Policy Engine**: تعریف واکنش‌ها (Allow/Warn/Block/Terminate)
- **Model Overrides**: مدیریت استثناها برای مدل‌های خاص
- **Telemetry**: گزارش‌دهی رویدادها بدون افشای اطلاعات شخصی

## نصب و راه‌اندازی

### 1. افزودن وابستگی

```gradle
dependencies {
    implementation project(':securitymodule')
}
```

### 2. تنظیمات Proguard/R8

قوانین consumer به صورت خودکار اعمال می‌شوند. برای تنظیمات اضافی:

```proguard
-keep class com.miaadrajabi.securitymodule.** { *; }
```

### 3. مجوزهای لازم

مجوزهای زیر به صورت خودکار در Manifest اضافه می‌شوند:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

## پیکربندی

### ایجاد فایل کانفیگ

فایل `security_config.json` را در `assets` قرار دهید:

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
    "repackagingDetection": true
  },
  "thresholds": {
    "emulatorSignalsToBlock": 2,
    "rootSignalsToBlock": 2
  },
  "overrides": {
    "allowedModels": ["SM-G973F"],
    "deniedModels": [],
    "allowedBrands": ["samsung"],
    "deniedBrands": ["unknown"]
  },
  "policy": {
    "onRoot": "BLOCK",
    "onEmulator": "BLOCK",
    "onDebugger": "WARN",
    "onUsbDebug": "WARN",
    "onVpn": "WARN",
    "onMitm": "BLOCK"
  },
  "appIntegrity": {
    "expectedPackageName": "com.yourcompany.yourapp",
    "expectedSignatureSha256": [
      "A1:B2:C3:D4:E5:F6:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90"
    ]
  },
  "telemetry": {
    "enabled": true
  }
}
```

### تنظیمات Policy

#### اعمال سیاست‌ها:
- **ALLOW**: اجازه ادامه
- **WARN**: هشدار (بدون مسدودسازی)
- **DEGRADE**: محدودسازی ویژگی‌ها
- **BLOCK**: هدایت به صفحه مسدود
- **TERMINATE**: خروج از اپلیکیشن

#### آستانه‌ها (Thresholds):
- `emulatorSignalsToBlock`: تعداد سیگنال‌های امولاتور برای مسدودسازی
- `rootSignalsToBlock`: تعداد سیگنال‌های روت برای مسدودسازی

## تنظیم امضای دیجیتال (Signature Check)

### 1. استخراج امضای فعلی

```kotlin
val signatures = SignatureVerifier.currentSigningSha256(context)
signatures.forEach { signature ->
    Log.d("Signature", "SHA256: $signature")
}
```

### 2. تنظیم در کانفیگ

امضاهای استخراج شده را در `security_config.json` قرار دهید:

```json
{
  "appIntegrity": {
    "expectedPackageName": "com.yourcompany.yourapp",
    "expectedSignatureSha256": [
      "1A2B3C4D5E6F7890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890",
      "FEDCBA0987654321FEDCBA0987654321FEDCBA0987654321FEDCBA0987654321"
    ]
  },
  "features": {
    "appSignatureVerification": true
  }
}
```

### 3. تولید امضا برای محیط‌های مختلف

#### Debug Keystore:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### Release Keystore:
```bash
keytool -list -v -keystore your-release-key.keystore -alias your-key-alias
```

### 4. بررسی خودکار در کد

```kotlin
// بررسی امضا در runtime
val actualSignatures = SignatureVerifier.currentSigningSha256(context)
val expectedSignatures = config.appIntegrity.expectedSignatureSha256

val isValid = actualSignatures.any { actual ->
    expectedSignatures.any { expected ->
        expected.equals(actual, ignoreCase = true)
    }
}

if (!isValid) {
    // اقدام امنیتی
}
```

## استفاده در کد

### راه‌اندازی پایه

```kotlin
class MainActivity : Activity() {
    private val telemetry = object : TelemetrySink {
        override fun onEvent(eventId: String, attributes: Map<String, String>) {
            // ارسال به سیستم telemetry شما
            Log.d("Security", "Event: $eventId, Attrs: $attributes")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // بارگذاری کانفیگ
        val config = SecurityConfigLoader.fromAsset(this)
        
        // ایجاد ماژول امنیت
        val securityModule = SecurityModule.Builder(applicationContext)
            .setConfig(config)
            .setTelemetry(telemetry)
            .build()

        // اجرای بررسی‌های امنیتی
        val report = securityModule.runAllChecksBlocking()
        
        // بررسی نتیجه
        when (report.overallSeverity) {
            Severity.OK -> {
                // ادامه عملیات عادی
                setupNormalUI()
            }
            Severity.WARN -> {
                // نمایش هشدار
                showWarningBanner(report.findings)
                setupNormalUI()
            }
            Severity.BLOCK -> {
                // هدایت به صفحه مسدود (خودکار انجام می‌شود)
                return
            }
        }
    }
}
```

### بررسی‌های async

```kotlin
lifecycleScope.launch {
    val report = securityModule.runAllChecks()
    handleSecurityReport(report)
}
```

### محافظت از اسکرین‌شات

```kotlin
// فعال‌سازی محافظت از اسکرین‌شات
if (config.features.screenCaptureProtection) {
    ScreenCaptureProtector.applySecureFlag(this)
    
    // مانیتورینگ تلاش‌های اسکرین‌شات
    val monitor = ScreenCaptureMonitor(this)
    monitor.start { type, uri ->
        // نمایش overlay محافظ
        ScreenCaptureProtector.showWhiteOverlay(this@Activity)
        
        // گزارش رویداد
        telemetry.onEvent("screenshot_attempt", mapOf("type" to type.name))
    }
}
```

### استفاده از رمزنگاری

```kotlin
// تولید کلید AES با Keystore
val key = KeystoreHelper.getOrCreateAesKey("my_secure_key")

// رمزنگاری AES-GCM
val plaintext = "داده‌های حساس".toByteArray()
val (iv, ciphertext) = CryptoUtils.encryptAesGcm(key, plaintext)

// رمزگشایی
val decrypted = CryptoUtils.decryptAesGcm(key, iv, ciphertext)

// هشینگ
val hash = CryptoUtils.sha256(plaintext)

// مقایسه امن
val isEqual = CryptoUtils.constantTimeEquals(hash1, hash2)
```

### Certificate Pinning

```kotlin
// ایجاد کلاینت HTTP با پینینگ
val pinnedClient = SecurityHttp.createPinnedClient(
    hostname = "api.mybank.com",
    sha256Pins = listOf(
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
    )
)

// استفاده در Retrofit
val retrofit = Retrofit.Builder()
    .client(pinnedClient)
    .baseUrl("https://api.mybank.com/")
    .build()
```

### ذخیره‌سازی رمزنگاری شده

```kotlin
// ایجاد SharedPreferences رمزنگاری شده
val securePrefs = EncryptedPreferences.create(context, "secure_data")

// ذخیره داده
securePrefs.putString("sensitive_token", userToken)

// بازیابی داده
val token = securePrefs.getString("sensitive_token", null)
```

## تشخیص تهدیدات

### Root Detection

کتابخانه از روش‌های زیر برای تشخیص روت استفاده می‌کند:

- بررسی فایل‌های `su` در مسیرهای مختلف
- تشخیص پکیج‌های مدیریت روت (Magisk, SuperSU)
- بررسی خصوصیات سیستم (`ro.debuggable`, `ro.secure`)
- اجرای دستورات `su` و `which su`
- تشخیص BusyBox و فلگ‌های mount
- بررسی Build.TAGS برای `test-keys`

### Emulator Detection

تشخیص امولاتور با روش‌های:

- خصوصیات Build (FINGERPRINT, MODEL, BRAND, HARDWARE)
- فایل‌های مخصوص امولاتور (`/dev/qemu_pipe`, `/dev/vboxguest`)
- خصوصیات سیستم (`ro.kernel.qemu`)
- آدرس IP پیش‌فرض امولاتور (10.0.2.15)
- تعداد سنسورهای کم
- تشخیص Genymotion و VirtualBox

### Hooking Detection

تشخیص ابزارهای hooking:

- اسکن `/proc/self/maps` برای کتابخانه‌های مشکوک
- تشخیص Frida, Xposed, LSPosed
- بررسی TracerPID در `/proc/self/status`

## مدیریت خطاها

```kotlin
try {
    val report = securityModule.runAllChecksBlocking()
    handleReport(report)
} catch (e: SecurityException) {
    // مدیریت خطاهای امنیتی
    Log.e("Security", "Security check failed", e)
    // اقدام احتیاطی
    fallbackSecurity()
}
```

## بهترین روش‌ها

### 1. تنظیم آستانه‌ها
- آستانه‌های پایین: حساسیت بالا، false positive بیشتر
- آستانه‌های بالا: حساسیت کم، false negative بیشتر

### 2. مدیریت Model Overrides
```json
{
  "overrides": {
    "allowedModels": ["SM-G973F", "Pixel 4"],
    "deniedModels": ["generic"],
    "allowedBrands": ["samsung", "google"],
    "deniedBrands": ["unknown", "generic"]
  }
}
```

### 3. Telemetry و Monitoring
```kotlin
private val telemetry = object : TelemetrySink {
    override fun onEvent(eventId: String, attributes: Map<String, String>) {
        // ارسال به سیستم monitoring
        Analytics.track(eventId, attributes)
        
        // لاگ محلی برای debug
        if (BuildConfig.DEBUG) {
            Log.d("SecurityTelemetry", "$eventId: $attributes")
        }
    }
}
```

### 4. مدیریت چرخه حیات
```kotlin
class SecureActivity : Activity() {
    private var captureMonitor: ScreenCaptureMonitor? = null
    
    override fun onResume() {
        super.onResume()
        // شروع مانیتورینگ
        captureMonitor?.start { type, uri ->
            handleCaptureAttempt(type)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // توقف مانیتورینگ
        captureMonitor?.stop()
    }
}
```

## عیب‌یابی

### فعال‌سازی لاگ‌های تشخیصی

```kotlin
// در محیط debug
if (BuildConfig.DEBUG) {
    val verboseTelemetry = object : TelemetrySink {
        override fun onEvent(eventId: String, attributes: Map<String, String>) {
            Log.v("SecurityDebug", "Event: $eventId")
            attributes.forEach { (key, value) ->
                Log.v("SecurityDebug", "  $key: $value")
            }
        }
    }
}
```

### بررسی امضاها

```kotlin
// چاپ امضای فعلی برای تنظیم کانفیگ
val currentSignatures = SignatureVerifier.currentSigningSha256(this)
Log.d("SignatureDebug", "Current signatures:")
currentSignatures.forEach { signature ->
    Log.d("SignatureDebug", signature)
}
```

### تست در محیط‌های مختلف

1. **دستگاه واقعی**: تست عملکرد طبیعی
2. **امولاتور**: بررسی تشخیص امولاتور
3. **دستگاه روت شده**: تست تشخیص روت
4. **با VPN**: بررسی تشخیص VPN

## محدودیت‌ها و نکات

### محدودیت‌های فنی
- برخی امولاتورها ممکن است FLAG_SECURE را دور بزنند
- تشخیص روت در برخی روش‌های جدید ممکن است محدود باشد
- دسترسی SYSTEM_ALERT_WINDOW در Android 10+ نیاز به اجازه کاربر دارد

### نکات امنیتی
- امضاها را هرگز در کد سخت‌کد نکنید
- از obfuscation برای محافظت از منطق امنیتی استفاده کنید
- به‌روزرسانی منظم کتابخانه برای مقابله با تهدیدات جدید

## پشتیبانی

- **حداقل SDK**: 21 (Android 5.0)
- **هدف SDK**: 34 (Android 14)
- **زبان هدف**: Java 8 / Kotlin
- **معماری**: ARM64, ARM, x86, x86_64

## مجوز

این کتابخانه تحت مجوز MIT منتشر شده است.

---

برای سوالات بیشتر یا گزارش مشکلات، لطفاً با تیم توسعه تماس بگیرید.