# Library-specific keep rules (example; refine as implementation grows)
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-keep class com.miaadrajabi.securitymodule.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# Keep kotlinx serialization generated code
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * { @kotlinx.serialization.Serializable *; }

# OkHttp / TLS pinning
-dontwarn okio.**
-keep class okhttp3.CertificatePinner { *; }
-keep class okhttp3.OkHttpClient { *; }

