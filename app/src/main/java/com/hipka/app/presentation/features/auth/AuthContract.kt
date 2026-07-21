package com.hipka.app.presentation.features.auth

data class AuthUiState(
    val isLoginMode: Boolean = true,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

sealed interface AuthIntent {
    data object ToggleAuthMode : AuthIntent
    data class OnNameChanged(val name: String) : AuthIntent
    data class OnEmailChanged(val email: String) : AuthIntent
    data class OnPasswordChanged(val password: String) : AuthIntent
    data object TogglePasswordVisibility : AuthIntent
    data object Submit : AuthIntent
    data object ClearError : AuthIntent
}