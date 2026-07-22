package com.hipka.app.presentation.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.hipka.app.presentation.theme.HipkaTheme

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPersian = LocalConfiguration.current.locales[0].language == "fa"
    val logoResId = if (isPersian) R.drawable.ic_logo_fa else R.drawable.ic_logo_en

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HipkaTheme.dimens.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
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

            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))

            // Full Name Field (Registration Mode Only)
            if (!uiState.isLoginMode) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onIntent(AuthIntent.OnNameChanged(it)) },
                    label = { Text(text = stringResource(id = R.string.auth_full_name)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
                )
                Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
            }

            // Email Field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onIntent(AuthIntent.OnEmailChanged(it)) },
                label = { Text(text = stringResource(id = R.string.auth_email)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
            )

            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))

            // Password Field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onIntent(AuthIntent.OnPasswordChanged(it)) },
                label = { Text(text = stringResource(id = R.string.auth_password)) },
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

            // Error Message Display
            val displayedError = when {
                uiState.isOfflineError -> stringResource(id = R.string.error_no_internet)
                else -> uiState.errorMessage
            }
            displayedError?.let { error ->
                Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceL))

            // Main Action Button (Sign In / Sign Up)
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

            // Toggle Between Sign In and Sign Up Modes
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
        }
    }
}