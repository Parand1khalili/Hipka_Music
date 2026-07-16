package com.hipka.app.presentation.features.followedusers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.repository.FollowRepository
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
class FollowedUsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowedUsersUiState())
    val uiState: StateFlow<FollowedUsersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: FollowedUsersIntent) {
        when (intent) {
            is FollowedUsersIntent.ToggleFollow -> toggleFollow(intent.targetUserId)
            FollowedUsersIntent.Refresh -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val myId = sessionManager.currentUserId.first()
            if (myId == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Pick a demo user on the Profile tab first.")
                }
                return@launch
            }
            runCatching {
                val allUsers = userRepository.getAllUsers().filterNot { it.id == myId }
                val followingIds = followRepository.getFollowingIds(myId)
                allUsers to followingIds
            }.onSuccess { (users, followingIds) ->
                _uiState.update { it.copy(users = users, followingIds = followingIds, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            val myId = sessionManager.currentUserId.first() ?: return@launch
            val wasFollowing = targetUserId in _uiState.value.followingIds

            // Optimistic update so the UI feels instant.
            _uiState.update { state ->
                val updated = state.followingIds.toMutableSet()
                if (wasFollowing) updated.remove(targetUserId) else updated.add(targetUserId)
                state.copy(followingIds = updated)
            }

            runCatching {
                if (wasFollowing) followRepository.unfollow(myId, targetUserId)
                else followRepository.follow(myId, targetUserId)
            }.onFailure { e ->
                // Revert on failure.
                _uiState.update { state ->
                    val reverted = state.followingIds.toMutableSet()
                    if (wasFollowing) reverted.add(targetUserId) else reverted.remove(targetUserId)
                    state.copy(followingIds = reverted, errorMessage = e.message)
                }
            }
        }
    }
}