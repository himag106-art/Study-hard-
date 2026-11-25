package com.ibrahim.studyhard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val BG = Color.parseColor("#0F3D3E")
    private val CARD = Color.parseColor("#17575A")
    private val GOLD = Color.parseColor("#F2A900")
    private val INK = Color.parseColor("#0F3D3E")

    private val chosen = HashSet<String>()
    private var appList: List<AppItem> = emptyList()
    private lateinit var status: TextView
    private lateinit var sub: TextView
    private lateinit var warn: TextView
    private lateinit var durIn: EditText
    private lateinit var reasonIn: EditText
    private lateinit var preview: TextView
    private lateinit var startBtn: Button
    private lateinit var appsBox: LinearLayout
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

    private fun input(hintText: String, def: String) = EditText(this).apply {
        this.hint = hintText
        if (def.isNotEmpty()) setText(def)
        textSize = 16f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setHintTextColor(Color.parseColor("#8FB0B0"))
        background = box(CARD, 14)
        inputType = InputType.TYPE_CLASS_TEXT
        isSingleLine = true
        setPadding(dp(12), dp(12), dp(12), dp(12))
    }

    private fun badge(t: String, color: Int, url: String) = TextView(this).apply {
        text = t
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = box(color, 24)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                toast("مش قادر أفتح الرابط")
            }
        }
    }

    // يفهم: ساعة، ساعتين، نص ساعة، ساعة ونص، 30 دقيقة، 1:30، 45
    private fun parseMinutes(raw: String): Long? {
        val sb = StringBuilder()
        for (c in raw.trim().lowercase()) {
            when (c) {
                'ـ' -> {}
                in '٠'..'٩' -> sb.append('0' + (c - '٠'))
                in '۰'..'۹' -> sb.append('0' + (c - '۰'))
                'أ', 'إ', 'آ' -> sb.append('ا')
                'ه' -> sb.append('ة')
                'ى' -> sb.append('ي')
                '٫', ',', '،' -> sb.append('.')
                else -> sb.append(c)
            }
        }
        var s = sb.toString().trim()
        if (s.isEmpty()) return null

        val words = listOf(
            "عشرين" to 20, "ثلاثين" to 30, "تلاتين" to 30, "اربعين" to 40, "خمسين" to 50,
            "ستين" to 60, "سبعين" to 70, "ثمانين" to 80, "تمانين" to 80, "تسعين" to 90,
            "واحد" to 1, "اتنين" to 2, "اثنين" to 2, "تلات" to 3, "ثلاث" to 3, "اربع" to 4,
            "خمس" to 5, "ست" to 6, "سبع" to 7, "تمان" to 8, "ثمان" to 8, "تسع" to 9, "عشر" to 10
        )
        for ((w, n) in words) {
            s = s.replace(Regex(w + "(?:ة)?\\s*(?=ساع|دقيق|دقاي|دقائ)"), "$n ")
        }

        if (Regex("^(?:نص|نصف)\\s*ساع").containsMatchIn(s)) return 30L
        if (Regex("^ربع\\s*ساع").containsMatchIn(s)) return 15L
        if (Regex("^(?:تلت|ثلث)\\s*ساع").containsMatchIn(s)) return 20L

        Regex("^(\\d{1,2}):(\\d{1,2})$").find(s)?.let {
            val t = it.groupValues[1].toLong() * 60 + it.groupValues[2].toLong()
            return if (t >= 1L && t <= 1440L) t else null
        }
        Regex("^\\d+(?:\\.\\d+)?$").find(s)?.let {
            val t = Math.round(s.toDouble())
            return if (t >= 1L && t <= 1440L) t else null
        }

        var hours = 0.0
        val hm = Regex("(\\d+(?:\\.\\d+)?)\\s*ساع").find(s)
        if (hm != null) hours = hm.groupValues[1].toDouble()
        else if (s.contains("ساعتين") || s.contains("ساعتان")) hours = 2.0
        else if (s.contains("ساعة")) hours = 1.0

        var mins = 0.0
        val mm = Regex("(\\d+)\\s*(?:دقيق|دقاي|دقائ|د(?!\\p{L}))").find(s)
        if (mm != null) mins = mm.groupValues[1].toDouble()
        else if (s.contains("دقيقتين") || s.contains("دقيقتان")) mins = 2.0

        if (hours > 0.0) {
            if (Regex("و\\s*(?:نص|نصف)").containsMatchIn(s)) mins += 30.0
            else if (Regex("و\\s*ربع").containsMatchIn(s)) mins += 15.0
            else if (Regex("و\\s*(?:تلت|ثلث)").containsMatchIn(s)) mins += 20.0
        }
        val total = Math.round(hours * 60 + mins)
        return if (total >= 1L && total <= 1440L) total else null
    }

    private fun hoursWord(n: Long): String = when (n) {
        1L -> "ساعة"
        2L -> "ساعتين"
        in 3L..10L -> "$n ساعات"
        else -> "$n ساعة"
    }

    private fun describe(m: Long): String {
        val hh = m / 60
        val mm = m % 60
        return when {
            hh > 0 && mm > 0 -> hoursWord(hh) + " و" + mm + " دقيقة"
            hh > 0 -> hoursWord(hh)
            else -> "$mm دقيقة"
        }
    }

    private fun showPreview() {
        val m = parseMinutes(durIn.text.toString())
        if (m == null) {
            preview.text = "اكتب مدة مفهومة، مثلاً: ساعتين أو 45 دقيقة"
            preview.setTextColor(GOLD)
        } else if (m >= 360L) {
            preview.text = "تحدي Impossible: " + describe(m) + ". مفيش إنهاء بدري!"
            preview.setTextColor(Color.parseColor("#FCA5A5"))
        } else {
            preview.text = "هتتقفل لمدة: " + describe(m) + ". مفيش إنهاء قبل الوقت"
            preview.setTextColor(GOLD)
        }
    }

    private fun paintRow(r: View, t: TextView, pkg: String) {
        val on = pkg in chosen
        r.background = box(if (on) Color.parseColor("#1F6F72") else Color.TRANSPARENT, 14)
        t.text = if (on) "مقفول" else "اختار"
        t.setTextColor(if (on) GOLD else Color.parseColor("#8FB0B0"))
        t.typeface = if (on) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun fillApps(list: List<AppItem>) {
        appList = list
        appsBox.removeAllViews()
        for (a in list) {
            val tagView = TextView(this).apply { textSize = 13f }
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
            }
            r.addView(
                ImageView(this).apply { setImageDrawable(a.icon) },
                LinearLayout.LayoutParams(dp(40), dp(40))
            )
            r.addView(TextView(this).apply {
                text = a.label
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(dp(12), 0, dp(12), 0)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            r.addView(tagView)
            paintRow(r, tagView, a.pkg)
            r.setOnClickListener {
                if (!chosen.add(a.pkg)) chosen.remove(a.pkg)
                paintRow(r, tagView, a.pkg)
            }
            appsBox.addView(r, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(2) })
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        chosen.addAll(Store.locked(this))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(BG)
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(40), dp(18), dp(8))
        }

        col.addView(TextView(this).apply {
            text = "Study Hard"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(GOLD)
        })
        col.addView(TextView(this).apply {
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
        warn = TextView(this).apply {
            text = "⚠ خدمة القفل مش شغالة. دوس هنا وفعّل Study Hard من إمكانية الوصول"
            textSize = 13f
            setTextColor(Color.parseColor("#FCA5A5"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
            visibility = View.GONE
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        card.addView(status)
        card.addView(sub)
        card.addView(warn)
        col.addView(card, LinearLayout.LayoutParams(-1, -2))

        col.addView(label("اكتب مدة المذاكرة"))
        preview = TextView(this).apply {
            textSize = 13f
            setPadding(dp(4), dp(6), dp(4), 0)
        }
        durIn = input("مثلاً: ساعة، ساعتين، 30 دقيقة، ساعة ونص", "30 دقيقة")
        durIn.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { showPreview() }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, c: Int) {}
        })
        col.addView(durIn, LinearLayout.LayoutParams(-1, -2))
        col.addView(preview)
        showPreview()

        val presets = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        val quick = listOf(
            "ساعة" to "ساعة", "ساعتين" to "ساعتين",
            "3 س" to "3 ساعات", "4 س" to "4 ساعات", "5 س" to "5 ساعات"
        )
        for ((lbl, value) in quick) {
            val c = TextView(this).apply {
                text = lbl
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = box(CARD, 14)
                setPadding(dp(4), dp(10), dp(4), dp(10))
                setOnClickListener { durIn.setText(value) }
            }
            presets.addView(c, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        col.addView(presets)

        val challenge = TextView(this).apply {
            text = "تحدي Impossible: 6 ساعات"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = box(Color.parseColor("#8B1E2D"), 14)
            setPadding(dp(8), dp(12), dp(8), dp(12))
            setOnClickListener { durIn.setText("6 ساعات") }
        }
        col.addView(challenge, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        col.addView(label("ليه بتذاكر؟ (اختياري)"))
        reasonIn = input("مثلاً: امتحان الفيزياء يوم الخميس", "")
        col.addView(reasonIn, LinearLayout.LayoutParams(-1, -2))

        col.addView(label("اختار التطبيقات اللي هتتقفل"))
        appsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(CARD, 20)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        appsBox.addView(TextView(this).apply {
            text = "جاري تحميل التطبيقات..."
            textSize = 14f
            setTextColor(Color.parseColor("#B7CFCF"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        })
        col.addView(appsBox, LinearLayout.LayoutParams(-1, -2))

        val about = TextView(this).apply {
            text = "البرنامج بيساعدك لو عندك تعفن دماغي، أو مش بتقدر تذاكر، أو بتقول «خمس دقايق وأقوم أذاكر» وبتفضل في دوامة بقية اليوم ومتذاكرش!"
            textSize = 14f
            setTextColor(Color.parseColor("#B7CFCF"))
            gravity = Gravity.CENTER
            background = box(CARD, 18)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        col.addView(about, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        col.addView(
            badge("TikTok  •  @ibrahimgamal999", Color.parseColor("#111111"), "https://www.tiktok.com/@ibrahimgamal999"),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        )
        col.addView(
            badge("Instagram  •  @hima_gamal20117", Color.parseColor("#C13584"), "https://www.instagram.com/hima_gamal20117"),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        )
        col.addView(View(this), LinearLayout.LayoutParams(-1, dp(20)))

        val sv = ScrollView(this)
        sv.addView(col)
        root.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(16))
        }
        startBtn = Button(this).apply {
            text = "ابدأ المذاكرة وقفل التطبيقات"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(INK)
            background = box(GOLD, 16)
            transformationMethod = null
            setOnClickListener { startSession() }
        }
        bottom.addView(startBtn, LinearLayout.LayoutParams(-1, dp(54)))
        val logBtn = Button(this).apply {
            text = "السجل"
            setTextColor(Color.WHITE)
            background = box(CARD, 16)
            transformationMethod = null
            setOnClickListener { startActivity(Intent(this@MainActivity, LogActivity::class.java)) }
        }
        bottom.addView(logBtn, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })
        root.addView(bottom, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)

        val cached = AppCache.items
        if (cached != null) {
            fillApps(cached)
        } else {
            Thread {
                val l = try {
                    AppCache.load(packageManager, packageName)
                } catch (e: Exception) {
                    emptyList<AppItem>()
                }
                runOnUiThread { if (!isFinishing) fillApps(l) }
            }.start()
        }
    }

    private fun startSession() {
        if (Store.active(this)) { toast("فيه جلسة شغالة دلوقتي"); return }
        if (chosen.isEmpty()) { toast("اختار تطبيق واحد على الأقل"); return }
        val total = parseMinutes(durIn.text.toString())
        if (total == null) { toast("المدة مش مفهومة. اكتب مثلاً: ساعة أو 30 دقيقة"); return }
        if (!serviceOn()) {
            toast("فعّل Study Hard من إعدادات إمكانية الوصول")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        AlertDialog.Builder(this)
            .setTitle(if (total >= 360L) "تحدي Impossible" else "تأكيد البدء")
            .setMessage("هتتقفل التطبيقات المختارة " + describe(total) + " ومفيش إنهاء قبل الوقت. والإعدادات وشاشة التثبيت هتتقفل كمان. متأكد؟")
            .setPositiveButton("ابدأ") { _, _ -> begin(total) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun begin(total: Long) {
        val names = appList.filter { it.pkg in chosen }.map { it.label }
        val now = System.currentTimeMillis()
        Store.setLocked(this, chosen)
        Store.setHard(this, true)
        Store.setEndAt(this, now + total * 60000L)

        val o = JSONObject()
        o.put("start", now)
        o.put("planned", total)
        o.put("hard", total >= 360L)
        o.put("reason", reasonIn.text.toString().trim())
        val arr = JSONArray()
        for (n in names) arr.put(n)
        o.put("apps", arr)
        Store.addLog(this, o)
        refresh()
    }

    private fun serviceOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains("$packageName/${BlockerService::class.java.name}") ||
            s.contains("$packageName/.BlockerService")
    }

    private fun refresh() {
        val left = (Store.endAt(this) - System.currentTimeMillis()) / 1000
        warn.visibility = if (serviceOn()) View.GONE else View.VISIBLE
        if (left > 0) {
            status.text = "%02d:%02d:%02d".format(left / 3600, (left % 3600) / 60, left % 60)
            sub.text = "مذاكرة شغالة. مفيش إنهاء قبل الوقت. اجتهد!"
            startBtn.isEnabled = false
            startBtn.alpha = 0.4f
            startBtn.text = "مفيش إنهاء قبل الوقت"
        } else {
            if (Store.hard(this)) Store.setHard(this, false)
            status.text = "جاهز"
            sub.text = "التطبيقات مفتوحة. اختار وابدأ"
            startBtn.isEnabled = true
            startBtn.alpha = 1f
            startBtn.text = "ابدأ المذاكرة وقفل التطبيقات"
        }
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }
}
