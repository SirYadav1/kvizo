package com.kvizo.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: String,
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val leaderboardOnline: Boolean = false,
    val autoUpdateCheck: Boolean = true,
    val updateNotifications: Boolean = true,
    val announcementsEnabled: Boolean = true,
    val language: String = "en"
)

class SettingsRepo(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val soundKey = booleanPreferencesKey("sound_enabled")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val leaderboardOnlineKey = booleanPreferencesKey("leaderboard_online")
    private val autoUpdateKey = booleanPreferencesKey("auto_update_check")
    private val updateNotifKey = booleanPreferencesKey("update_notifications")
    private val announcementsKey = booleanPreferencesKey("announcements_enabled")
    private val languageKey = stringPreferencesKey("language")

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[themeKey] ?: "system",
            soundEnabled = p[soundKey] ?: true,
            hapticsEnabled = p[hapticsKey] ?: true,
            leaderboardOnline = p[leaderboardOnlineKey] ?: false,
            autoUpdateCheck = p[autoUpdateKey] ?: true,
            updateNotifications = p[updateNotifKey] ?: true,
            announcementsEnabled = p[announcementsKey] ?: true,
            language = p[languageKey] ?: "en"
        )
    }

    suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[themeKey] = mode }
    suspend fun setSoundEnabled(v: Boolean) = context.dataStore.edit { it[soundKey] = v }
    suspend fun setHapticsEnabled(v: Boolean) = context.dataStore.edit { it[hapticsKey] = v }
    suspend fun setAutoUpdateCheck(v: Boolean) = context.dataStore.edit { it[autoUpdateKey] = v }
    suspend fun setUpdateNotifications(v: Boolean) = context.dataStore.edit { it[updateNotifKey] = v }
    suspend fun setAnnouncementsEnabled(v: Boolean) = context.dataStore.edit { it[announcementsKey] = v }
    suspend fun setLanguage(lang: String) = context.dataStore.edit { it[languageKey] = lang }

    suspend fun lastAnnouncementSeen(): Long {
        context.dataStore.data.first().let { p -> p[lastSeenKey]?.let { return it } }
        return 0L
    }

    suspend fun setLastAnnouncementSeen(ts: Long) = context.dataStore.edit { it[lastSeenKey] = ts }
    suspend fun setLeaderboardOnline(v: Boolean) = context.dataStore.edit { it[leaderboardOnlineKey] = v }

    suspend fun getDeviceId(): String {
        context.dataStore.data.first().let { p ->
            p[deviceIdKey]?.let { return it }
        }
        val id = UUID.randomUUID().toString().replace("-", "").take(20)
        context.dataStore.edit { it[deviceIdKey] = id }
        return id
    }

    private val deviceIdKey = stringPreferencesKey("device_id")
    private val lastSeenKey = longPreferencesKey("last_announcement_seen")
}
