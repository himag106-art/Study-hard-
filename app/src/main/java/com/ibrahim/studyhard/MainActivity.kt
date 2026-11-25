package com.ibrahim.studyhard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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

    private class AppItem(val label: String, val pkg: String, val icon: Drawable)

    private val chosen = HashSet<String>()
    private var appList: List<AppItem> = emptyList()
    private lateinit var status: TextView
    private lateinit var sub: TextView
    private lateinit var durIn: EditText
    private lateinit var reasonIn: EditText
    private lateinit var preview: TextView
    private lateinit var stopBtn: Button
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
        s
