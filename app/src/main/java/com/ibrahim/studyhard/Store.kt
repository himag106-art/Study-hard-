package com.ibrahim.studyhard

import android.content.Context

object Store {
    private fun p(c: Context) = c.getSharedPreferences("sh", Context.MODE_PRIVATE)
    fun locked(c: Context): Set<String> = p(c).getStringSet("locked", emptySet()) ?: emptySet()
    fun setLocked(c: Context, s: Set<String>) = p(c).edit().putStringSet("locked", HashSet(s)).apply()
    fun endAt(c: Context): Long = p(c).getLong("endAt", 0L)
    fun setEndAt(c: Context, t: Long) = p(c).edit().putLong("endAt", t).apply()
    fun active(c: Context) = System.currentTimeMillis() < endAt(c)
}
