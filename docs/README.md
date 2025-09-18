# 📚 مستندات APK HMAC Protection System

## 🎯 معرفی

**APK HMAC Protection System** یک سیستم امنیتی پیشرفته برای محافظت از اپلیکیشن‌های Android در برابر **repackaging** و **tampering** است.

## 📖 فهرست مستندات

### 🚀 شروع سریع
- **[راهنمای سریع](apk-hmac-quick-start.md)** - شروع در 5 دقیقه
- **[راهنمای مرحله‌به‌مرحله](apk-hmac-step-by-step-guide.md)** - راهنمای کامل

### 📱 مثال‌های عملی
- **[مثال عملی اپلیکیشن بانکداری](apk-hmac-practical-example.md)** - پیاده‌سازی کامل
- **[مستندات فنی](apk-hmac-protection.md)** - جزئیات فنی

### 🔧 ابزارها و اسکریپت‌ها
- **[اسکریپت Python](generate_apk_hmac.py)** - تولید HMAC signature
- **[اسکریپت Shell](sign_apk_with_hmac.sh)** - خودکارسازی فرآیند

## 🏗️ معماری سیستم

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Build Time    │    │   Runtime       │    │   Security      │
│                 │    │                 │    │                 │
│ 1. Build APK    │───▶│ 1. Load App     │───▶│ 1. Verify      │
│ 2. Generate     │    │ 2. Check        │    │ 2. Detect       │
│    HMAC         │    │    Integrity    │    │    Repackaging  │
│ 3. Store        │    │ 3. Monitor      │    │ 3. Take Action  │
│    Signature    │    │    Continuously │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔐 سطوح امنیتی

### 🛡️ StrongBox (حداکثر امنیت)
- Hardware Security Module (HSM)
- کلید هرگز از سخت‌افزار امن خارج نمی‌شود
- بالاترین محافظت در برابر حملات

### 🔒 TEE (امنیت بالا)
- Trusted Execution Environment
- امنیت پشتیبانی شده توسط سخت‌افزار
- محافظت خوب در برابر حملات نرم‌افزاری

### 💻 Software (امنیت متوسط)
- پیاده‌سازی نرم‌افزاری استاندارد
- محافظت پایه در برابر دستکاری

## 🚀 شروع سریع

### 1. کپی اسکریپت‌ها
```bash
cp -r /path/to/PosSecurity/scripts/ ./scripts/
chmod +x scripts/*.sh scripts/*.py
```

### 2. اضافه کردن dependency
```gradle
dependencies {
    implementation project(':securitymodule')
}
```

### 3. Build و Sign
```bash
./gradlew assembleRelease
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk
```

### 4. اضافه کردن کد تأیید
```kotlin
lifecycleScope.launch {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
    if (!integrityInfo.isIntegrityValid) {
        // اقدام امنیتی
        finish()
    }
}
```

## 📋 ویژگی‌ها

- ✅ **تأیید اصالت APK** - بررسی تغییرات در فایل APK
- ✅ **تشخیص Repackaging** - شناسایی نصب از منابع غیرمجاز
- ✅ **امنیت سخت‌افزاری** - استفاده از StrongBox/TEE
- ✅ **یکپارچه‌سازی Build** - خودکارسازی در فرآیند build
- ✅ **بررسی دوره‌ای** - نظارت مداوم بر امنیت
- ✅ **مدیریت خطا** - پردازش graceful خطاها

## 🔧 ابزارها

### اسکریپت Python
```bash
python3 scripts/generate_apk_hmac.py app-release.apk
```

### اسکریپت Shell
```bash
./scripts/sign_apk_with_hmac.sh app-release.apk -a src/main/assets/
```

### Gradle Integration
```gradle
buildTypes {
    release {
        doLast {
            exec {
                commandLine 'bash', '../scripts/sign_apk_with_hmac.sh', 
                    "${buildDir}/outputs/apk/release/app-release.apk"
            }
        }
    }
}
```

## 📊 عملکرد

| معیار | تأثیر |
|-------|-------|
| زمان Startup | +50-100ms |
| مصرف حافظه | +1-2MB |
| تأثیر باتری | حداقل |
| استفاده شبکه | هیچ |

## 🔍 تست

### تست عادی
```bash
adb install app-release.apk
adb shell am start -n com.yourpackage/.MainActivity
```

### تست Repackaging
```bash
# تغییر APK
apktool d app-release.apk
# تغییر فایل‌ها
apktool b app-release -o modified.apk
# نصب و تست
adb install modified.apk
```

## 🛠️ عیب‌یابی

### مشکل رایج 1: اسکریپت اجرا نمی‌شود
```bash
chmod +x scripts/sign_apk_with_hmac.sh
```

### مشکل رایج 2: Signature پیدا نمی‌شود
```bash
ls -la app/src/main/assets/apk_hmac_signature.txt
```

### مشکل رایج 3: اپلیکیشن crash می‌کند
```kotlin
try {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
} catch (e: Exception) {
    Log.e("APK", "Error", e)
}
```

## 📚 منابع بیشتر

- **[راهنمای کامل](apk-hmac-step-by-step-guide.md)** - جزئیات کامل
- **[مثال عملی](apk-hmac-practical-example.md)** - پیاده‌سازی واقعی
- **[مستندات فنی](apk-hmac-protection.md)** - جزئیات فنی

## 🤝 مشارکت

برای مشارکت در توسعه:
1. Fork کنید
2. Branch جدید بسازید
3. تغییرات را commit کنید
4. Pull request ارسال کنید

## 📄 مجوز

این پروژه تحت مجوز MIT منتشر شده است.

## 🆘 پشتیبانی

در صورت بروز مشکل:
1. مستندات را مطالعه کنید
2. مثال‌ها را بررسی کنید
3. Issues را جستجو کنید
4. Issue جدید ایجاد کنید

---

**🎉 APK شما حالا با HMAC محافظت شده و در برابر repackaging امن است!**
