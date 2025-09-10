package com.miaadrajabi.securitymodule.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SecurityConfig(
    @SerialName("features") val features: Features = Features(),
    @SerialName("thresholds") val thresholds: Thresholds = Thresholds(),
    @SerialName("overrides") val overrides: Overrides = Overrides(),
    @SerialName("policy") val policy: PolicyRules = PolicyRules(),
    @SerialName("telemetry") val telemetry: Telemetry = Telemetry(),
    @SerialName("appIntegrity") val appIntegrity: AppIntegrity = AppIntegrity(),
    @SerialName("advanced") val advanced: Advanced = Advanced()
) {
    @Serializable
    data class Features(
        val rootDetection: Boolean = true,
        val emulatorDetection: Boolean = true,
        val debuggerDetection: Boolean = true,
        val usbDebugDetection: Boolean = true,
        val vpnDetection: Boolean = true,
        val mitmDetection: Boolean = true,
        val screenCaptureProtection: Boolean = true,
        val appSignatureVerification: Boolean = false,
        val repackagingDetection: Boolean = true,
        val playIntegrityCheck: Boolean = false,
        val advancedAppIntegrity: Boolean = false,
        val strongBoxKeys: Boolean = false,
        val userAuthBoundKeys: Boolean = false,
        val deviceBinding: Boolean = false,
        val configIntegrity: Boolean = false,
        val tamperEvidence: Boolean = false
    ) {
        @JvmName("isScreenCaptureProtection")
        fun isScreenCaptureProtection(): Boolean = screenCaptureProtection
    }

    @Serializable
    data class Thresholds(
        val emulatorSignalsToBlock: Int = 2,
        val rootSignalsToBlock: Int = 2,
        val playIntegritySignalsToBlock: Int = 1,
        val appIntegritySignalsToBlock: Int = 1
    )

    @Serializable
    data class Overrides(
        val allowedModels: List<String> = emptyList(),
        val deniedModels: List<String> = emptyList(),
        val allowedBrands: List<String> = emptyList(),
        val deniedBrands: List<String> = emptyList(),
        val allowedManufacturers: List<String> = emptyList(),
        val allowedProducts: List<String> = emptyList(),
        val allowedDevices: List<String> = emptyList(),
        val allowedBoards: List<String> = emptyList()
    )

    @Serializable
    data class PolicyRules(
        val onRoot: Action = Action.BLOCK,
        val onEmulator: Action = Action.BLOCK,
        val onDebugger: Action = Action.WARN,
        val onUsbDebug: Action = Action.WARN,
        val onVpn: Action = Action.WARN,
        val onMitm: Action = Action.BLOCK,
        val onPlayIntegrityFailure: Action = Action.WARN,
        val onAppIntegrityFailure: Action = Action.BLOCK,
        val onConfigTampering: Action = Action.BLOCK,
        val onStrongBoxUnavailable: Action = Action.WARN
    )

    @Serializable
    data class Telemetry(
        val enabled: Boolean = true
    )

    enum class Action { ALLOW, WARN, DEGRADE, BLOCK, TERMINATE }

    @Serializable
    data class AppIntegrity(
        val expectedPackageName: String? = null,
        val expectedSignatureSha256: List<String> = emptyList(),
        val allowedInstallers: List<String> = listOf(
            "com.android.vending",
            "com.huawei.appmarket",
            "com.samsung.android.galaxyapps",
            "com.amazon.venezia",
            "com.sec.android.app.samsungapps"
        ),
        val expectedDexChecksums: Map<String, String> = emptyMap(),
        val expectedSoChecksums: Map<String, String> = emptyMap()
    )

    @Serializable
    data class Advanced(
        val playIntegrity: PlayIntegrityConfig = PlayIntegrityConfig(),
        val keystore: KeystoreConfig = KeystoreConfig(),
        val configIntegrity: ConfigIntegritySettings = ConfigIntegritySettings(),
        val tamperEvidence: TamperEvidenceSettings = TamperEvidenceSettings()
    )

    @Serializable
    data class PlayIntegrityConfig(
        val enabled: Boolean = false,
        val nonce: String = "default_nonce",
        val fallbackOnUnavailable: Boolean = true,
        val timeoutSeconds: Long = 30
    )

    @Serializable
    data class KeystoreConfig(
        val preferStrongBox: Boolean = false,
        val userAuthRequired: Boolean = false,
        val userAuthValiditySeconds: Int = 300,
        val deviceBindingEnabled: Boolean = false
    )

    @Serializable
    data class ConfigIntegritySettings(
        val enabled: Boolean = false,
        val verificationMethod: String = "HMAC", // "HMAC" or "RSA"
        val hmacKey: String? = null,
        val publicKeyPem: String? = null,
        val networkConfig: NetworkConfigSettings? = null
    )

    @Serializable
    data class NetworkConfigSettings(
        val configUrl: String = "",
        val signatureUrl: String? = null,
        val timeoutSeconds: Long = 30,
        val cacheMaxAgeSeconds: Long = 3600
    )

    @Serializable
    data class TamperEvidenceSettings(
        val enabled: Boolean = false,
        val maxCacheAgeSeconds: Long = 86400, // 24 hours
        val autoRotateKeys: Boolean = false,
        val keyRotationIntervalSeconds: Long = 604800 // 7 days
    )
}


