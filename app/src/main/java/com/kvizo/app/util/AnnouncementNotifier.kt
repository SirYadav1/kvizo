package com.kvizo.app.util

import android.content.Context
import com.kvizo.app.R
import com.kvizo.app.data.RemoteNotification

/**
 * Community announcements — no Firebase / third-party push.
 * Background checks run via WorkManager (AnnouncementWorker) so system
 * notifications arrive even when the app is closed, as long as the phone is on.
 */
object AnnouncementNotifier {

    const val CHANNEL_ID = "announcements"

    /** Posts a single system notification for an announcement. Safe to call on any thread. */
    fun post(context: Context, n: RemoteNotification) {
        try {
            val app = context.applicationContext
            val nm = app.getSystemService(android.app.NotificationManager::class.java)
            nm.createNotificationChannel(
                android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Announcements",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Community announcements from Kvizo" }
            )
            val notif = androidx.core.app.NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(n.title)
                .setContentText(n.body)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(n.body))
                .setSound(android.net.Uri.parse("android.resource://" + app.packageName + "/" + R.raw.sound_bell))
                .setAutoCancel(true)
                .build()
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    app, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.NotificationManagerCompat.from(app).notify(1001 + (n.id.hashCode() % 1000), notif)
            }
        } catch (_: Exception) {
        }
    }
}
