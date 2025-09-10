package com.miaadrajabi.securitymodule.detectors

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.WindowManager
import java.io.File
import android.os.FileObserver

enum class CaptureEventType { SCREENSHOT, SCREEN_RECORDING }

class ScreenCaptureMonitor(private val context: Context) {
    private var handlerThread: HandlerThread? = null
    private var imageObserver: ContentObserver? = null
    private var videoObserver: ContentObserver? = null
    private var fileObserverImages: FileObserver? = null
    private var fileObserverVideos: FileObserver? = null

    fun applySecureFlag(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun start(onEvent: (CaptureEventType, Uri?) -> Unit) {
        stop()
        handlerThread = HandlerThread("ScreenCaptureMonitor").apply { start() }
        val handler = Handler(handlerThread!!.looper)

        // MediaStore observers (modern)
        val cr: ContentResolver = context.contentResolver
        imageObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (isScreenshotUri(uri)) {
                    onEvent(CaptureEventType.SCREENSHOT, uri)
                }
            }
        }
        videoObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (isScreenRecordingUri(uri)) {
                    onEvent(CaptureEventType.SCREEN_RECORDING, uri)
                }
            }
        }
        try {
            cr.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imageObserver!!)
        } catch (_: Throwable) { }
        try {
            cr.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoObserver!!)
        } catch (_: Throwable) { }

        // Legacy file observers (best-effort)
        try {
            val pics = when {
                Build.VERSION.SDK_INT >= 29 -> File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "Pictures/Screenshots")
                else -> File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "Screenshots")
            }
            if (pics.exists()) {
                @Suppress("DEPRECATION")
                fileObserverImages = object : FileObserver(pics.absolutePath, CREATE) {
                    override fun onEvent(event: Int, path: String?) {
                        if (event == CREATE) onEvent(CaptureEventType.SCREENSHOT, null)
                    }
                }
                fileObserverImages?.startWatching()
            }
        } catch (_: Throwable) { }

        try {
            val movies = when {
                Build.VERSION.SDK_INT >= 29 -> File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "Movies/Screen recordings")
                else -> File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "Screen recordings")
            }
            if (movies.exists()) {
                @Suppress("DEPRECATION")
                fileObserverVideos = object : FileObserver(movies.absolutePath, CREATE) {
                    override fun onEvent(event: Int, path: String?) {
                        if (event == CREATE) onEvent(CaptureEventType.SCREEN_RECORDING, null)
                    }
                }
                fileObserverVideos?.startWatching()
            }
        } catch (_: Throwable) { }
    }

    fun stop() {
        val cr = context.contentResolver
        if (imageObserver != null) try { cr.unregisterContentObserver(imageObserver!!) } catch (_: Throwable) {}
        if (videoObserver != null) try { cr.unregisterContentObserver(videoObserver!!) } catch (_: Throwable) {}
        imageObserver = null
        videoObserver = null

        try { fileObserverImages?.stopWatching() } catch (_: Throwable) {}
        try { fileObserverVideos?.stopWatching() } catch (_: Throwable) {}
        fileObserverImages = null
        fileObserverVideos = null

        handlerThread?.quitSafely()
        handlerThread = null
    }

    private fun isScreenshotUri(uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0) ?: ""
                    val rel = safeGet(c, 1)
                    val data = safeGet(c, 2)
                    return name.contains("screenshot", true) || rel?.contains("Screenshots", true) == true || data?.contains("Screenshots", true) == true
                }
            }
            false
        } catch (_: Throwable) { false }
    }

    private fun isScreenRecordingUri(uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            val projection = arrayOf(MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.RELATIVE_PATH, MediaStore.Video.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0) ?: ""
                    val rel = safeGet(c, 1)
                    val data = safeGet(c, 2)
                    return name.contains("screen", true) || rel?.contains("Screen recordings", true) == true || data?.contains("Screen recordings", true) == true
                }
            }
            false
        } catch (_: Throwable) { false }
    }

    private fun safeGet(c: android.database.Cursor, idx: Int): String? = try { c.getString(idx) } catch (_: Throwable) { null }
}


