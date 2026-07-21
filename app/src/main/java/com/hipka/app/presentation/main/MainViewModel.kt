package com.hipka.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.core.locale.LocaleManager
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.data.local.datastore.SettingsDataStore
import com.hipka.app.data.local.datastore.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class MainUiState(
    val languageCode: String = LocaleManager.LANGUAGE_ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true
)

sealed interface MainIntent {
    data class ChangeLanguage(val languageCode: String) : MainIntent
    data class ChangeThemeMode(val themeMode: ThemeMode) : MainIntent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        languageCode = settings.languageCode,
                        themeMode = settings.themeMode,
                        isLoading = false
                    )
                }
                // Keeps AppCompat's per-app language in sync with the
                // persisted choice (covers first launch after install).
                if (LocaleManager.currentLanguageTag() != settings.languageCode) {
                    LocaleManager.setLocale(settings.languageCode)
                }
            }
        }
    }

    fun onIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.ChangeLanguage -> changeLanguage(intent.languageCode)
            is MainIntent.ChangeThemeMode -> changeThemeMode(intent.themeMode)
        }
    }

    private fun changeLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(languageCode)
            LocaleManager.setLocale(languageCode) // triggers activity recreation + RTL/LTR flip
        }
    }

    private fun changeThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }
}
