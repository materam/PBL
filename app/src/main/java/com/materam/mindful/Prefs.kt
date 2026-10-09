package com.materam.mindful

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** All app state lives on the device in SharedPreferences. Nothing leaves the phone. */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("mindful", Context.MODE_PRIVATE)

    private fun readSet(key: String): Set<String> = HashSet(sp.getStringSet(key, emptySet()) ?: emptySet())

    private fun writeSet(key: String, value: Set<String>) {
        sp.edit().putStringSet(key, HashSet(value)).apply()
    }

    /** Ordered list of package names shown on the home screen. */
    var favorites: List<String>
        get() = (sp.getString("favorites", "") ?: "").split("|").filter { it.isNotBlank() }
        set(value) {
            sp.edit().putString("favorites", value.distinct().take(MAX_FAVORITES).joinToString("|")).apply()
        }

    /** Apps that require a mindful pause before opening. */
    var mindful: Set<String>
        get() = readSet("mindful")
        set(value) = writeSet("mindful", value)

    /** Apps hidden from the app list. */
    var hidden: Set<String>
        get() = readSet("hidden")
        set(value) = writeSet("hidden", value)

    var pauseSeconds: Int
        get() = sp.getInt("pause_seconds", 10)
        set(value) {
            sp.edit().putInt("pause_seconds", value).apply()
        }

    var focusEnabled: Boolean
        get() = sp.getBoolean("focus_enabled", false)
        set(value) {
            sp.edit().putBoolean("focus_enabled", value).apply()
        }

    var focusStart: Int
        get() = sp.getInt("focus_start", 22)
        set(value) {
            sp.edit().putInt("focus_start", value).apply()
        }

    var focusEnd: Int
        get() = sp.getInt("focus_end", 7)
        set(value) {
            sp.edit().putInt("focus_end", value).apply()
        }

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(value) {
            sp.edit().putBoolean("onboarded", value).apply()
        }

    fun isFocusTime(now: Calendar = Calendar.getInstance()): Boolean {
        if (!focusEnabled || focusStart == focusEnd) return false
        val hour = now.get(Calendar.HOUR_OF_DAY)
        return if (focusStart < focusEnd) {
            hour in focusStart until focusEnd
        } else {
            hour >= focusStart || hour < focusEnd
        }
    }

    fun focusLabel(): String = "${hourLabel(focusStart)}–${hourLabel(focusEnd)}"

    private fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun opensKey(pkg: String) = "opens_${today()}_$pkg"

    fun opensToday(pkg: String): Int = sp.getInt(opensKey(pkg), 0)

    fun recordOpen(pkg: String) {
        val editor = sp.edit()
        // Drop counters from previous days so storage never grows.
        val todayPrefix = "opens_${today()}_"
        sp.all.keys.filter { it.startsWith("opens_") && !it.startsWith(todayPrefix) }.forEach { editor.remove(it) }
        editor.putInt(opensKey(pkg), opensToday(pkg) + 1).apply()
    }

    val intentions: List<String>
        get() = (sp.getString("intentions", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun addIntention(label: String, text: String, minutes: Int) {
        val time = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date())
        val entry = "$time  ·  $label  ·  $minutes min  —  “${text.replace("\n", " ").trim()}”"
        val updated = listOf(entry) + intentions
        sp.edit().putString("intentions", updated.take(MAX_INTENTIONS).joinToString("\n")).apply()
    }

    fun clearIntentions() {
        sp.edit().remove("intentions").apply()
    }

    companion object {
        const val MAX_FAVORITES = 7
        const val MAX_INTENTIONS = 100

        fun hourLabel(hour: Int): String = String.format(Locale.US, "%02d:00", hour)
    }
}
