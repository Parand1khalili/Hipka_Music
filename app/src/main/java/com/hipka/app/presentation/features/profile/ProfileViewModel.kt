package com.hipka.app.presentation.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.currentUserId.collect { userId ->
                if (userId != null) {
                    loadCurrentUser(userId)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = null,
                            followingIds = emptySet(),
                            followerIds = emptySet()
                        )
                    }
                }
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Logout -> logout()
            ProfileIntent.Retry -> retry()
            ProfileIntent.UpgradePremium -> upgradePremium()
            is ProfileIntent.SelectDemoUser -> { /* سیستم دمو حذف شد */ }
        }
    }

    private suspend fun loadCurrentUser(userId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            coroutineScope {
                val userDeferred = async { userRepository.getUserById(userId) }
                val followingDeferred = async { userRepository.getFollowingIds(userId) }
                val followerDeferred = async { userRepository.getFollowerIds(userId) }

                Triple(userDeferred.await(), followingDeferred.await(), followerDeferred.await())
            }
        }.onSuccess { (user, followingIds, followerIds) ->
            _uiState.update {
                it.copy(
                    currentUser = user,
                    followingIds = followingIds.toSet(),
                    followerIds = followerIds.toSet(),
                    isLoading = false
                )
            }
        }.onFailure { e ->
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
        }
    }

    private fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.clearCurrentUser()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentUser = null,
                    followingIds = emptySet(),
                    followerIds = emptySet()
                )
            }
        }
    }

    private fun retry() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.currentUserId.first()
            if (userId != null) {
                loadCurrentUser(userId)
            }
        }
    }

    private fun upgradePremium() {
        val user = _uiState.value.currentUser ?: return
        if (user.isPremium || _uiState.value.isUpgradingPremium) return

        _uiState.update { it.copy(isUpgradingPremium = true) }
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            runCatching { userRepository.setPremiumStatus(user.id, true) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            currentUser = user.copy(isPremium = true),
                            isUpgradingPremium = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isUpgradingPremium = false, errorMessage = e.message) }
                }
        }
    }
}