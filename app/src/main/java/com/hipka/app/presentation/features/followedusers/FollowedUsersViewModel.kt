package com.hipka.app.presentation.features.followedusers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowedUsersViewModel @Inject constructor(
    private val userRepository: UserRepository, // 💡 وابستگی قدیمی followRepository کاملاً حذف شد
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
                coroutineScope {
                    val usersDeferred = async { userRepository.getAllUsers().filterNot { it.id == myId } }
                    val followingDeferred = async { userRepository.getFollowingIds(myId) }
                    val followersDeferred = async { userRepository.getFollowerIds(myId) }

                    Triple(usersDeferred.await(), followingDeferred.await(), followersDeferred.await())
                }
            }.onSuccess { (users, followingIds, followerIds) ->
                _uiState.update {
                    it.copy(
                        users = users,
                        followingIds = followingIds.toSet(),
                        followerIds = followerIds.toSet(),
                        isLoading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            val myId = sessionManager.currentUserId.first() ?: return@launch
            val wasFollowing = targetUserId in _uiState.value.followingIds

            // Optimistic update
            _uiState.update { state ->
                val updated = state.followingIds.toMutableSet()
                if (wasFollowing) updated.remove(targetUserId) else updated.add(targetUserId)
                state.copy(followingIds = updated)
            }

            // ✨ اتصال عملیات فالو و آنفالو به آدرس و جدول واقعی سوپابیس
            runCatching {
                if (wasFollowing) {
                    userRepository.unfollowUser(myId, targetUserId)
                } else {
                    userRepository.followUser(myId, targetUserId)
                }
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