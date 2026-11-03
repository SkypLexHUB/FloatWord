package com.example.floatword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var web: WebView? = null
    private lateinit var bubbleLp: WindowManager.LayoutParams
    private lateinit var panelLp: WindowManager.LayoutParams
    private val main = Handler(Looper.getMainLooper())
    private var panelOpen = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (bubble == null) start()
        return START_NOT_STICKY
    }

    private fun start() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        buildWebView()
        buildBubble()
        buildPanel()
        wm.addView(bubble, bubbleLp)
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("float", "ทายคำลอย", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 0, Intent(this, FloatingService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, "float")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle("ทายคำลอยกำลังทำงาน")
            .setContentText("แตะที่นี่เพื่อปิด")
            .setContentIntent(stop)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun overlayLp(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun buildWebView() {
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            setBackgroundColor(0xFF17122E.toInt())
            addJavascriptInterface(object {
                @JavascriptInterface fun collapse() { main.post { showBubble() } }
            }, "Android")
            loadUrl("file:///android_asset/index.html")
        }
    }

    private fun buildBubble() {
        val b = TextView(this).apply {
            text = "🔍"; textSize = 26f; gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                orientation = GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(0xFF7C3AED.toInt(), 0xFF06B6D4.toInt())
            }
        }
        bubbleLp = overlayLp(
            dp(56), dp(56),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        ).apply { x = dp(16); y = dp(200) }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        b.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = bubbleLp.x; oy = bubbleLp.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (abs(dx) + abs(dy) > dp(6)) moved = true
                    if (moved) { bubbleLp.x = ox + dx.toInt(); bubbleLp.y = oy + dy.toInt(); wm.updateViewLayout(b, bubbleLp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) showPanel()
            }
            true
        }
        bubble = b
    }

    private fun buildPanel() {
        val bg = GradientDrawable().apply { setColor(0xFF17122E.toInt()); cornerRadius = dp(20).toFloat() }
        val handle = TextView(this).apply {
            text = "⠿  ลากตรงนี้เพื่อย้าย"; textSize = 11f; gravity = Gravity.CENTER
            setTextColor(0xFF9B94C4.toInt())
        }
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg
            clipToOutline = true
            addView(handle, LinearLayout.LayoutParams(-1, dp(28)))
            addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        panelLp = overlayLp(
            0, 0,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        ).apply { softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = panelLp.x; oy = panelLp.y }
                MotionEvent.ACTION_MOVE -> {
                    panelLp.x = ox + (e.rawX - sx).toInt(); panelLp.y = oy + (e.rawY - sy).toInt()
                    wm.updateViewLayout(p, panelLp)
                }
            }
            true
        }
        panel = p
    }

    private fun showPanel() {
        if (panelOpen) return
        val m = resources.displayMetrics
        panelLp.width = minOf((m.widthPixels * 0.94).toInt(), dp(420))
        panelLp.height = minOf((m.heightPixels * 0.85).toInt(), dp(560))
        panelLp.x = (m.widthPixels - panelLp.width) / 2
        panelLp.y = dp(24)
        wm.removeView(bubble)
        wm.addView(panel, panelLp)
        panelOpen = true
    }

    private fun showBubble() {
        if (!panelOpen) return
        wm.removeView(panel)
        wm.addView(bubble, bubbleLp)
        panelOpen = false
    }

    override fun onDestroy() {
        try { if (panelOpen) wm.removeView(panel) else wm.removeView(bubble) } catch (_: Exception) {}
        web?.destroy()
        super.onDestroy()
    }
}
