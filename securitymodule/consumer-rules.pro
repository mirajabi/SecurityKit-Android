# Consumer rules for apps integrating SecurityModule
-keep class com.miaadrajabi.securitymodule.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# OkHttp and TLS pinning
-dontwarn okio.**
-keep class okhttp3.CertificatePinner { *; }
-keep class okhttp3.OkHttpClient { *; }

