package com.kvizo.app.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kvizo.app.data.SettingsRepo
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Background notification engine — fetches announcements from the Kvizo server
 * and posts system notifications. Runs via WorkManager, so it survives process
 * kills and reboots: notifications arrive even when the app is closed,
 * as long as the phone is on.
 *
 * Old announcements are never re-delivered: on the very first run after
 * install, the server's current announcement time is stored as the baseline
 * and nothing is posted — only announcements created AFTER that are live.
 */
class AnnouncementWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        return try {
            val repo = SettingsRepo(app)
            CommunityFetch.CacheProvider.appContext = app.applicationContext
            val list = fetchSignedNotifications()
            if (list.isEmpty()) return Result.success() // offline / nothing on server

            // First run after install: seed the baseline so old announcements
            // never pop up as notifications.
            if (repo.lastAnnouncementSeen() == 0L) {
                repo.setLastAnnouncementSeen(
                    maxOf(list.maxOfOrNull { it.createdAt } ?: 0L, System.currentTimeMillis())
                )
                return Result.success()
            }

            val settings = repo.settings.first()
            if (!settings.announcementsEnabled) return Result.success()

            val lastSeen = repo.lastAnnouncementSeen()
            val fresh = list.filter { it.id.isNotBlank() && it.createdAt > lastSeen }
            if (fresh.isEmpty()) return Result.success()

            val canPost = androidx.core.content.ContextCompat.checkSelfPermission(
                app, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!canPost) return Result.retry() // wait for permission, don't swallow the batch

            fresh.forEach { AnnouncementNotifier.post(app, it) }
            repo.setLastAnnouncementSeen(fresh.maxOf { it.createdAt })
            Result.success()
        } catch (_: Exception) {
            // offline / server down — retry on the next run
            Result.retry()
        }
    }

    companion object {
        const val NOTIFICATIONS_FILE = "notifications.json"

        /** Fetches notifications.json from the signed GitHub Pages source (Ed25519 verified, cached offline). */
        private suspend fun fetchSignedNotifications(): List<com.kvizo.app.data.RemoteNotification> =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val bytes = CommunityFetch.fetch(NOTIFICATIONS_FILE)
                    val list = mutableListOf<com.kvizo.app.data.RemoteNotification>()
                    val arr = org.json.JSONObject(String(bytes, Charsets.UTF_8)).optJSONArray("notifications")
                        ?: return@withContext emptyList<com.kvizo.app.data.RemoteNotification>()
                    for (i in 0 until minOf(arr.length(), 50)) {
                        val o = arr.getJSONObject(i)
                        list.add(
                            com.kvizo.app.data.RemoteNotification(
                                id = sanitize(o.optString("id", "")),
                                title = sanitize(o.optString("title", "")),
                                body = sanitize(o.optString("body", "")),
                                createdAt = o.optLong("createdAt", o.optLong("created_at", 0L))
                            )
                        )
                    }
                    list.sortedByDescending { it.createdAt }
                } catch (_: Exception) {
                    emptyList<com.kvizo.app.data.RemoteNotification>()
                }
            }

        suspend fun fetchSignedNotificationsPublic(): List<com.kvizo.app.data.RemoteNotification> =
            fetchSignedNotifications()

        private fun sanitize(t: String): String =
            t.replace(Regex("[\\p{Cntrl}]"), "").replace(Regex("<[^>]*>"), "").trim()

        const val PERIODIC_NAME = "kvizo-announcements"
        const val NOW_NAME = "kvizo-announcements-now"

        /** Schedules the periodic background check (every 15 min, network required). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AnnouncementWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Immediate one-shot check (used on app launch) — replaces the previous "now" job. */
        fun checkNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AnnouncementWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(NOW_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
