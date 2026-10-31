package com.nullx11sudu.official

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private val projectionCode = 31
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(32.dp, 24.dp, 32.dp, 24.dp); setBackgroundColor(0xFF10191B.toInt())
        }
        fun text(value: String, size: Float) = TextView(this).apply { text = value; textSize = size; setTextColor(0xFFE5F1EF.toInt()) }
        root.addView(text("Nullx11Sudo", 34f))
        root.addView(text("A quiet companion for Sudoku. Scan the visible board, then see answers only in empty cells.", 17f).apply { setPadding(0, 18.dp, 0, 28.dp) })
        status = text("", 15f); root.addView(status)
        val button = Button(this).apply { text = "Allow overlay & start scan"; setOnClickListener { startFlow() } }
        root.addView(button)
        root.addView(text("Permissions: display-over-other-apps for the floating guide, and a one-time Android screen-capture prompt each session. Notifications keep the capture service visible. Images stay on your device. Tap the bubble to align and scan; tap it again to close the guide. Stop from the notification.", 14f).apply { setPadding(0, 26.dp, 0, 0) })
        setContentView(root)
    }
    override fun onResume() { super.onResume(); if (::status.isInitialized) status.text = if (Settings.canDrawOverlays(this)) "Overlay access enabled" else "Overlay access needed to show the floating guide" }
    private fun startFlow() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Enable display over other apps, then return and tap Start", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 32)
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), projectionCode)
    }
    @Deprecated("Legacy result API for minSdk 26")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == projectionCode && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, ScanService::class.java).putExtra("resultCode", resultCode).putExtra("captureData", data)
            ContextCompat.startForegroundService(this, intent)
            finish()
        }
    }
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}
