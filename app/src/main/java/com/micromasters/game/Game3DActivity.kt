package com.micromasters.game

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts the WebGL (Three.js) 3D game, which lives entirely in app/src/main/assets/game
 * and runs fully offline. The WebView is the gameplay surface; everything else (menu,
 * world select) stays native.
 */
class Game3DActivity : AppCompatActivity() {

    private var web: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val w = WebView(this)
        web = w
        w.setBackgroundColor(0xFF0A1330.toInt())
        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
        }
        w.addJavascriptInterface(Bridge(), "Android")
        setContentView(w)

        val world = intent.getStringExtra("world") ?: "kitchen"
        w.loadUrl("file:///android_asset/game/index.html?world=$world")
    }

    private inner class Bridge {
        @JavascriptInterface
        fun back() { runOnUiThread { finish() } }
    }

    override fun onPause() { super.onPause(); web?.onPause() }
    override fun onResume() { super.onResume(); web?.onResume() }

    override fun onDestroy() {
        web?.let { it.loadUrl("about:blank"); it.destroy() }
        web = null
        super.onDestroy()
    }
}
