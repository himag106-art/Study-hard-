package com.ibrahim.studyhard

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val chosen = HashSet<String>()
    private lateinit var status: TextView
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { refresh(); h.postDelayed(this, 1000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        chosen.addAll(Store.locked(this))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(40, 60, 40, 30)
        }
        root.addView(TextView(this).apply {
            text = "عمل أخوك إبراهيم جمال"; textSize = 14f; gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Study Hard"; textSize = 28f; typeface = Typeface.DEFAULT_BOLD
        })
        status = TextView(this).apply { textSize = 18f; setPadding(0, 12, 0, 12) }
        root.addView(status)
        root.addView(TextView(this).apply { text = "اختار التطبيقات اللي هتتقفل:" })

        val pm = packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .filter { it.second != packageName }
            .sortedBy { it.first.lowercase() }

        val list = ListView(this)
        list.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, apps.map { it.first })
        apps.forEachIndexed { i, a -> list.setItemChecked(i, a.second in chosen) }
        list.setOnItemClickListener { _, _, i, _ ->
            if (list.isItemChecked(i)) chosen.add(apps[i].second) else chosen.remove(apps[i].second)
        }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        val minutes = EditText(this).apply {
            hint = "مدة المذاكرة بالدقايق"; setText("25")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        root.addView(minutes)

        root.addView(Button(this).apply {
            text = "ابدأ المذاكرة وقفل التطبيقات"
            setOnClickListener {
                if (chosen.isEmpty()) { toast("اختار تطبيق واحد على الأقل"); return@setOnClickListener }
                if (!serviceOn()) {
                    toast("فعّل Study Hard من إعدادات إمكانية الوصول")
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    return@setOnClickListener
                }
                val m = minutes.text.toString().toLongOrNull()?.coerceIn(1, 600) ?: 25
                Store.setLocked(this@MainActivity, chosen)
                Store.setEndAt(this@MainActivity, System.currentTimeMillis() + m * 60000)
                refresh()
            }
        })
        root.addView(Button(this).apply {
            text = "خلصت مذاكرة"
            setOnClickListener { Store.setEndAt(this@MainActivity, 0); refresh() }
        })
        setContentView(root)
    }

    private fun serviceOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains("$packageName/${BlockerService::class.java.name}")
    }

    private fun refresh() {
        val left = (Store.endAt(this) - System.currentTimeMillis()) / 1000
        status.text = if (left > 0) "مذاكرة شغالة، فاضل %02d:%02d".format(left / 60, left % 60)
        else "جاهز. التطبيقات مفتوحة."
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }
}
