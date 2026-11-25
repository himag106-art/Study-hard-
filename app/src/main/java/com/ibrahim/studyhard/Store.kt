package com.ibrahim.studyhard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.json.JSONArray
import org.json.JSONObject

class AppItem(val label: String, val pkg: String, val icon: Drawable)

object AppCache {
    @Volatile
    var items: List<AppItem>? = null

    @Synchronized
    fun load(pm: PackageManager, self: String): List<AppItem> {
        val have = items
        if (have != null) return have
        val l = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
            .filter { it.pkg != self }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
        items = l
        return l
    }
}

object Store {
    private fun p(c: Context) = c.getSharedPreferences("sh", Context.MODE_PRIVATE)

    fun locked(c: Context): Set<String> = p(c).getStringSet("locked", emptySet()) ?: emptySet()
    fun setLocked(c: Context, s: Set<String>) = p(c).edit().putStringSet("locked", HashSet(s)).apply()
    fun endAt(c: Context): Long = p(c).getLong("endAt", 0L)
    fun setEndAt(c: Context, t: Long) = p(c).edit().putLong("endAt", t).apply()
    fun active(c: Context) = System.currentTimeMillis() < endAt(c)

    fun hard(c: Context): Boolean = p(c).getBoolean("hard", false)
    fun setHard(c: Context, v: Boolean) = p(c).edit().putBoolean("hard", v).apply()

    fun alive(c: Context): Boolean = p(c).getBoolean("alive", false)
    fun setAlive(c: Context, v: Boolean) = p(c).edit().putBoolean("alive", v).apply()

    val HARD_BLOCK = setOf(
        "com.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller"
    )

    fun log(c: Context): JSONArray =
        try { JSONArray(p(c).getString("log", "[]")) } catch (e: Exception) { JSONArray() }

    fun addLog(c: Context, o: JSONObject) {
        val a = log(c)
        a.put(o)
        p(c).edit().putString("log", a.toString()).apply()
    }

    fun endLastEarly(c: Context) {
        val a = log(c)
        if (a.length() == 0) return
        val o = a.getJSONObject(a.length() - 1)
        val now = System.currentTimeMillis()
        if (o.optLong("endedAt", 0L) == 0L && now < o.getLong("start") + o.getLong("planned") * 60000L) {
            o.put("endedAt", now)
            p(c).edit().putString("log", a.toString()).apply()
        }
    }
}
