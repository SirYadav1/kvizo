package com.quizforge.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quizforge.app.R
import com.quizforge.app.data.RemoteApi
import com.quizforge.app.data.RemoteNotification
import com.quizforge.app.data.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Community announcements — no Firebase / third-party push.
 * The app polls the QuizForge server; new announcements are shown as
 * Android system notifications (bell sound) and as in-app banners.
 */
object AnnouncementNotifier {

    const val CHANNEL_ID = "announcements"
    const val ACTION_CHECK = "com.quizforge.app.ACTION_CHECK_ANNOUNCEMENTS"

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
                ).apply { description = "Community announcements from QuizForge" }
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

    /** Schedules the periodic background check (every 15 minutes, inexact — battery friendly). */
    fun schedule(context: Context) {
        try {
            val app = context.applicationContext
            val am = app.getSystemService(AlarmManager::class.java)
            val pi = PendingIntent.getBroadcast(
                app, 0,
                Intent(app, AnnouncementAlarmReceiver::class.java).setAction(ACTION_CHECK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + 15 * 60_000L,
                15 * 60_000L,
                pi
            )
        } catch (_: Exception) {
        }
    }
}

/** Periodic receiver: fetches announcements and posts notifications for new ones. */
class AnnouncementAlarmReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext
                val repo = SettingsRepo(app)
                val settings = repo.settings.first()
                if (!settings.announcementsEnabled) return@launch
                val list = RemoteApi().fetchNotifications()
                if (list.isEmpty()) return@launch
                val lastSeen = repo.lastAnnouncementSeen()
                val fresh = list.filter { it.id.isNotBlank() && it.createdAt > lastSeen }
                if (fresh.isNotEmpty()) {
                    repo.setLastAnnouncementSeen(fresh.maxOf { it.createdAt })
                    fresh.forEach { AnnouncementNotifier.post(app, it) }
                }
            } catch (_: Exception) {
                // offline — the next tick will retry
            } finally {
                // self-heal: keep the periodic check alive even if the app process was killed
                AnnouncementNotifier.schedule(context)
                pending.finish()
            }
        }
    }
}
