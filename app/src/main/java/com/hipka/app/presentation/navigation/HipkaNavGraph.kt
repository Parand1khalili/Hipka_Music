package com.hipka.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hipka.app.R
import com.hipka.app.core.locale.LocaleManager
import com.hipka.app.data.local.datastore.ThemeMode
import com.hipka.app.presentation.features.home.HomeScreen
import com.hipka.app.presentation.features.home.HomeViewModel
import com.hipka.app.presentation.features.player.MiniPlayerBar
import com.hipka.app.presentation.features.player.PlayerIntent
import com.hipka.app.presentation.features.player.PlayerViewModel
import com.hipka.app.presentation.main.MainIntent
import com.hipka.app.presentation.main.MainUiState
import com.hipka.app.presentation.theme.HipkaTheme

/**
 * App-wide navigation graph. Screens are stubbed with [PlaceholderScreen] so
 * the graph is runnable end-to-end from Sprint 1 — each teammate replaces
 * their own `composable(...)` body with the real screen as it lands, without
 * ever needing to touch this file's structure or the [Screen] routes.
 */
@Composable
fun HipkaNavGraph(
    mainUiState: MainUiState,
    onMainIntent: (MainIntent) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(playerViewModel) {
        playerViewModel.playbackErrors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                playerUiState.currentSong?.let { song ->
                    MiniPlayerBar(
                        song = song,
                        isPlaying = playerUiState.isPlaying,
                        onTogglePlayPause = { playerViewModel.onIntent(PlayerIntent.TogglePlayPause) },
                        modifier = Modifier.padding(horizontal = HipkaTheme.dimens.spaceS)
                    )
                }
                HipkaBottomBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- Bottom nav destinations --------------------------------
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = homeUiState,
                    onSongClick = { song -> playerViewModel.onIntent(PlayerIntent.PlaySong(song)) }
                )
            }
            composable(Screen.Search.route) {
                PlaceholderScreen("Search") // Owner: Person 1 (A4)
            }
            composable(Screen.Downloads.route) {
                PlaceholderScreen("Downloads") // Owner: Person 2 (B6)
            }
            composable(Screen.Playlists.route) {
                PlaceholderScreen("Playlists") // Owner: Person 3 (C5)
            }
            composable(Screen.Profile.route) {
                // Demo screen wired to MainViewModel so Design System +
                // Navigation + Theme + Localization are verifiable right now,
                // ahead of the real Profile screen (C5).
                ProfilePlaceholderScreen(uiState = mainUiState, onIntent = onMainIntent)
            }

            // --- Secondary destinations ----------------------------------
            composable(Screen.NowPlaying.route) {
                PlaceholderScreen("Now Playing") // Owner: Person 2 (B3/B4)
            }
            composable(Screen.Settings.route) {
                PlaceholderScreen("Settings") // Owner: Person 3 (C5)
            }
            composable(Screen.FollowedUsers.route) {
                PlaceholderScreen("Followed Users") // Owner: Person 3 (C5)
            }
            composable(Screen.LikedSongs.route) {
                PlaceholderScreen("Liked Songs") // Owner: Person 1 (A5)
            }
            composable(Screen.RecentlyPlayed.route) {
                PlaceholderScreen("Recently Played") // Owner: Person 1 (A5)
            }
            composable(Screen.ChatList.route) {
                PlaceholderScreen("Chats") // Owner: Person 3 (C3)
            }
            composable(Screen.ChatConversation.route) {
                PlaceholderScreen("Conversation") // Owner: Person 3 (C3)
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

/**
 * Temporary stand-in for the real Profile screen (C5). Lets anyone on the
 * team confirm, before any other screen exists, that:
 *  - switching language flips RTL/LTR live,
 *  - switching theme mode recolors the whole app,
 *  - both choices survive a process restart (via DataStore).
 * Delete this composable once the real Profile screen (C5) lands.
 */
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
                label = { Text(text = "English") } // اصولی: همیشه ثابت به زبان اصلی
            )
            FilterChip(
                selected = uiState.languageCode == LocaleManager.LANGUAGE_PERSIAN,
                onClick = { onIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_PERSIAN)) },
                label = { Text(text = "فارسی") } // اصولی: همیشه ثابت به زبان اصلی
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