package com.hipka.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hipka.app.R
import com.hipka.app.core.locale.LocaleManager
import com.hipka.app.data.local.datastore.ThemeMode
import com.hipka.app.presentation.main.MainIntent
import com.hipka.app.presentation.main.MainUiState
import com.hipka.app.presentation.theme.HipkaTheme

@Composable
fun HipkaNavGraph(
    mainUiState: MainUiState,
    onMainIntent: (MainIntent) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = { HipkaBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- مقصدهای منوی پایین صفحه --------------------------------
            composable(Screen.Home.route) {
                PlaceholderScreen("Home")
            }
            composable(Screen.Search.route) {
                PlaceholderScreen("Search")
            }
            composable(Screen.Downloads.route) {
                PlaceholderScreen("Downloads")
            }
            composable(Screen.Playlists.route) {
                PlaceholderScreen("Playlists")
            }
            composable(Screen.Profile.route) {
                ProfilePlaceholderScreen(uiState = mainUiState, onIntent = onMainIntent)
            }

            // --- مسیرهای فرعی و صفحات دیگر ----------------------------------
            composable(Screen.NowPlaying.route) {
                PlaceholderScreen("Now Playing")
            }
            composable(Screen.Settings.route) {
                PlaceholderScreen("Settings")
            }
            composable(Screen.FollowedUsers.route) {
                PlaceholderScreen("Followed Users")
            }
            composable(Screen.LikedSongs.route) {
                PlaceholderScreen("Liked Songs")
            }
            composable(Screen.RecentlyPlayed.route) {
                PlaceholderScreen("Recently Played")
            }
            composable(Screen.ChatList.route) {
                PlaceholderScreen("Chats")
            }
            composable(Screen.ChatConversation.route) {
                PlaceholderScreen("Conversation")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    val translatedTitle = when (title) {
        "Home" -> stringResource(id = R.string.nav_home)
        "Search" -> stringResource(id = R.string.nav_search)
        "Downloads" -> stringResource(id = R.string.nav_downloads)
        "Playlists" -> stringResource(id = R.string.nav_playlists)
        "Settings" -> stringResource(id = R.string.settings_title)
        else -> title
    }

    val isPersian = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "fa"
    val comingSoonText = if (isPersian) "به\u200cزودی" else "coming soon"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HipkaTheme.dimens.spaceM),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$translatedTitle — $comingSoonText",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePlaceholderScreen(
    uiState: MainUiState,
    onIntent: (MainIntent) -> Unit
) {
    val isPersian = uiState.languageCode == LocaleManager.LANGUAGE_PERSIAN
    val profileTitle = stringResource(id = R.string.nav_profile)
    val comingSoonText = if (isPersian) "به\u200cزودی" else "coming soon"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HipkaTheme.dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM)
    ) {
        Text(
            text = "$profileTitle — $comingSoonText",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )

        Text(
            text = stringResource(id = R.string.settings_language),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
            FilterChip(
                selected = uiState.languageCode == LocaleManager.LANGUAGE_ENGLISH,
                onClick = { onIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_ENGLISH)) },
                label = { Text(text = "English") } // 👈 طبق استاندارد: همیشه ثابت به زبان اصلی
            )
            FilterChip(
                selected = uiState.languageCode == LocaleManager.LANGUAGE_PERSIAN,
                onClick = { onIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_PERSIAN)) },
                label = { Text(text = "فارسی") } // 👈 طبق استاندارد: همیشه ثابت به زبان اصلی
            )
        }

        Text(
            text = stringResource(id = R.string.settings_theme),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
            FilterChip(
                selected = uiState.themeMode == ThemeMode.LIGHT,
                onClick = { onIntent(MainIntent.ChangeThemeMode(ThemeMode.LIGHT)) },
                label = { Text(text = stringResource(id = R.string.settings_theme_light)) }
            )
            FilterChip(
                selected = uiState.themeMode == ThemeMode.DARK,
                onClick = { onIntent(MainIntent.ChangeThemeMode(ThemeMode.DARK)) },
                label = { Text(text = stringResource(id = R.string.settings_theme_dark)) }
            )
            FilterChip(
                selected = uiState.themeMode == ThemeMode.SYSTEM,
                onClick = { onIntent(MainIntent.ChangeThemeMode(ThemeMode.SYSTEM)) },
                label = { Text(text = stringResource(id = R.string.settings_theme_system)) }
            )
        }
    }
}