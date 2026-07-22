package com.hipka.app.presentation.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hipka.app.R
import com.hipka.app.core.locale.LocaleManager
import com.hipka.app.presentation.main.MainIntent
import com.hipka.app.presentation.theme.HipkaTheme

@Composable
fun AuthScreen(
    onMainIntent: (MainIntent) -> Unit,
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLang = LocalConfiguration.current.locales[0].language
    val isPersian = currentLang == "fa"
    val logoResId = if (isPersian) R.drawable.ic_logo_fa else R.drawable.ic_logo_en

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onAuthSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ۱. نوار انتخاب زبان (بدون همپوشانی و کاملاً کلیک‌پذیر)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HipkaTheme.dimens.spaceM, vertical = HipkaTheme.dimens.spaceS),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !isPersian,
                onClick = { onMainIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_ENGLISH)) },
                label = { Text("English") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = isPersian,
                onClick = { onMainIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_PERSIAN)) },
                label = { Text("فارسی") },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // ۲. فرم اصلی با قابلیت اسکرول
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HipkaTheme.dimens.spaceL)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // لوگوی برنامه
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier
                    .height(64.dp)
                    .width(180.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceL))

            Text(
                text = if (uiState.isLoginMode) {
                    stringResource(id = R.string.auth_welcome_back)
                } else {
                    stringResource(id = R.string.auth_create_account)
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceL))

            if (uiState.isLoginMode) {
                // فیلد ورود: ایمیل یا نام کاربری *
                OutlinedTextField(
                    value = uiState.loginIdentifier,
                    onValueChange = { viewModel.onIntent(AuthIntent.OnLoginIdentifierChanged(it)) },
                    label = { RequiredLabel(text = stringResource(id = R.string.auth_username_or_email)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
                )
            } else {
                // فیلد ثبت‌نام ۱: نام و نام خانوادگی *
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onIntent(AuthIntent.OnNameChanged(it)) },
                    label = { RequiredLabel(text = stringResource(id = R.string.auth_full_name)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
                )

                Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))

                // فیلد ثبت‌نام ۲: نام کاربری *
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.onIntent(AuthIntent.OnUsernameChanged(it)) },
                    label = { RequiredLabel(text = stringResource(id = R.string.auth_username)) },
                    leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
                )

                Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))

                // فیلد ثبت‌نام ۳: ایمیل *
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onIntent(AuthIntent.OnEmailChanged(it)) },
                    label = { RequiredLabel(text = stringResource(id = R.string.auth_email)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
                )
            }

            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))

            // فیلد رمز عبور *
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onIntent(AuthIntent.OnPasswordChanged(it)) },
                label = { RequiredLabel(text = stringResource(id = R.string.auth_password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.onIntent(AuthIntent.TogglePasswordVisibility) }) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
            )

            // نمایش پیام‌های خطا
            val displayedError = when {
                uiState.isOfflineError -> stringResource(id = R.string.error_no_internet)
                else -> uiState.errorMessage
            }
            displayedError?.let { error ->
                Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(HipkaTheme.dimens.cornerS),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(HipkaTheme.dimens.spaceS)
                    )
                }
            }

            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceL))

            // دکمه اصلی عملیات (ورود / ثبت‌نام)
            Button(
                onClick = { viewModel.onIntent(AuthIntent.Submit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(HipkaTheme.dimens.cornerM),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (uiState.isLoginMode) {
                            stringResource(id = R.string.auth_login_button)
                        } else {
                            stringResource(id = R.string.auth_register_button)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))

            // دکمه تغییر حالت بین ورود و ثبت‌نام
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (uiState.isLoginMode) {
                        stringResource(id = R.string.auth_no_account)
                    } else {
                        stringResource(id = R.string.auth_already_have_account)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (uiState.isLoginMode) {
                        stringResource(id = R.string.auth_sign_up_link)
                    } else {
                        stringResource(id = R.string.auth_sign_in_link)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { viewModel.onIntent(AuthIntent.ToggleAuthMode) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RequiredLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text)
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = "*", color = MaterialTheme.colorScheme.error)
    }
}