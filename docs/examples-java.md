# Examples (Java)

## Minimal
```java
SecurityConfig config = SecurityConfigLoader.fromAsset(this, "security_config.json");
SecurityModule module = new SecurityModule.Builder(getApplicationContext())
    .setConfig(config)
    .build();
SecurityReport report = module.runAllChecksBlocking();
```

## Signed config loader
```java
String key = BuildConfig.CONFIG_HMAC_KEY;
SecurityConfig config = SecurityConfigLoader.fromAssetPreferSigned(
  this, "security_config.json", "security_config.sig", (key != null && !key.isEmpty()) ? key : null);
```

## Screen capture
```java
if (config.getFeatures().isScreenCaptureProtection()) {
  ScreenCaptureProtector.applySecureFlag(this);
  ScreenCaptureMonitor monitor = new ScreenCaptureMonitor(this);
  monitor.start((type, uri) -> ScreenCaptureProtector.showWhiteOverlay(this));
}
```
