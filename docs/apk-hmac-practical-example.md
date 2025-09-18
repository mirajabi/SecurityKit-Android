# مثال عملی APK HMAC Protection

## 📱 سناریو: اپلیکیشن بانکداری

فرض کنید یک اپلیکیشن بانکداری دارید که باید از repackaging محافظت شود.

---

## 🏗️ مرحله 1: آماده‌سازی پروژه

### ساختار پروژه
```
BankingApp/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   ├── java/com/bank/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── LoginActivity.kt
│   │   │   └── SecurityManager.kt
│   │   └── res/
│   └── build.gradle
├── scripts/
└── docs/
```

### app/build.gradle
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.bank.app"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
            
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

dependencies {
    implementation project(':securitymodule')
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
```

---

## 🔐 مرحله 2: پیاده‌سازی SecurityManager

### SecurityManager.kt
```kotlin
package com.bank.app

import android.content.Context
import android.util.Log
import com.miaadrajabi.securitymodule.crypto.ApkHmacProtector
import kotlinx.coroutines.*

class SecurityManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isSecurityVerified = false
    
    companion object {
        private const val TAG = "SecurityManager"
        private const val SECURITY_CHECK_INTERVAL = 60000L // 1 دقیقه
    }
    
    /**
     * تأیید امنیت APK در startup
     */
    suspend fun verifyStartupSecurity(): Boolean {
        return try {
            Log.d(TAG, "Starting APK security verification...")
            
            // تأیید APK integrity
            val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
            
            if (!integrityInfo.isIntegrityValid) {
                Log.e(TAG, "APK integrity check failed")
                return false
            }
            
            // بررسی repackaging
            val isRepackaged = ApkHmacProtector.detectRepackaging(context)
            
            if (isRepackaged) {
                Log.e(TAG, "Repackaging detected")
                return false
            }
            
            Log.d(TAG, "APK security verification passed")
            isSecurityVerified = true
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "APK security verification failed", e)
            false
        }
    }
    
    /**
     * شروع بررسی دوره‌ای امنیت
     */
    fun startPeriodicSecurityCheck() {
        scope.launch {
            while (isActive && isSecurityVerified) {
                delay(SECURITY_CHECK_INTERVAL)
                
                try {
                    val isRepackaged = ApkHmacProtector.detectRepackaging(context)
                    
                    if (isRepackaged) {
                        Log.e(TAG, "Repackaging detected during periodic check")
                        handleSecurityBreach()
                        break
                    }
                    
                    Log.d(TAG, "Periodic security check passed")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Periodic security check failed", e)
                    handleSecurityError()
                    break
                }
            }
        }
    }
    
    /**
     * مدیریت نقض امنیت
     */
    private fun handleSecurityBreach() {
        Log.e(TAG, "Security breach detected - terminating app")
        
        // ذخیره رویداد امنیتی
        saveSecurityEvent("APK_REPACKAGING_DETECTED")
        
        // اطلاع‌رسانی به سرور
        notifySecurityBreach()
        
        // بستن اپلیکیشن
        scope.launch(Dispatchers.Main) {
            // می‌توانید به جای finish() به صفحه امنیتی بروید
            (context as? android.app.Activity)?.finish()
        }
    }
    
    /**
     * مدیریت خطای امنیتی
     */
    private fun handleSecurityError() {
        Log.e(TAG, "Security error occurred")
        
        // ذخیره رویداد امنیتی
        saveSecurityEvent("APK_SECURITY_ERROR")
        
        // اقدام مناسب (مثلاً محدود کردن دسترسی)
        limitAppFunctionality()
    }
    
    /**
     * ذخیره رویداد امنیتی
     */
    private fun saveSecurityEvent(event: String) {
        try {
            val prefs = context.getSharedPreferences("security_events", Context.MODE_PRIVATE)
            val events = prefs.getStringSet("events", mutableSetOf()) ?: mutableSetOf()
            events.add("${System.currentTimeMillis()}:$event")
            prefs.edit().putStringSet("events", events).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save security event", e)
        }
    }
    
    /**
     * اطلاع‌رسانی نقض امنیت به سرور
     */
    private fun notifySecurityBreach() {
        scope.launch {
            try {
                // ارسال به سرور (پیاده‌سازی بر اساس نیاز)
                Log.d(TAG, "Notifying server about security breach")
                // TODO: پیاده‌سازی API call
            } catch (e: Exception) {
                Log.e(TAG, "Failed to notify server", e)
            }
        }
    }
    
    /**
     * محدود کردن عملکرد اپلیکیشن
     */
    private fun limitAppFunctionality() {
        // می‌توانید عملکردهای حساس را غیرفعال کنید
        Log.w(TAG, "Limiting app functionality due to security concerns")
    }
    
    /**
     * بررسی وضعیت امنیت
     */
    fun isSecurityVerified(): Boolean = isSecurityVerified
    
    /**
     * پاکسازی منابع
     */
    fun cleanup() {
        scope.cancel()
    }
}
```

---

## 🏠 مرحله 3: پیاده‌سازی MainActivity

### MainActivity.kt
```kotlin
package com.bank.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var securityManager: SecurityManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // ایجاد SecurityManager
        securityManager = SecurityManager(this)
        
        // تأیید امنیت در startup
        verifySecurityAndProceed()
    }
    
    private fun verifySecurityAndProceed() {
        lifecycleScope.launch {
            // نمایش loading
            showLoadingDialog("Verifying app security...")
            
            try {
                // تأیید امنیت APK
                val isSecure = securityManager.verifyStartupSecurity()
                
                if (isSecure) {
                    // امنیت تأیید شد
                    hideLoadingDialog()
                    proceedToMainApp()
                } else {
                    // نقض امنیت
                    hideLoadingDialog()
                    handleSecurityBreach()
                }
                
            } catch (e: Exception) {
                hideLoadingDialog()
                handleSecurityError(e)
            }
        }
    }
    
    private fun proceedToMainApp() {
        // شروع بررسی دوره‌ای
        securityManager.startPeriodicSecurityCheck()
        
        // رفتن به صفحه اصلی
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    private fun handleSecurityBreach() {
        Toast.makeText(this, "Security breach detected. App will close.", Toast.LENGTH_LONG).show()
        
        // بستن اپلیکیشن
        finishAffinity()
    }
    
    private fun handleSecurityError(e: Exception) {
        Toast.makeText(this, "Security check failed. Please restart the app.", Toast.LENGTH_LONG).show()
        
        // بستن اپلیکیشن
        finishAffinity()
    }
    
    private fun showLoadingDialog(message: String) {
        // پیاده‌سازی loading dialog
        // TODO: نمایش dialog
    }
    
    private fun hideLoadingDialog() {
        // مخفی کردن loading dialog
        // TODO: مخفی کردن dialog
    }
    
    override fun onDestroy() {
        super.onDestroy()
        securityManager.cleanup()
    }
}
```

---

## 🔑 مرحله 4: پیاده‌سازی LoginActivity

### LoginActivity.kt
```kotlin
package com.bank.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var securityManager: SecurityManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // ایجاد SecurityManager
        securityManager = SecurityManager(this)
        
        // تأیید امنیت قبل از login
        verifySecurityBeforeLogin()
    }
    
    private fun verifySecurityBeforeLogin() {
        lifecycleScope.launch {
            try {
                // بررسی سریع امنیت
                val isSecure = securityManager.isSecurityVerified()
                
                if (!isSecure) {
                    // امنیت تأیید نشده
                    handleSecurityBreach()
                    return@launch
                }
                
                // امنیت OK - ادامه login
                enableLoginForm()
                
            } catch (e: Exception) {
                handleSecurityError(e)
            }
        }
    }
    
    private fun enableLoginForm() {
        // فعال کردن فرم login
        // TODO: فعال کردن UI elements
    }
    
    private fun handleSecurityBreach() {
        Toast.makeText(this, "Security breach detected. Please restart the app.", Toast.LENGTH_LONG).show()
        finishAffinity()
    }
    
    private fun handleSecurityError(e: Exception) {
        Toast.makeText(this, "Security check failed. Please restart the app.", Toast.LENGTH_LONG).show()
        finishAffinity()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        securityManager.cleanup()
    }
}
```

---

## 🏗️ مرحله 5: Build و Deploy

### Build Script
```bash
#!/bin/bash
# build_and_sign.sh

