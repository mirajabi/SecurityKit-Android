# Screen Capture Protection

Multi-layered approach:
- Apply `FLAG_SECURE` to prevent OS-level screenshots
- Active monitoring for screenshot/recording
- White overlay workaround (useful on some emulators like Genymotion)

## Kotlin
```kotlin
if (config.features.screenCaptureProtection) {
  com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector.applySecureFlag(this)
  com.miaadrajabi.securitymodule.detectors.ScreenCaptureMonitor(this).start { _, _ ->
    com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector.showWhiteOverlay(this)
  }
}
```

## Java
```java
if (config.getFeatures().isScreenCaptureProtection()) {
  com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector.applySecureFlag(this);
  com.miaadrajabi.securitymodule.detectors.ScreenCaptureMonitor m = new com.miaadrajabi.securitymodule.detectors.ScreenCaptureMonitor(this);
  m.start((type, uri) -> com.miaadrajabi.securitymodule.detectors.ScreenCaptureProtector.showWhiteOverlay(this));
}
```
