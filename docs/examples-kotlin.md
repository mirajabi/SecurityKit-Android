# Examples (Kotlin)

## Minimal
```kotlin
val config = SecurityConfigLoader.fromAsset(this, "security_config.json")
val module = SecurityModule.Builder(applicationContext).setConfig(config).build()
val report = module.runAllChecksBlocking()
```

## Signed config loader
```kotlin
val key = BuildConfig.CONFIG_HMAC_KEY
val config = SecurityConfigLoader.fromAssetPreferSigned(this, "security_config.json", "security_config.sig", key.ifEmpty { null })
```

## Screen capture
```kotlin
if (config.features.screenCaptureProtection) {
  ScreenCaptureProtector.applySecureFlag(this)
  ScreenCaptureMonitor(this).start { _, _ -> ScreenCaptureProtector.showWhiteOverlay(this) }
}
```
