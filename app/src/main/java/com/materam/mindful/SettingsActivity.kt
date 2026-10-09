package com.materam.mindful

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var box: LinearLayout
    private val dialogTheme = android.R.style.Theme_DeviceDefault_Dialog_Alert

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (28 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        setContentView(ScrollView(this).apply { addView(box) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val prefs = Prefs(this)
        val apps = Apps.load(this)
        box.removeAllViews()

        title("settings")

        row("Set Mindful as your home app", "Makes the launcher replace your current home screen.") {
            Apps.safeStart(this, Intent(Settings.ACTION_HOME_SETTINGS))
        }

        section("mindful pause")
        row("Pause apps", "${prefs.mindful.size} apps ask you why before opening.") {
            pickApps("Pause before opening", apps, prefs.mindful) { prefs.mindful = it }
        }
        row("Pause length", "${prefs.pauseSeconds} seconds") {
            val choices = listOf(5, 10, 20, 30, 60)
            AlertDialog.Builder(this, dialogTheme)
                .setTitle("Pause length")
                .setItems(choices.map { "$it seconds" }.toTypedArray()) { _, which ->
                    prefs.pauseSeconds = choices[which]
                    render()
                }
                .show()
        }
        row(
            "Focus hours",
            if (prefs.focusEnabled) "On · ${prefs.focusLabel()} · pauses become 30 s or longer" else "Off",
        ) { editFocusHours(prefs) }

        section("home & list")
        row("Home apps", "${prefs.favorites.size} of ${Prefs.MAX_FAVORITES}") {
            pickApps("Home apps (max ${Prefs.MAX_FAVORITES})", apps, prefs.favorites.toSet()) { chosen ->
                val kept = prefs.favorites.filter { it in chosen }
                val added = apps.map { it.pkg }.filter { it in chosen && it !in kept }
                val result = kept + added
                if (result.size > Prefs.MAX_FAVORITES) {
                    Toast.makeText(this, "Only the first ${Prefs.MAX_FAVORITES} were kept", Toast.LENGTH_SHORT).show()
                }
                prefs.favorites = result
            }
        }
        row("Hidden apps", "${prefs.hidden.size} hidden from the list") {
            pickApps("Hidden apps", apps, prefs.hidden) { prefs.hidden = it }
        }

        section("reflect")
        row("Today", "Opens of your pause apps today") { showToday(prefs, apps) }
        row("Intentions", "${prefs.intentions.size} reasons you wrote down") { showIntentions(prefs) }

        section("about")
        row("Mindful Launcher ${BuildConfigLite.versionName(this)}", "Everything stays on this phone. No internet permission.") {
            Apps.safeStart(this, Intent(Intent.ACTION_VIEW, Uri.parse("https://materam.github.io/PBL/")))
        }
    }

    private fun editFocusHours(prefs: Prefs) {
        val toggle = if (prefs.focusEnabled) "Turn off" else "Turn on"
        AlertDialog.Builder(this, dialogTheme)
            .setTitle("Focus hours")
            .setItems(arrayOf(toggle, "Start: ${Prefs.hourLabel(prefs.focusStart)}", "End: ${Prefs.hourLabel(prefs.focusEnd)}")) { _, which ->
                when (which) {
                    0 -> {
                        prefs.focusEnabled = !prefs.focusEnabled
                        render()
                    }
                    1 -> pickHour("Focus starts at") { prefs.focusStart = it; render() }
                    2 -> pickHour("Focus ends at") { prefs.focusEnd = it; render() }
                }
            }
            .show()
    }

    private fun pickHour(title: String, onPick: (Int) -> Unit) {
        val hours = (0..23).map { Prefs.hourLabel(it) }.toTypedArray()
        AlertDialog.Builder(this, dialogTheme)
            .setTitle(title)
            .setItems(hours) { _, which -> onPick(which) }
            .show()
    }

    private fun pickApps(title: String, apps: List<AppEntry>, selected: Set<String>, onSave: (Set<String>) -> Unit) {
        val labels = apps.map { it.label }.toTypedArray()
        val checked = BooleanArray(apps.size) { apps[it].pkg in selected }
        AlertDialog.Builder(this, dialogTheme)
            .setTitle(title)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton("Save") { _, _ ->
                onSave(apps.filterIndexed { i, _ -> checked[i] }.map { it.pkg }.toSet())
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToday(prefs: Prefs, apps: List<AppEntry>) {
        val lines = apps.filter { it.pkg in prefs.mindful }
            .map { it.label to prefs.opensToday(it.pkg) }
            .sortedByDescending { it.second }
            .map { "${it.first}  —  ${it.second}" }
        AlertDialog.Builder(this, dialogTheme)
            .setTitle("Today")
            .setMessage(if (lines.isEmpty()) "No pause apps yet. Hold an app in the list and choose “Pause before opening”." else lines.joinToString("\n"))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showIntentions(prefs: Prefs) {
        val log = prefs.intentions
        AlertDialog.Builder(this, dialogTheme)
            .setTitle("Your intentions")
            .setMessage(if (log.isEmpty()) "Nothing yet. Reasons you write in the pause screen show up here." else log.joinToString("\n\n"))
            .setPositiveButton("OK", null)
            .setNeutralButton("Clear") { _, _ ->
                prefs.clearIntentions()
                render()
            }
            .show()
    }

    private fun title(value: String) {
        box.addView(TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            typeface = android.graphics.Typeface.create("sans-serif-thin", android.graphics.Typeface.NORMAL)
            setTextColor(getColor(R.color.fg))
            setPadding(0, 0, 0, dp(12))
        })
    }

    private fun section(value: String) {
        box.addView(TextView(this).apply {
            text = value.uppercase()
            letterSpacing = 0.15f
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(getColor(R.color.faint))
            setPadding(0, dp(28), 0, dp(4))
        })
    }

    private fun row(main: String, sub: String, onClick: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            isClickable = true
            setOnClickListener { onClick() }
        }
        row.addView(TextView(this).apply {
            text = main
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(getColor(R.color.fg))
        })
        row.addView(TextView(this).apply {
            text = sub
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(2), 0, 0)
        })
        box.addView(row)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

/** Reads the version name without needing the generated BuildConfig class. */
object BuildConfigLite {
    fun versionName(activity: Activity): String = try {
        "v" + (activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "")
    } catch (e: Exception) {
        ""
    }
}
