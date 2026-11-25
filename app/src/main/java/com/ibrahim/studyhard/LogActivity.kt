package com.ibrahim.studyhard

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogActivity : Activity() {
    private val BG = Color.parseColor("#0F3D3E")
    private val CARD = Color.parseColor("#17575A")
    private val GOLD = Color.parseColor("#F2A900")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun box(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun txt(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun fmt(m: Long): String {
        val hh = m / 60
        val mm = m % 60
        return when {
            hh > 0 && mm > 0 -> "$hh ساعة و$mm دقيقة"
            hh > 0 -> "$hh ساعة"
            else -> "$mm دقيقة"
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG
        window.navigationBarColor = BG

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(BG)
            setPadding(dp(18), dp(40), dp(18), dp(16))
        }
        root.addView(txt("سجل المذاكرة", 26f, GOLD, true))

        val log = Store.log(this)
        val now = System.currentTimeMillis()
        val df = SimpleDateFormat("EEEE d/M  hh:mm a", Locale("ar"))
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var totalMin = 0L

        for (i in log.length() - 1 downTo 0) {
            val o = log.getJSONObject(i)
            val start = o.getLong("start")
            val planned = o.getLong("planned")
            val endedAt = o.optLong("endedAt", 0L)
            val hard = o.optBoolean("hard", false)
            val reason = o.optString("reason", "")
            val names = ArrayList
