# APK HMAC Protection - راهنمای سریع

## 🚀 شروع سریع (5 دقیقه)

### مرحله 1: کپی اسکریپت‌ها
```bash
cp -r /path/to/PosSecurity/scripts/ ./scripts/
chmod +x scripts/*.sh scripts/*.py
```

### مرحله 2: اضافه کردن dependency
```gradle
// در app/build.gradle
dependencies {
    implementation project(':securitymodule')
}
```

### مرحله 3: Build APK
```bash
./gradlew assembleRelease
```

### مرحله 4: تولید HMAC Signature
```bash
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk
```

### مرحله 5: اضافه کردن کد تأیید
```kotlin
// در MainActivity.onCreate()
lifecycleScope.launch {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(this@MainActivity)
    if (!integrityInfo.isIntegrityValid) {
        // اقدام امنیتی
        finish()
    }
}
```

## ✅ تست سریع

```bash
# نصب و تست
adb install app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.yourpackage/.MainActivity
```

## 🔧 عیب‌یابی سریع

### مشکل: اسکریپت اجرا نمی‌شود
```bash
chmod +x scripts/sign_apk_with_hmac.sh
```

### مشکل: Signature پیدا نمی‌شود
```bash
ls -la app/src/main/assets/apk_hmac_signature.txt
```

### مشکل: اپلیکیشن crash می‌کند
```kotlin
try {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
} catch (e: Exception) {
    Log.e("APK", "Error", e)
}
```

## 📋 چک‌لیست

- [ ] اسکریپت‌ها کپی شده
- [ ] dependency اضافه شده
- [ ] APK build شده
- [ ] HMAC signature تولید شده
- [ ] کد تأیید اضافه شده
- [ ] تست انجام شده

## 🎯 نتیجه

APK شما حالا با HMAC محافظت شده و در برابر repackaging امن است!

---

**📖 برای جزئیات بیشتر، راهنمای کامل را مطالعه کنید: [apk-hmac-step-by-step-guide.md](apk-hmac-step-by-step-guide.md)**
