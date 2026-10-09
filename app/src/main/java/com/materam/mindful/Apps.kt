package com.materam.mindful

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

data class AppEntry(val label: String, val pkg: String, val component: ComponentName)

object Apps {

    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    pkg = it.activityInfo.packageName,
                    component = ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                )
            }
            .distinctBy { it.component }
            .sortedBy { it.label.lowercase() }
    }

    /** Opens an app, routing it through the mindful pause if the user asked for one. */
    fun open(activity: Activity, app: AppEntry) {
        val prefs = Prefs(activity)
        if (app.pkg in prefs.mindful) {
            val gate = Intent(activity, GateActivity::class.java)
                .putExtra(GateActivity.EXTRA_PKG, app.pkg)
                .putExtra(GateActivity.EXTRA_CLASS, app.component.className)
                .putExtra(GateActivity.EXTRA_LABEL, app.label)
            activity.startActivity(gate)
        } else {
            launchDirect(activity, app.component)
        }
    }

    fun launchDirect(context: Context, component: ComponentName) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't open that app", Toast.LENGTH_SHORT).show()
        }
    }

    /** Long-press menu shared by the home screen and the app list. */
    fun showMenu(activity: Activity, app: AppEntry, onChanged: () -> Unit) {
        val prefs = Prefs(activity)
        val isFavorite = app.pkg in prefs.favorites
        val isMindful = app.pkg in prefs.mindful
        val actions = mutableListOf<Pair<String, () -> Unit>>()

        actions += (if (isFavorite) "Remove from home" else "Add to home") to {
            if (isFavorite) {
                prefs.favorites = prefs.favorites - app.pkg
            } else if (prefs.favorites.size >= Prefs.MAX_FAVORITES) {
                Toast.makeText(activity, "Home holds ${Prefs.MAX_FAVORITES} apps. Remove one first.", Toast.LENGTH_SHORT).show()
            } else {
                prefs.favorites = prefs.favorites + app.pkg
            }
            onChanged()
        }
        actions += (if (isMindful) "Turn off mindful pause" else "Pause before opening") to {
            prefs.mindful = if (isMindful) prefs.mindful - app.pkg else prefs.mindful + app.pkg
            onChanged()
        }
        actions += "Hide from list" to {
            prefs.hidden = prefs.hidden + app.pkg
            prefs.favorites = prefs.favorites - app.pkg
            onChanged()
        }
        actions += "App info" to {
            safeStart(activity, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")))
        }
        actions += "Uninstall" to {
            safeStart(activity, Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")))
        }

        AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.label)
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }
            .show()
    }

    fun safeStart(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Not available on this phone", Toast.LENGTH_SHORT).show()
        }
    }
}
