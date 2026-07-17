package com.hipka.app.presentation.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.currentUserId.collect { userId ->
                if (userId == null) loadDemoUserPicker() else loadCurrentUser(userId)
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.SelectDemoUser -> selectDemoUser(intent.userId)
            ProfileIntent.Logout -> logout()
            ProfileIntent.Retry -> retry()
        }
    }

    private suspend fun loadDemoUserPicker() {
        _uiState.update { it.copy(isLoading = true, currentUser = null, errorMessage = null) }
        runCatching { userRepository.getAllUsers() }
            .onSuccess { users -> _uiState.update { it.copy(allUsers = users, isLoading = false) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
    }

    private suspend fun loadCurrentUser(userId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { userRepository.getUserById(userId) }
            .onSuccess { user -> _uiState.update { it.copy(currentUser = user, isLoading = false) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
    }

    private fun selectDemoUser(userId: String) {
        viewModelScope.launch { sessionManager.setCurrentUser(userId) }
    }

    private fun logout() {
        viewModelScope.launch { sessionManager.clearCurrentUser() }
    }

    private fun retry() {
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.first()
            if (userId == null) loadDemoUserPicker() else loadCurrentUser(userId)
        }
    }
}