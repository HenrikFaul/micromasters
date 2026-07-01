package com.micromasters.game

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Deliberately minimal: a plain framework Activity built entirely in code — no
 * Material theme, no ViewBinding, no vector drawables — so it can display a
 * crash report even when the thing that crashed is the theme or a resource.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trace = intent.getStringExtra("trace")
            ?: getSharedPreferences("crash", MODE_PRIVATE).getString("trace", "")
            ?: "(nincs rögzített hiba)"

        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF101826.toInt())
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "MicroMasters – hibajelentés"
            setTextColor(0xFFF2A33C.toInt())
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Készíts képernyőképet vagy oszd meg ezt a szöveget a fejlesztőnek:"
            setTextColor(0xFFAFBEDC.toInt())
            textSize = 13f
            setPadding(0, pad / 2, 0, pad / 2)
        })

        val traceView = TextView(this).apply {
            text = trace
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        root.addView(
            ScrollView(this).apply { addView(traceView) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, pad / 2, 0, 0)
        }
        buttons.addView(Button(this).apply {
            text = "Megosztás"
            setOnClickListener {
                try {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, trace)
                    }
                    startActivity(Intent.createChooser(send, "Megosztás"))
                } catch (_: Throwable) {
                }
            }
        })
        buttons.addView(Button(this).apply {
            text = "Újraindítás"
            setOnClickListener {
                getSharedPreferences("crash", MODE_PRIVATE).edit().remove("trace").apply()
                startActivity(
                    Intent(this@CrashActivity, TitleActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }
        })
        root.addView(buttons)

        setContentView(root)
    }
}
