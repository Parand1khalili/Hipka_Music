package com.hipka.app.presentation.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            AuthIntent.ToggleAuthMode -> _uiState.update {
                it.copy(isLoginMode = !it.isLoginMode, errorMessage = null, isOfflineError = false)
            }
            is AuthIntent.OnNameChanged -> _uiState.update { it.copy(name = intent.name) }
            is AuthIntent.OnEmailChanged -> _uiState.update { it.copy(email = intent.email) }
            is AuthIntent.OnPasswordChanged -> _uiState.update { it.copy(password = intent.password) }
            AuthIntent.TogglePasswordVisibility -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            AuthIntent.Submit -> submitAuth()
            AuthIntent.ClearError -> _uiState.update { it.copy(errorMessage = null, isOfflineError = false) }
        }
    }

    private fun submitAuth() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }

        if (!currentState.isLoginMode && currentState.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your name") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, isOfflineError = false) }

        viewModelScope.launch {
            val result = if (currentState.isLoginMode) {
                userRepository.login(currentState.email.trim(), currentState.password)
            } else {
                userRepository.register(currentState.name.trim(), currentState.email.trim(), currentState.password)
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    // خطای شبکه (مثل UnknownHostException) نباید به صورت پیام خام
                    // «Unable to resolve host» به کاربر نشان داده شود
                    val isNetworkError = error is IOException

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineError = isNetworkError,
                            errorMessage = if (isNetworkError) null else (error.message ?: "An unexpected error occurred")
                        )
                    }
                }
            )
        }
    }
}