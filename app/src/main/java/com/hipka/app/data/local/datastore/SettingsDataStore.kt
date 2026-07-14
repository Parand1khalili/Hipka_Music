package com.hipka.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hipka.app.core.locale.LocaleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hipka_settings")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val languageCode: String = LocaleManager.LANGUAGE_ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            languageCode = prefs[Keys.LANGUAGE] ?: LocaleManager.LANGUAGE_ENGLISH,
            themeMode = prefs[Keys.THEME_MODE]?.let { raw ->
                runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM
        )
    }

    suspend fun setLanguage(languageCode: String) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = languageCode }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}