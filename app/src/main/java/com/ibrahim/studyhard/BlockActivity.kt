package com.ibrahim.studyhard

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class BlockActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val mins = (Store.endAt(this) - System.currentTimeMillis()).coerceAtLeast(0) / 60000 + 1
        setContentView(TextView(this).apply {
            text = "التطبيق ده مقفول دلوقتي.\nفاضل حوالي $mins دقيقة. كمّل مذاكرة!"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setOnClickListener { finish() }
        })
    }
}
