# راهنمای کامل APK HMAC Protection - مرحله به مرحله

## 📋 فهرست مطالب
1. [مقدمه و هدف](#مقدمه-و-هدف)
2. [مرحله 1: آماده‌سازی پروژه](#مرحله-1-آماده‌سازی-پروژه)
3. [مرحله 2: نصب و پیکربندی](#مرحله-2-نصب-و-پیکربندی)
4. [مرحله 3: تولید APK](#مرحله-3-تولید-apk)
5. [مرحله 4: تولید HMAC Signature](#مرحله-4-تولید-hmac-signature)
6. [مرحله 5: ادغام در اپلیکیشن](#مرحله-5-ادغام-در-اپلیکیشن)
7. [مرحله 6: تست و تأیید](#مرحله-6-تست-و-تأیید)
8. [مرحله 7: استقرار در تولید](#مرحله-7-استقرار-در-تولید)
9. [عیب‌یابی و مشکلات رایج](#عیب‌یابی-و-مشکلات-رایج)
10. [بهترین روش‌ها](#بهترین-روش‌ها)

---

## مقدمه و هدف

**APK HMAC Protection System** یک سیستم امنیتی پیشرفته است که از اپلیکیشن شما در برابر **repackaging** و **tampering** محافظت می‌کند.

### 🎯 اهداف:
- جلوگیری از تغییر APK توسط مهاجمان
- تشخیص نصب از منابع غیرمجاز
- تأیید اصالت اپلیکیشن در runtime
- استفاده از سخت‌افزار امنیتی دستگاه

---

## مرحله 1: آماده‌سازی پروژه

### 1.1 بررسی پیش‌نیازها

```bash
# بررسی نسخه Java
java -version
# باید Java 8 یا بالاتر باشد

# بررسی نسخه Python
python3 --version
# باید Python 3.6 یا بالاتر باشد

# بررسی دسترسی به فایل‌ها
ls -la scripts/
```

### 1.2 ساختار پروژه

```
your-project/
├── app/
│   ├── src/main/
│   │   ├── assets/          # اینجا signature ذخیره می‌شود
│   │   ├── java/
│   │   └── res/
│   └── build.gradle
├── scripts/                 # اسکریپت‌های HMAC
└── docs/                   # مستندات
```

### 1.3 اضافه کردن dependency

در فایل `app/build.gradle`:

```gradle
dependencies {
    implementation 'com.miaadrajabi.securitymodule:securitymodule:1.0.0'
    // یا اگر local module است:
    implementation project(':securitymodule')
}
```

---

## مرحله 2: نصب و پیکربندی

### 2.1 کپی کردن اسکریپت‌ها

```bash
# کپی اسکریپت‌ها به پروژه شما
cp -r /path/to/PosSecurity/scripts/ ./scripts/
chmod +x scripts/*.sh
chmod +x scripts/*.py
```

### 2.2 تست اسکریپت‌ها

```bash
# تست اسکریپت Python
python3 scripts/generate_apk_hmac.py --help

# تست اسکریپت Shell
./scripts/sign_apk_with_hmac.sh --help
```

### 2.3 پیکربندی Gradle

در فایل `app/build.gradle`:

```gradle
android {
    // ... سایر تنظیمات
    
    buildTypes {
        release {
            // ... سایر تنظیمات
            
            // اضافه کردن task برای تولید HMAC
            doLast {
                exec {
                    commandLine 'bash', '../scripts/sign_apk_with_hmac.sh', 
                        "${buildDir}/outputs/apk/release/app-release.apk",
                        '-a', 'src/main/assets/',
                        '-v'
                }
            }
        }
    }
}
```

---

## مرحله 3: تولید APK

### 3.1 Build کردن پروژه

```bash
# Clean کردن پروژه
./gradlew clean

# Build کردن APK
./gradlew assembleRelease

# یا برای debug
./gradlew assembleDebug
```

### 3.2 بررسی APK تولید شده

```bash
# بررسی وجود APK
ls -la app/build/outputs/apk/release/

# بررسی اندازه APK
du -h app/build/outputs/apk/release/app-release.apk
```

### 3.3 مسیر APK

APK شما در این مسیر قرار دارد:
```
app/build/outputs/apk/release/app-release.apk
```

---

## مرحله 4: تولید HMAC Signature

### 4.1 روش دستی

```bash
# تولید signature با اسکریپت Shell
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk

# یا با Python
python3 scripts/generate_apk_hmac.py app/build/outputs/apk/release/app-release.apk
```

### 4.2 روش خودکار (توصیه شده)

```bash
# اجرای build با تولید خودکار signature
./gradlew assembleRelease
```

### 4.3 بررسی signature تولید شده

```bash
# بررسی فایل signature
ls -la app/src/main/assets/apk_hmac_signature.txt

# محتوای signature
cat app/src/main/assets/apk_hmac_signature.txt
```

### 4.4 خروجی مورد انتظار

```
✅ APK HMAC signing completed successfully!
📁 Output file: app-release_hmac_signature.txt
📊 APK hash: a1b2c3d4e5f6...
🔏 HMAC signature: 680ddcb08678f2932bf01672f9671ec0bdba1a6bdb161e989235c9981c3289c9
```

---

## مرحله 5: ادغام در اپلیکیشن

### 5.1 اضافه کردن import

در فایل `MainActivity.kt` یا کلاس اصلی:

```kotlin
import com.miaadrajabi.securitymodule.crypto.ApkHmacProtector
import kotlinx.coroutines.*
```

### 5.2 اضافه کردن کد تأیید در onCreate

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // تأیید APK integrity در startup
        verifyApkIntegrity()
    }
    
    private fun verifyApkIntegrity() {
        lifecycleScope.launch {
            try {
                // تأیید integrity
                val integrityInfo = ApkHmacProtector.verifyApkIntegrity(this@MainActivity)
                
                if (!integrityInfo.isIntegrityValid) {
                    // APK تغییر کرده - اقدام امنیتی
                    handleSecurityBreach()
                    return@launch
                }
                
                // بررسی repackaging
                val isRepackaged = ApkHmacProtector.detectRepackaging(this@MainActivity)
                
                if (isRepackaged) {
                    // APK repackaged شده - اقدام امنیتی
                    handleRepackagingDetected()
                    return@launch
                }
                
                // همه چیز OK
                Log.d("APK", "APK integrity verified successfully")
                
            } catch (e: Exception) {
                Log.e("APK", "APK integrity check failed", e)
                // در صورت خطا، اقدام امنیتی
                handleSecurityError()
            }
        }
    }
    
    private fun handleSecurityBreach() {
        // اقدامات امنیتی در صورت تغییر APK
        Toast.makeText(this, "Security breach detected!", Toast.LENGTH_LONG).show()
        // می‌توانید اپلیکیشن را ببندید یا به صفحه امنیتی بروید
        finish()
    }
    
    private fun handleRepackagingDetected() {
        // اقدامات امنیتی در صورت repackaging
        Toast.makeText(this, "Repackaging detected!", Toast.LENGTH_LONG).show()
        finish()
    }
    
    private fun handleSecurityError() {
        // اقدامات امنیتی در صورت خطا
        Toast.makeText(this, "Security check failed!", Toast.LENGTH_LONG).show()
        finish()
    }
}
```

### 5.3 اضافه کردن بررسی دوره‌ای

```kotlin
class MainActivity : AppCompatActivity() {
    
    private val securityCheckInterval = 30000L // 30 ثانیه
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // تأیید اولیه
        verifyApkIntegrity()
        
        // بررسی دوره‌ای
        startPeriodicSecurityCheck()
    }
    
    private fun startPeriodicSecurityCheck() {
        lifecycleScope.launch {
            while (isActive) {
                delay(securityCheckInterval)
                
                try {
                    val isRepackaged = ApkHmacProtector.detectRepackaging(this@MainActivity)
                    if (isRepackaged) {
                        handleRepackagingDetected()
                        break
                    }
                } catch (e: Exception) {
                    Log.e("APK", "Periodic security check failed", e)
                }
            }
        }
    }
}
```

---

## مرحله 6: تست و تأیید

### 6.1 تست اولیه

```bash
# Build و نصب اپلیکیشن
./gradlew installRelease

# یا برای debug
./gradlew installDebug
```

### 6.2 تست عملکرد

1. **تست موفقیت‌آمیز:**
   - اپلیکیشن باید بدون مشکل اجرا شود
   - لاگ‌ها باید "APK integrity verified successfully" را نشان دهند

2. **تست تغییر APK:**
   - APK را تغییر دهید (مثلاً با apktool)
   - اپلیکیشن باید خطای امنیتی نشان دهد

### 6.3 تست با دکمه APK HMAC Protection

در اپلیکیشن نمونه:
1. دکمه "🛡️ APK HMAC Protection" را فشار دهید
2. نتایج را در TestResultsActivity بررسی کنید

### 6.4 بررسی لاگ‌ها

```bash
# بررسی لاگ‌های اپلیکیشن
adb logcat | grep "ApkHmacProtector"

# یا
adb logcat | grep "APK"
```

---

## مرحله 7: استقرار در تولید

### 7.1 Build نهایی

```bash
# Clean و build کامل
./gradlew clean
./gradlew assembleRelease

# بررسی APK نهایی
ls -la app/build/outputs/apk/release/
```

### 7.2 تأیید signature

```bash
# بررسی وجود signature
cat app/src/main/assets/apk_hmac_signature.txt

# بررسی محتوای APK
unzip -l app/build/outputs/apk/release/app-release.apk | grep signature
```

### 7.3 تست نهایی

```bash
# نصب و تست
adb install -r app/build/outputs/apk/release/app-release.apk

# اجرای اپلیکیشن و بررسی عملکرد
adb shell am start -n com.yourpackage/.MainActivity
```

### 7.4 انتشار

```bash
# آپلود به Google Play Store یا سایر پلتفرم‌ها
# APK با signature محافظت شده آماده است
```

---

## عیب‌یابی و مشکلات رایج

### مشکل 1: اسکریپت اجرا نمی‌شود

```bash
# بررسی دسترسی
ls -la scripts/sign_apk_with_hmac.sh

# تغییر دسترسی
chmod +x scripts/sign_apk_with_hmac.sh

# تست اجرا
./scripts/sign_apk_with_hmac.sh --help
```

### مشکل 2: Python script خطا می‌دهد

```bash
# بررسی نسخه Python
python3 --version

# نصب dependencies
pip3 install hashlib hmac

# تست script
python3 scripts/generate_apk_hmac.py --help
```

### مشکل 3: Signature تولید نمی‌شود

```bash
# بررسی مسیر APK
ls -la app/build/outputs/apk/release/app-release.apk

# بررسی دسترسی به فایل
file app/build/outputs/apk/release/app-release.apk

# اجرای دستی
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk -v
```

### مشکل 4: اپلیکیشن crash می‌کند

```kotlin
// اضافه کردن try-catch
try {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
    // ...
} catch (e: Exception) {
    Log.e("APK", "Integrity check failed", e)
    // اقدام مناسب
}
```

### مشکل 5: Signature پیدا نمی‌شود

```kotlin
// بررسی وجود فایل signature
val signatureFile = File("${context.filesDir}/apk_hmac_signature.txt")
if (!signatureFile.exists()) {
    Log.e("APK", "Signature file not found")
    // تولید signature یا استفاده از fallback
}
```

---

## بهترین روش‌ها

### 1. امنیت

```kotlin
// همیشه در background thread اجرا کنید
lifecycleScope.launch(Dispatchers.IO) {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
    // ...
}

// از hardware-backed keys استفاده کنید
val key = SecureHmacHelper.getBestAvailableHmacKey()
```

### 2. عملکرد

```kotlin
// بررسی دوره‌ای را بهینه کنید
private val securityCheckInterval = 60000L // 1 دقیقه

// از cache استفاده کنید
private var lastIntegrityCheck = 0L
private val integrityCheckInterval = 300000L // 5 دقیقه
```

### 3. مدیریت خطا

```kotlin
// مدیریت graceful برای خطاها
private fun handleSecurityError() {
    // لاگ کردن
    Log.e("APK", "Security check failed")
    
    // اطلاع‌رسانی به کاربر
    showSecurityWarning()
    
    // اقدام امنیتی
    // finish() یا redirect به صفحه امنیتی
}
```

### 4. تست

```kotlin
// تست‌های واحد
@Test
fun testApkIntegrity() {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
    assertTrue(integrityInfo.isIntegrityValid)
}

// تست‌های integration
@Test
fun testRepackagingDetection() {
    val isRepackaged = ApkHmacProtector.detectRepackaging(context)
    assertFalse(isRepackaged)
}
```

### 5. CI/CD

```yaml
# GitHub Actions
- name: Build APK
  run: ./gradlew assembleRelease

- name: Generate HMAC Signature
  run: ./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk

- name: Test APK
  run: ./gradlew test
```

---

## خلاصه مراحل

1. ✅ **آماده‌سازی**: نصب dependencies و اسکریپت‌ها
2. ✅ **پیکربندی**: تنظیم Gradle و مسیرها
3. ✅ **تولید APK**: build کردن پروژه
4. ✅ **تولید Signature**: اجرای اسکریپت HMAC
5. ✅ **ادغام**: اضافه کردن کد تأیید
6. ✅ **تست**: بررسی عملکرد
7. ✅ **استقرار**: انتشار نهایی

---

## پشتیبانی

در صورت بروز مشکل:
1. لاگ‌ها را بررسی کنید
2. مستندات را مطالعه کنید
3. تست‌های نمونه را اجرا کنید
4. با تیم توسعه تماس بگیرید

**🎉 حالا APK شما با HMAC محافظت شده و در برابر repackaging امن است!**
