package com.miaadrajabi.securitysample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.miaadrajabi.securitymodule.SecurityReport
import com.miaadrajabi.securitymodule.config.SecurityConfig

class ReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val severity = intent.getStringExtra("severity") ?: "OK"
        val ids = intent.getStringArrayListExtra("finding_ids") ?: arrayListOf()
        val titles = intent.getStringArrayListExtra("finding_titles") ?: arrayListOf()
        val severities = intent.getStringArrayListExtra("finding_severities") ?: arrayListOf()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        val lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        root.layoutParams = lp
        root.gravity = Gravity.TOP

        fun addTitle(text: String) {
            val tv = TextView(this)
            tv.text = text
            tv.textSize = 18f
            tv.setTextColor(Color.BLACK)
            tv.setPadding(32, 32, 32, 12)
            root.addView(tv)
        }

        fun addLine(text: String, color: Int = Color.DKGRAY) {
            val tv = TextView(this)
            tv.text = text
            tv.setTextColor(color)
            tv.setPadding(48, 8, 32, 8)
            root.addView(tv)
        }

        addTitle("Security Report - Severity: $severity")

        addTitle("Detected")
        if (ids.isEmpty()) {
            addLine("No issues detected", Color.parseColor("#2E7D32"))
        } else {
            for (i in ids.indices) {
                val label = if (i < titles.size) titles[i] else ids[i]
                val sev = if (i < severities.size) severities[i] else "INFO"
                val color = if (sev.equals("BLOCK", true)) Color.RED else Color.rgb(230,156,0)
                addLine("- ${ids[i]}: $label ($sev)", color)
            }
        }

        // Known categories for quick overview
        val known = linkedSetOf(
            "root", "emulator", "debugger", "usb_debug", "vpn", "proxy", "mitm",
            "tracer", "hooking", "busybox", "mount", "dev_options", "signature", "repackaging"
        )
        val detected = ids.toSet()
        val notDetected = known.filterNot { detected.contains(it) }

        addTitle("Not Detected (overview)")
        if (notDetected.isEmpty()) {
            addLine("None", Color.DKGRAY)
        } else {
            notDetected.forEach { addLine("- $it", Color.parseColor("#2E7D32")) }
        }

        setContentView(root)
    }

    companion object {
        fun start(context: Context, report: SecurityReport, config: SecurityConfig) {
            val intent = Intent(context, ReportActivity::class.java)
            intent.putExtra("severity", report.overallSeverity.name)
            
            val ids = ArrayList<String>()
            val titles = ArrayList<String>()
            val sevs = ArrayList<String>()
            report.findings.forEach {
                ids.add(it.id)
                titles.add(it.title)
                sevs.add(it.severity.name)
            }
            
            intent.putStringArrayListExtra("finding_ids", ids)
            intent.putStringArrayListExtra("finding_titles", titles)
            intent.putStringArrayListExtra("finding_severities", sevs)
            
            context.startActivity(intent)
        }
    }
}


