# SecurityModule (v1.2.0)

Production-grade Android security toolkit for banking/fintech/POS apps.


SecurityModule is a modular Android security toolkit. You can enable features selectively and tune policies and thresholds to match your needs. At a glance:

- Device integrity (root, bootloader, emulator, ADB, ...)
- App integrity (signature, debuggable, code injection, runtime changes)
- Screen capture and recording protection
- Cryptography and Keystore-backed key management (StrongBox, auth-bound, device-binding)
- Runtime protections (debugger, hooking, Frida detection, ...)
- Tamper evidence store
- Play Integrity (optional)
- Telemetry and reporting
- Policies, thresholds, and overrides
- JSON configuration and signed configuration (HMAC)

For details on each capability and how to use it, see the docs below:

- [Docs Home](docs/index.md)
- [Installation](docs/installation.md)
- [Configuration (JSON & Signed Config)](docs/configuration.md)
- [Policy & Thresholds & Overrides](docs/policy.md)
- [Device Integrity](docs/device-integrity.md)
- [App Integrity](docs/app-integrity.md)
- [Screen Capture Protection](docs/screen-capture.md)
- [Cryptography](docs/crypto.md)
- [Keystore (StrongBox/Auth-bound/Device-binding)](docs/keystore.md)
- [Runtime Protections](docs/runtime-protections.md)
- [Tamper Evidence Store](docs/tamper-evidence.md)
- [Play Integrity (optional)](docs/play-integrity.md)
- [Telemetry](docs/telemetry.md)
- [Examples (Kotlin)](docs/examples-kotlin.md)
- [Examples (Java)](docs/examples-java.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Best Practices](docs/best-practices.md)

New in v1.2.0:
- [APK HMAC Protection](docs/apk-hmac-protection.md)
- [APK HMAC - Step-by-step Guide](docs/apk-hmac-step-by-step-guide.md)
- [APK HMAC - Quick Start](docs/apk-hmac-quick-start.md)
- [APK HMAC - Practical Example](docs/apk-hmac-practical-example.md)
- [Emulator vs Real Device](docs/emulator-vs-real-device.md)
- [Fallback Strategy](docs/fallback-strategy.md)
- [Secure HMAC](docs/secure-hmac.md)
- [Main Activity Test Hub UI](docs/main-activity-ui.md)

### Quick JitPack usage

1) Add JitPack repository

Gradle (Groovy) — settings.gradle (Gradle 7+):
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Gradle (Kotlin DSL) — settings.gradle.kts (Gradle 7+):
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

2) Add the dependency to your app module

Groovy — build.gradle (Module):
```groovy
dependencies {
    implementation 'com.github.mirajabi:SecurityKit-Android:1.2.0'
}
```

Kotlin DSL — build.gradle.kts (Module):
```kotlin
dependencies {
    implementation("com.github.mirajabi:SecurityKit-Android:1.2.0")
}
```

Then sync Gradle. If a newer release is available, update the version to the latest tag.
