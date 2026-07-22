package com.hipka.app.presentation.features.auth

data class AuthUiState(
    val isLoginMode: Boolean = true,
    val loginIdentifier: String = "", // ایمیل یا نام کاربری برای ورود
    val name: String = "",            // نام و نام خانوادگی
    val username: String = "",        // نام کاربری اختصاصی
    val email: String = "",           // آدرس ایمیل
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOfflineError: Boolean = false,
    val isSuccess: Boolean = false
)

sealed interface AuthIntent {
    data object ToggleAuthMode : AuthIntent
    data class OnLoginIdentifierChanged(val identifier: String) : AuthIntent
    data class OnNameChanged(val name: String) : AuthIntent
    data class OnUsernameChanged(val username: String) : AuthIntent
    data class OnEmailChanged(val email: String) : AuthIntent
    data class OnPasswordChanged(val password: String) : AuthIntent
    data object TogglePasswordVisibility : AuthIntent
    data class ChangeLanguage(val languageCode: String) : AuthIntent
    data object Submit : AuthIntent
    data object ClearError : AuthIntent
}