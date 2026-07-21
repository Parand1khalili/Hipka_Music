package com.hipka.app.presentation.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.hipka.app.R
import com.hipka.app.core.locale.LocaleManager
import com.hipka.app.data.local.datastore.ThemeMode
import com.hipka.app.presentation.main.MainIntent
import com.hipka.app.presentation.main.MainUiState
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainUiState: MainUiState,
    onMainIntent: (MainIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(HipkaTheme.dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM)
        ) {
            Text(text = stringResource(id = R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
                FilterChip(
                    selected = mainUiState.languageCode == LocaleManager.LANGUAGE_ENGLISH,
                    onClick = { onMainIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_ENGLISH)) },
                    label = { Text(stringResource(id = R.string.settings_language_english)) }
                )
                FilterChip(
                    selected = mainUiState.languageCode == LocaleManager.LANGUAGE_PERSIAN,
                    onClick = { onMainIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_PERSIAN)) },
                    label = { Text(stringResource(id = R.string.settings_language_persian)) }
                )
            }

            Text(text = stringResource(id = R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
                FilterChip(
                    selected = mainUiState.themeMode == ThemeMode.LIGHT,
                    onClick = { onMainIntent(MainIntent.ChangeThemeMode(ThemeMode.LIGHT)) },
                    label = { Text(stringResource(id = R.string.settings_theme_light)) }
                )
                FilterChip(
                    selected = mainUiState.themeMode == ThemeMode.DARK,
                    onClick = { onMainIntent(MainIntent.ChangeThemeMode(ThemeMode.DARK)) },
                    label = { Text(stringResource(id = R.string.settings_theme_dark)) }
                )
                FilterChip(
                    selected = mainUiState.themeMode == ThemeMode.SYSTEM,
                    onClick = { onMainIntent(MainIntent.ChangeThemeMode(ThemeMode.SYSTEM)) },
                    label = { Text(stringResource(id = R.string.settings_theme_system)) }
                )
            }

            Spacer(Modifier.height(HipkaTheme.dimens.spaceL))

            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onLoggedOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.settings_logout))
            }
        }
    }
}
