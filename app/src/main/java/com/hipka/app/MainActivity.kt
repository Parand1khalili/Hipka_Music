package com.hipka.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hipka.app.data.local.datastore.SettingsDataStore
import com.hipka.app.data.local.datastore.ThemeMode
import com.hipka.app.presentation.main.MainViewModel
import com.hipka.app.presentation.main.MainViewModelFactory
import com.hipka.app.presentation.navigation.HipkaNavGraph
import com.hipka.app.presentation.theme.HipkaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val settingsDataStore by lazy { SettingsDataStore(applicationContext) }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(settingsDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = when (uiState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            HipkaTheme(darkTheme = darkTheme) {
                HipkaNavGraph(
                    mainUiState = uiState,
                    onMainIntent = viewModel::onIntent
                )
            }
        }
    }
}