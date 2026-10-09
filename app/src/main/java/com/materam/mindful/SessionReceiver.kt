package com.materam.mindful

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when the session length the user chose in the mindful pause runs out. */
class SessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "app"
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Session reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        val home = PendingIntent.getActivity(
            context, 0,
            Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your $minutes-minute $label session is up")
            .setContentText("Tap to put the phone down and go home.")
            .setContentIntent(home)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(label.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notifications not allowed; nothing else to do.
        }
    }

    companion object {
        private const val CHANNEL = "sessions"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_MINUTES = "minutes"

        fun schedule(context: Context, pkg: String, label: String, minutes: Int) {
            val alarms = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, SessionReceiver::class.java)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_MINUTES, minutes)
            val pending = PendingIntent.getBroadcast(
                context, pkg.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + minutes * 60_000L,
                pending,
            )
        }
    }
}
