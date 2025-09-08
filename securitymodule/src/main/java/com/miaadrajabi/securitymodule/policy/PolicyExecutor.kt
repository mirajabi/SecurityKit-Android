package com.miaadrajabi.securitymodule.policy

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.miaadrajabi.securitymodule.config.SecurityConfig
import com.miaadrajabi.securitymodule.ui.SecurityBlockedActivity

class PolicyExecutor(private val context: Context) {
    fun execute(action: SecurityConfig.Action) {
        when (action) {
            SecurityConfig.Action.ALLOW -> return
            SecurityConfig.Action.WARN -> return
            SecurityConfig.Action.DEGRADE -> return
            SecurityConfig.Action.BLOCK, SecurityConfig.Action.TERMINATE -> {
                val intent = Intent(context, SecurityBlockedActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                if (action == SecurityConfig.Action.TERMINATE && context is Activity) {
                    context.finishAffinity()
                }
            }
        }
    }
}


