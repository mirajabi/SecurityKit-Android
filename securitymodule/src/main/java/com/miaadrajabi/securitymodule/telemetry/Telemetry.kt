package com.miaadrajabi.securitymodule.telemetry

import com.miaadrajabi.securitymodule.SecurityFinding

interface TelemetrySink {
    fun onEvent(eventId: String, attributes: Map<String, String> = emptyMap())
}

object NoopTelemetry : TelemetrySink {
    override fun onEvent(eventId: String, attributes: Map<String, String>) { }
}

object TelemetryEvents {
    const val ROOT_DETECTED = "root_detected"
    const val EMULATOR_DETECTED = "emulator_detected"
    const val DEBUGGER_ATTACHED = "debugger_attached"
    const val USB_DEBUG_ENABLED = "usb_debug_enabled"
    const val VPN_ACTIVE = "vpn_active"
    const val POLICY_DECISION = "policy_decision"
}


