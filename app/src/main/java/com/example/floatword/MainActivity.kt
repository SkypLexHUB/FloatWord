package com.example.floatword

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "🔍 ทายคำลอย\n\nกดปุ่มด้านล่างเพื่อเปิดไอคอนลอย\nแล้วสลับไปเปิดเกมอื่นได้เลย"
            textSize = 18f
            gravity = Gravity.CENTER
        })
        root.addView(Button(this).apply {
            text = "เปิดตัวช่วยลอย"
            setOnClickListener { launch() }
        })
        root.addView(Button(this).apply {
            text = "ปิดตัวช่วยลอย"
            setOnClickListener {
                startService(Intent(this@MainActivity, FloatingService::class.java).setAction("STOP"))
            }
        })
        setContentView(root)
    }

    private fun launch() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "กรุณาอนุญาต \"แสดงทับแอปอื่น\" แล้วกลับมากดใหม่", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
        go()
    }

    private fun go() {
        startForegroundService(Intent(this, FloatingService::class.java))
        moveTaskToBack(true)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        go() // ทำงานต่อได้แม้ไม่อนุญาตแจ้งเตือน
    }
}
