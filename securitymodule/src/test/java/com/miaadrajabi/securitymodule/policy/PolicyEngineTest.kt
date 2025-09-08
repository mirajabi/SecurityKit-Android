package com.miaadrajabi.securitymodule.policy

import com.miaadrajabi.securitymodule.config.SecurityConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class PolicyEngineTest {
    @Test
    fun rootThresholdBlocks() {
        val cfg = SecurityConfig(
            policy = SecurityConfig.PolicyRules(onRoot = SecurityConfig.Action.BLOCK),
            thresholds = SecurityConfig.Thresholds(rootSignalsToBlock = 2)
        )
        val engine = PolicyEngine(cfg)
        val d1 = engine.onRoot(1)
        val d2 = engine.onRoot(2)
        assertEquals(SecurityConfig.Action.ALLOW, d1.action)
        assertEquals(SecurityConfig.Action.BLOCK, d2.action)
    }
}


