package com.miaadrajabi.securitymodule.policy

import com.miaadrajabi.securitymodule.Severity
import com.miaadrajabi.securitymodule.config.SecurityConfig

data class PolicyDecision(
    val action: SecurityConfig.Action,
    val reason: String
)

class PolicyEngine(private val config: SecurityConfig) {
    fun onRoot(findingsCount: Int): PolicyDecision {
        val action = if (findingsCount >= config.thresholds.rootSignalsToBlock) config.policy.onRoot else SecurityConfig.Action.ALLOW
        return PolicyDecision(action, "root_signals=$findingsCount")
    }

    fun onEmulator(findingsCount: Int): PolicyDecision {
        val action = if (findingsCount >= config.thresholds.emulatorSignalsToBlock) config.policy.onEmulator else SecurityConfig.Action.ALLOW
        return PolicyDecision(action, "emulator_signals=$findingsCount")
    }

    fun onDebugger(attached: Boolean): PolicyDecision {
        val action = if (attached) config.policy.onDebugger else SecurityConfig.Action.ALLOW
        return PolicyDecision(action, "debugger=$attached")
    }

    fun onUsbDebug(enabled: Boolean): PolicyDecision {
        val action = if (enabled) config.policy.onUsbDebug else SecurityConfig.Action.ALLOW
        return PolicyDecision(action, "usb_debug=$enabled")
    }

    fun onVpn(active: Boolean): PolicyDecision {
        val action = if (active) config.policy.onVpn else SecurityConfig.Action.ALLOW
        return PolicyDecision(action, "vpn=$active")
    }

    fun onMitm(detected: Boolean): PolicyDecision {
        val action = if (detected) config.policy.onMitm else SecurityConfig.Action.ALLOW
        return PolicyDecision(action, "mitm=$detected")
    }
}


