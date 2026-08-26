package com.quizforge.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: String,   // "system" | "light" | "dark"
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val timerOnBackground: String // "pause" | "submit"
)

class SettingsRepo(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val soundKey = booleanPreferencesKey("sound_enabled")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val timerBgKey = stringPreferencesKey("timer_background")

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[themeKey] ?: "system",
            soundEnabled = p[soundKey] ?: true,
            hapticsEnabled = p[hapticsKey] ?: true,
            timerOnBackground = p[timerBgKey] ?: "pause"
        )
    }

    suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[themeKey] = mode }

    suspend fun setSoundEnabled(v: Boolean) = context.dataStore.edit { it[soundKey] = v }

    suspend fun setHapticsEnabled(v: Boolean) = context.dataStore.edit { it[hapticsKey] = v }

    suspend fun setTimerOnBackground(v: String) = context.dataStore.edit { it[timerBgKey] = v }
}
