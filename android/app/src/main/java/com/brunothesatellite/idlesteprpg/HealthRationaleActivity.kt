package com.brunothesatellite.idlesteprpg

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Écran de justification demandé par Santé Connect (« Pourquoi cette
 * autorisation ? »). Depuis Android 14, l'application **doit** exposer une
 * activité sur `ACTION_VIEW_PERMISSION_USAGE` + catégorie `HEALTH_PERMISSIONS`,
 * sinon Santé Connect referme aussitôt la demande d'autorisation
 * (« App should support rationale intent »).
 */
class HealthRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(11, 8, 16))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(10), dp(10))
        }
        header.addView(TextView(this).apply {
            text = getString(R.string.health_rationale_title)
            setTextColor(Color.rgb(246, 221, 166))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "✕"
            setTextColor(Color.rgb(149, 135, 168))
            textSize = 20f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(33, 24, 45))
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            setOnClickListener { finish() }
        })

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(18))
            addView(TextView(this@HealthRationaleActivity).apply {
                text = getString(R.string.health_rationale_heading)
                setTextColor(Color.rgb(246, 221, 166))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@HealthRationaleActivity).apply {
                text = getString(R.string.health_rationale_body)
                setTextColor(Color.rgb(214, 202, 228))
                textSize = 10f
                setLineSpacing(dp(4).toFloat(), 1f)
                setPadding(0, dp(10), 0, dp(18))
            })
            addView(TextView(this@HealthRationaleActivity).apply {
                text = getString(R.string.health_rationale_close)
                setTextColor(Color.rgb(11, 8, 16))
                textSize = 13f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setBackgroundColor(Color.rgb(246, 221, 166))
                setPadding(0, dp(13), 0, dp(13))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener { finish() }
            })
        }

        root.addView(header, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
