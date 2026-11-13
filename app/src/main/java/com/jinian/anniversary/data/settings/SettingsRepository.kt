package com.jinian.anniversary.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jinian_settings")

class SettingsRepository(private val context: Context) {

    private val themeColorKey = intPreferencesKey("theme_color")
    private val themeModeKey = intPreferencesKey("theme_mode")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeColor = prefs[themeColorKey]?.let { key ->
                runCatching { ThemeColor.entries.getOrNull(key) }.getOrNull()
            } ?: ThemeColor.BANANA_RED,
            themeMode = prefs[themeModeKey]?.let { key ->
                runCatching { ThemeMode.entries.getOrNull(key) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun setThemeColor(value: ThemeColor) {
        context.dataStore.edit { it[themeColorKey] = value.ordinal }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = value.ordinal }
    }
}