package com.miaadrajabi.securitymodule.detectors

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object HookingDetector {
    private val suspiciousLibs = listOf("frida", "substrate", "xposed", "lsposed")

    fun suspiciousLoadedLibs(): Int {
        return try {
            val maps = File("/proc/self/maps")
            if (!maps.canRead()) return 0
            BufferedReader(FileReader(maps)).use { br ->
                br.lineSequence().count { line ->
                    suspiciousLibs.any { key -> line.contains(key, ignoreCase = true) }
                }
            }
        } catch (t: Throwable) {
            0
        }
    }
}

object MitmDetector {
    fun userAddedCertificatesPresent(context: Context): Boolean {
        // Heuristic: on Android N+, user-added CAs are in user-managed stores; full check requires APIs.
        // Keep lightweight and permissive; return false if not sure.
        return try {
            val isNetworkLogged = Settings.Global.getInt(context.contentResolver, "network_logging_enabled", 0) == 1
            isNetworkLogged
        } catch (t: Throwable) {
            false
        }
    }
}

@SuppressLint("StaticFieldLeak")
object ScreenCaptureProtector {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    @JvmStatic fun applySecureFlag(activity: Activity) {
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    @JvmStatic fun showWhiteOverlay(context: Context) {
        if (overlayView != null) return
        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val overlay = LinearLayout(context).apply {
                setBackgroundColor(Color.WHITE)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            
            val text = TextView(context).apply {
                text = "Protected content"
                textSize = 24f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            }
            overlay.addView(text)

            val params = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START

            windowManager?.addView(overlay, params)
            overlayView = overlay

            // Auto-remove after 3 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                hideWhiteOverlay()
            }, 3000)
        } catch (e: Exception) {
            // If overlay not permitted, FLAG_SECURE still helps
        }
    }

    @JvmStatic fun hideWhiteOverlay() {
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            overlayView = null
            windowManager = null
        }
    }
}

object TracerPidDetector {
    fun isTraced(): Boolean {
        return try {
            val status = File("/proc/self/status")
            if (!status.canRead()) return false
            BufferedReader(FileReader(status)).use { br ->
                br.lineSequence().any { it.startsWith("TracerPid:") && it.substringAfter(":").trim() != "0" }
            }
        } catch (t: Throwable) {
            false
        }
    }
}

object DeveloperOptionsDetector {
    fun isEnabled(context: Context): Boolean {
        return try {
            val dev = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
            dev
        } catch (t: Throwable) {
            false
        }
    }
}


