# Installation

## JitPack (online)

settings.gradle:
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

app/build.gradle:
```gradle
dependencies {
    implementation 'com.github.mirajabi:SecurityKit-Android:1.1.0'
}
```

## Local module include
```gradle
dependencies { implementation project(':securitymodule') }
```

## Kotlin quick start
```kotlin
val config = com.miaadrajabi.securitymodule.config.SecurityConfigLoader.fromAsset(this, "security_config.json")
val module = com.miaadrajabi.securitymodule.SecurityModule.Builder(applicationContext)
    .setConfig(config)
    .build()
val report = module.runAllChecksBlocking()
```

## Java quick start
```java
com.miaadrajabi.securitymodule.config.SecurityConfig config =
  com.miaadrajabi.securitymodule.config.SecurityConfigLoader.fromAsset(this, "security_config.json");
com.miaadrajabi.securitymodule.SecurityModule module = new com.miaadrajabi.securitymodule.SecurityModule.Builder(getApplicationContext())
    .setConfig(config)
    .build();
com.miaadrajabi.securitymodule.SecurityReport report = module.runAllChecksBlocking();
```
