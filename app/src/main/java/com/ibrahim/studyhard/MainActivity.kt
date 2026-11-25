package com.ibrahim.studyhard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val BG = Color.parseColor("#0F3D3E")
    private val CARD = Color.parseColor("#17575A")
    private val GOLD = Color.parseColor("#F2A900")
    private val INK = Color.parseColor("#0F3D3E")

    private class AppItem(val label: String, val pkg: String, val icon: Drawable)

    private val chosen = HashSet<String>()
    private var mins = 25L
    private lateinit var status: TextView
    private lateinit var sub: TextView
    private val chips = ArrayList<TextView>()
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { refresh(); h.postDelayed(this, 1000) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun box(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun label(t: String) = TextView(this).apply {
        text = t
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        setPadding(0, dp(16), 0, dp(8))
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        chosen.addAll(Store.locked(this))

        val pm = packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
            .filter { it.pkg != packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(BG)
            setPadding(dp(18), dp(40), dp(18), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "Study Hard"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(GOLD)
        })
        root.addView(TextView(this).apply {
            text = "اجتهد  •  عمل أخوك إبراهيم جمال"
            textSize = 13f
            setTextColor(Color.parseColor("#B7CFCF"))
            setPadding(0, 0, 0, dp(14))
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = box(CARD, 20)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        status = TextView(this).apply {
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        sub = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#B7CFCF"))
            gravity = Gravity.CENTER
        }
        card.addView(status)
        card.addView(sub)
        root.addView(card, LinearLayout.LayoutParams(-1, -2))

        root.addView(label("مدة المذاكرة"))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (m in listOf(25L, 45L, 60L, 90L)) {
            val c = TextView(this).apply {
                text = "$m د"
                textSize = 15f
                gravity = Gravity.CENTER
                tag = m
                setPadding(dp(8), dp(10), dp(8), dp(10))
                setOnClickListener { mins = m; paintChips() }
            }
            chips.add(c)
            row.addView(c, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
        root.addView(row)
        paintChips()

        root.addView(label("اختار التطبيقات اللي هتتقفل"))
        val list = ListView(this).apply {
            divider = null
            dividerHeight = 0
            background = box(CARD, 20)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            clipToPadding = false
        }
        val adapter = object : BaseAdapter() {
            override fun getCount() = apps.size
            override fun getItem(i: Int): Any = apps[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, v: View?, p: ViewGroup?): View {
                val a = apps[i]
                val on = a.pkg in chosen
                val r = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    background = box(if (on) Color.parseColor("#1F6F72") else Color.TRANSPARENT, 14)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                }
                r.addView(
                    ImageView(this@MainActivity).apply { setImageDrawable(a.icon) },
                    LinearLayout.LayoutParams(dp(40), dp(40))
                )
                r.addView(TextView(this@MainActivity).apply {
                    text = a.label
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setPadding(dp(12), 0, dp(12), 0)
                }, LinearLayout.LayoutParams(0, -2, 1f))
                r.addView(TextView(this@MainActivity).apply {
                    text = if (on) "مقفول" else "اختار"
                    textSize = 13f
                    setTextColor(if (on) GOLD else Color.parseColor("#8FB0B0"))
                    typeface = if (on) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                })
                return r
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, i, _ ->
            val pkg = apps[i].pkg
            if (!chosen.add(pkg)) chosen.remove(pkg)
            adapter.notifyDataSetChanged()
        }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        val start = Button(this).apply {
            text = "ابدأ المذاكرة وقفل التطبيقات"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(INK)
            background = box(GOLD, 16)
            setAllCaps(false)
            setOnClickListener { startSession() }
        }
        root.addView(start, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })

        val stop = Button(this).apply {
            text = "خلصت مذاكرة"
            setTextColor(Color.WHITE)
            background = box(CARD, 16)
            setAllCaps(false)
            setOnClickListener { Store.setEndAt(this@MainActivity, 0); refresh() }
        }
        root.addView(stop, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })

        setContentView(root)
    }

    private fun paintChips() {
        for (c in chips) {
            val on = (c.tag as Long) == mins
            c.background = box(if (on) GOLD else CARD, 14)
            c.setTextColor(if (on) INK else Color.WHITE)
        }
    }

    private fun startSession() {
        if (chosen.isEmpty()) { toast("اختار تطبيق واحد على الأقل"); return }
        if (!serviceOn()) {
            toast("فعّل Study Hard من إعدادات إمكانية الوصول")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        Store.setLocked(this, chosen)
        Store.setEndAt(this, System.currentTimeMillis() + mins * 60000)
        refresh()
    }

    private fun serviceOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains("$packageName/${BlockerService::class.java.name}")
    }

    private fun refresh() {
        val left = (Store.endAt(this) - System.currentTimeMillis()) / 1000
        if (left > 0) {
            status.text = "%02d:%02d".format(left / 60, left % 60)
            sub.text = "مذاكرة شغالة. التطبيقات المختارة مقفولة"
        } else {
            status.text = "جاهز"
            sub.text = "التطبيقات مفتوحة. اختار وابدأ"
        }
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }
}
