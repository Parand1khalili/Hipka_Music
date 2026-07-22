package com.hipka.app.presentation.features.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.core.locale.LocaleManager
import com.hipka.app.data.local.datastore.SettingsDataStore
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
    private val userRepository: UserRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            AuthIntent.ToggleAuthMode -> _uiState.update {
                it.copy(isLoginMode = !it.isLoginMode, errorMessage = null, isOfflineError = false)
            }
            is AuthIntent.OnLoginIdentifierChanged -> _uiState.update { it.copy(loginIdentifier = intent.identifier) }
            is AuthIntent.OnNameChanged -> _uiState.update { it.copy(name = intent.name) }
            is AuthIntent.OnUsernameChanged -> _uiState.update { it.copy(username = intent.username) }
            is AuthIntent.OnEmailChanged -> _uiState.update { it.copy(email = intent.email) }
            is AuthIntent.OnPasswordChanged -> _uiState.update { it.copy(password = intent.password) }
            is AuthIntent.OnGenderChanged -> _uiState.update { it.copy(gender = intent.gender) }
            AuthIntent.TogglePasswordVisibility -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            is AuthIntent.ChangeLanguage -> changeLanguage(intent.languageCode)
            AuthIntent.Submit -> submitAuth()
            AuthIntent.ClearError -> _uiState.update { it.copy(errorMessage = null, isOfflineError = false) }
        }
    }

    private fun changeLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(languageCode)
            LocaleManager.setLocale(languageCode)
        }
    }

    private fun submitAuth() {
        val state = _uiState.value
        val isPersian = LocaleManager.currentLanguageTag() == LocaleManager.LANGUAGE_PERSIAN

        if (state.isLoginMode) {
            if (state.loginIdentifier.isBlank()) {
                val error = if (isPersian) "لطفاً ایمیل یا نام کاربری خود را وارد کنید" else "Please enter your email or username"
                _uiState.update { it.copy(errorMessage = error, isOfflineError = false) }
                return
            }
            if (state.password.isBlank()) {
                val error = if (isPersian) "لطفاً رمز عبور را وارد کنید" else "Please enter your password"
                _uiState.update { it.copy(errorMessage = error, isOfflineError = false) }
                return
            }
        } else {

            // اعتبارسنجی ثبت‌نام
            if (state.name.isBlank()) {
                val error = if (isPersian) "لطفاً نام و نام خانوادگی خود را وارد کنید" else "Please enter your full name"
                _uiState.update { it.copy(errorMessage = error, isOfflineError = false) }
                return
            }
            if (state.username.isBlank()) {
                val error = if (isPersian) "لطفاً نام کاربری را وارد کنید" else "Please enter a username"
                _uiState.update { it.copy(errorMessage = error, isOfflineError = false) }
                return
            }
            if (state.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
                val error = if (isPersian) "فرمت ایمیل وارد شده نادرست است" else "Please enter a valid email address"
                _uiState.update { it.copy(errorMessage = error, isOfflineError = false) }
                return
            }
            if (state.password.length < 6) {
                val error = if (isPersian) "رمز عبور باید حداقل ۶ کاراکتر باشد" else "Password must be at least 6 characters"
                _uiState.update { it.copy(errorMessage = error, isOfflineError = false) }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, isOfflineError = false) }

        viewModelScope.launch {
            val result = if (state.isLoginMode) {
                userRepository.login(state.loginIdentifier.trim(), state.password)
            } else {
                userRepository.register(name = state.name.trim(), username = state.username.trim(), email = state.email.trim(),password = state.password, gender = state.gender)
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    val isNetworkError = error is IOException
                    val userFriendlyError = when (error.message) {
                        "AUTH_INVALID_CREDENTIALS" -> if (isPersian) "نام کاربری/ایمیل یا رمز عبور اشتباه است" else "Invalid username/email or password"
                        else -> if (isPersian) "خطا در برقراری ارتباط. لطفاً دوباره تلاش کنید" else "Authentication failed. Please try again."
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineError = isNetworkError,
                            errorMessage = if (isNetworkError) null else userFriendlyError
                        )
                    }
                }
            )
        }
    }
}