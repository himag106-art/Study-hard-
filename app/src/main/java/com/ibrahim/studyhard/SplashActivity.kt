package com.ibrahim.studyhard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class SplashActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private var opened = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.parseColor("#0F3D3E")
        window.navigationBarColor = Color.parseColor("#0F3D3E")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.parseColor("#0F3D3E"))
            setPadding(60, 60, 60, 60)
        }
        val main = TextView(this).apply {
            text = "صَلِّ على النبي"
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F2A900"))
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "اللهم صلِّ وسلِّم على سيدنا محمد"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        root.addView(main)
        root.addView(sub)
        setContentView(root)

        root.alpha = 0f
        root.animate().alpha(1f).setDuration(900).start()
        root.setOnClickListener { go() }
        h.postDelayed({ go() }, 2800)
    }

    private fun go() {
        if (opened) return
        opened = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
