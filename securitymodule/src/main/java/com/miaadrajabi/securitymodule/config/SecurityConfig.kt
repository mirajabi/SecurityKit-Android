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
    @SerialName("appIntegrity") val appIntegrity: AppIntegrity = AppIntegrity()
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
        val repackagingDetection: Boolean = true
    )

    @Serializable
    data class Thresholds(
        val emulatorSignalsToBlock: Int = 2,
        val rootSignalsToBlock: Int = 2
    )

    @Serializable
    data class Overrides(
        val allowedModels: List<String> = emptyList(),
        val deniedModels: List<String> = emptyList(),
        val allowedBrands: List<String> = emptyList(),
        val deniedBrands: List<String> = emptyList()
    )

    @Serializable
    data class PolicyRules(
        val onRoot: Action = Action.BLOCK,
        val onEmulator: Action = Action.BLOCK,
        val onDebugger: Action = Action.WARN,
        val onUsbDebug: Action = Action.WARN,
        val onVpn: Action = Action.WARN,
        val onMitm: Action = Action.BLOCK
    )

    @Serializable
    data class Telemetry(
        val enabled: Boolean = true
    )

    enum class Action { ALLOW, WARN, DEGRADE, BLOCK, TERMINATE }

    @Serializable
    data class AppIntegrity(
        val expectedPackageName: String? = null,
        val expectedSignatureSha256: List<String> = emptyList()
    )
}


