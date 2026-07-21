package com.hipka.app.presentation.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Language and theme state live in MainViewModel (app-wide, above the nav
 * graph) — SettingsScreen reads/writes those via the same MainUiState /
 * MainIntent already threaded through HipkaNavGraph. This ViewModel only
 * owns logout, which is local to this screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    fun logout() {
        viewModelScope.launch { sessionManager.clearCurrentUser() }
    }
}
