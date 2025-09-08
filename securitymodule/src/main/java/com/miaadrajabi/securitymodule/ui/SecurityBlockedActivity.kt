package com.miaadrajabi.securitymodule.ui

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.miaadrajabi.securitymodule.R

class SecurityBlockedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = TextView(this)
        message.text = getString(R.string.security_blocked_message)
        setContentView(message)
    }
}