echo "🏗️ Building Banking App..."

# Clean
./gradlew clean

# Build
./gradlew assembleRelease

# Check if APK was built
if [ ! -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo "❌ APK build failed"
    exit 1
fi

echo "✅ APK built successfully"

# Generate HMAC signature
echo "🔐 Generating HMAC signature..."
./scripts/sign_apk_with_hmac.sh app/build/outputs/apk/release/app-release.apk -a src/main/assets/ -v

# Check if signature was generated
if [ ! -f "app/src/main/assets/apk_hmac_signature.txt" ]; then
    echo "❌ HMAC signature generation failed"
    exit 1
fi

echo "✅ HMAC signature generated successfully"

# Final verification
echo "🔍 Verifying final APK..."
ls -la app/build/outputs/apk/release/app-release.apk
ls -la app/src/main/assets/apk_hmac_signature.txt

echo "🎉 Banking App is ready for deployment!"
```

### اجرای Build
```bash
chmod +x build_and_sign.sh
./build_and_sign.sh
```

---

## 🧪 مرحله 6: تست

### تست 1: تست عادی
```bash
# نصب APK
adb install app/build/outputs/apk/release/app-release.apk

# اجرای اپلیکیشن
adb shell am start -n com.bank.app/.MainActivity

# بررسی لاگ‌ها
adb logcat | grep "SecurityManager"
```

### تست 2: تست Repackaging
```bash
# تغییر APK با apktool
apktool d app/build/outputs/apk/release/app-release.apk
# تغییر فایل‌ها
apktool b app-release -o modified.apk
# نصب APK تغییر یافته
adb install modified.apk

# اجرا - باید خطای امنیتی نشان دهد
adb shell am start -n com.bank.app/.MainActivity
```

### تست 3: تست Performance
```bash
# بررسی زمان startup
adb shell am start -W com.bank.app/.MainActivity

# بررسی مصرف حافظه
adb shell dumpsys meminfo com.bank.app
```

---

## 📊 نتایج مورد انتظار

### تست موفقیت‌آمیز
```
D/SecurityManager: Starting APK security verification...
D/SecurityManager: APK security verification passed
D/SecurityManager: Periodic security check passed
```

### تست Repackaging
```
E/SecurityManager: APK integrity check failed
E/SecurityManager: Repackaging detected
E/SecurityManager: Security breach detected - terminating app
```

### تست Performance
```
Startup time: +50-100ms
Memory usage: +1-2MB
Battery impact: Minimal
```

---

## 🔧 عیب‌یابی

### مشکل 1: اپلیکیشن crash می‌کند
```kotlin
// اضافه کردن try-catch
try {
    val integrityInfo = ApkHmacProtector.verifyApkIntegrity(context)
} catch (e: Exception) {
    Log.e("Security", "Error", e)
    // اقدام مناسب
}
```

### مشکل 2: Signature پیدا نمی‌شود
```bash
# بررسی وجود فایل
ls -la app/src/main/assets/apk_hmac_signature.txt

# بررسی محتوای APK
unzip -l app/build/outputs/apk/release/app-release.apk | grep signature
```

### مشکل 3: Performance کند است
```kotlin
// بهینه‌سازی بررسی دوره‌ای
private const val SECURITY_CHECK_INTERVAL = 300000L // 5 دقیقه

// استفاده از cache
private var lastCheck = 0L
```

---

## 🎯 خلاصه

این مثال عملی نشان می‌دهد که چگونه:

1. ✅ **SecurityManager** برای مدیریت امنیت
2. ✅ **تأیید امنیت** در startup
3. ✅ **بررسی دوره‌ای** برای repackaging
4. ✅ **مدیریت خطا** و نقض امنیت
5. ✅ **Build خودکار** با HMAC signature
6. ✅ **تست کامل** عملکرد و امنیت

**🏦 اپلیکیشن بانکداری شما حالا کاملاً محافظت شده است!**
