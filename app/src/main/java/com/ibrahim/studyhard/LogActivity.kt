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
            val names = ArrayList<String>()
            val ap = o.optJSONArray("apps")
            if (ap != null) for (k in 0 until ap.length()) names.add(ap.getString(k))

            val studied: Long
            val state: String
            val stateColor: Int
            if (endedAt > 0L) {
                studied = (endedAt - start) / 60000L
                state = "وقفتها بدري"
                stateColor = Color.parseColor("#FCA5A5")
            } else if (now >= start + planned * 60000L) {
                studied = planned
                state = "كمّلتها"
                stateColor = Color.parseColor("#6EE7B7")
            } else {
                studied = (now - start) / 60000L
                state = "شغالة دلوقتي"
                stateColor = GOLD
            }
            totalMin += studied

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = box(CARD, 18)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            card.addView(txt(df.format(Date(start)), 15f, Color.WHITE, true))
            card.addView(txt(state, 13f, stateColor, true))

            var line = "المدة: " + fmt(planned)
            if (endedAt > 0L) line += "  •  ذاكرت: " + fmt(studied)
            if (hard) line += "  •  تحدي Impossible"
            card.addView(txt(line, 13f, Color.parseColor("#B7CFCF")).apply { setPadding(0, dp(6), 0, 0) })

            val why = if (reason.isBlank()) "من غير سبب" else reason
            card.addView(txt("السبب: " + why, 14f, Color.WHITE).apply { setPadding(0, dp(6), 0, 0) })
            card.addView(
                txt("التطبيقات المقفولة: " + names.joinToString("، "), 13f, Color.parseColor("#B7CFCF"))
                    .apply { setPadding(0, dp(6), 0, 0) }
            )
            inner.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        }

        if (log.length() == 0) {
            root.addView(txt("لسه مفيش جلسات. ابدأ أول مذاكرة!", 15f, Color.WHITE).apply {
                setPadding(0, dp(20), 0, 0)
            })
        } else {
            root.addView(txt(
                "إجمالي المذاكرة: " + fmt(totalMin) + "  •  عدد الجلسات: " + log.length(),
                14f, Color.parseColor("#B7CFCF")
            ).apply { setPadding(0, dp(6), 0, 0) })
        }

        val sv = ScrollView(this)
        sv.addView(inner)
        root.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))

        val back = Button(this).apply {
            text = "رجوع"
            setTextColor(Color.WHITE)
            background = box(CARD, 16)
            transformationMethod = null
            setOnClickListener { finish() }
        }
        root.addView(back, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) })

        setContentView(root)
    }
}
