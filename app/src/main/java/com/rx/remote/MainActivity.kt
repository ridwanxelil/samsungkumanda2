package com.rx.remote

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast

/**
 * RX Remote: native shell around the HTML UI in assets/index.html.
 * JS calls Android.transmit(carrierHz, "560,560,...") -> ConsumerIrManager.transmit.
 * The UI and the key/protocol spec live in index.html (see <script id="rx-spec">).
 */
class MainActivity : Activity() {

    private var ir: ConsumerIrManager? = null
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        ir = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        if (ir?.hasIrEmitter() != true) {
            Toast.makeText(this, "Bu telefonda IR vericisi bulunamadı", Toast.LENGTH_LONG).show()
        }

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            isHapticFeedbackEnabled = true
            setBackgroundColor(0xFF0D0F14.toInt())
            addJavascriptInterface(Bridge(), "Android")
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(web)
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    inner class Bridge {
        @JavascriptInterface
        fun transmit(carrierHz: Int, pattern: String) {
            val m = ir ?: return
            if (!m.hasIrEmitter()) return
            val p = pattern.split(",").map { it.trim().toInt() }.toIntArray()
            m.transmit(carrierHz, p)
            web.post { web.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
        }

        @JavascriptInterface
        fun hasIr(): Boolean = ir?.hasIrEmitter() == true
    }
}
