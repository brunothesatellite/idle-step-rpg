package com.brunothesatellite.idlesteprpg

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManualActivity : ComponentActivity() {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scale = getSharedPreferences("app_settings", MODE_PRIVATE)
            .getFloat("text_scale", 1.35f)
            .coerceIn(1f, 1.7f)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(11, 8, 16))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(10), dp(10))
            setBackgroundColor(Color.rgb(11, 8, 16))
        }
        val title = TextView(this).apply {
            text = getString(R.string.manual_title)
            setTextColor(Color.rgb(246, 221, 166))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = TextView(this).apply {
            text = "×"
            setTextColor(Color.rgb(149, 135, 168))
            textSize = 24f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(33, 24, 45))
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            setOnClickListener {
                finish()
            }
        }
        header.addView(title)
        header.addView(close)

        val web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = false
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.textZoom = (scale * 100).toInt()
            isVerticalScrollBarEnabled = true
            isNestedScrollingEnabled = false
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/manuel/index.html")
        }
        webView = web

        root.addView(header, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(web, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    override fun onDestroy() {
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        webView = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
