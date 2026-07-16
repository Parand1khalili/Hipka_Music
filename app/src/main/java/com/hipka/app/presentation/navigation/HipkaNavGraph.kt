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
import com.hipka.app.presentation.features.home.HomeIntent
import com.hipka.app.presentation.features.home.HomeScreen
import com.hipka.app.presentation.features.home.HomeViewModel
import com.hipka.app.presentation.features.home.SeeAllScreen
import com.hipka.app.presentation.features.player.MiniPlayerBar
import com.hipka.app.presentation.features.player.PlayerIntent
import com.hipka.app.presentation.features.player.PlayerViewModel
import com.hipka.app.presentation.main.MainIntent
import com.hipka.app.presentation.main.MainUiState
import com.hipka.app.presentation.theme.HipkaTheme
import com.hipka.app.presentation.features.search.SearchScreen
import com.hipka.app.presentation.features.likedsongs.LikedSongsScreen
import com.hipka.app.presentation.features.recent.RecentSongsScreen
import com.hipka.app.presentation.main.SongInteractionViewModel

@Composable
fun HipkaNavGraph(
    mainUiState: MainUiState,
    onMainIntent: (MainIntent) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()

    val songInteractionViewModel: SongInteractionViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(playerViewModel) {
        playerViewModel.playbackErrors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    val likedSongIds by songInteractionViewModel.likedSongIds.collectAsStateWithLifecycle()
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
                val context = androidx.compose.ui.platform.LocalContext.current

                HomeScreen(
                    uiState = homeUiState,
                    likedSongIds = likedSongIds, // ارسال لیست به صفحه
                    onSongClick = { song ->
                        songInteractionViewModel.addToRecentlyPlayed(song)
                        playerViewModel.onIntent(PlayerIntent.PlaySong(song))
                    },
                    onLikeClick = { song -> // حالا به جای آیدی، خود آهنگ را می‌فرستیم
                        songInteractionViewModel.toggleLike(song)
                    },
                    onRefresh = { homeViewModel.onIntent(HomeIntent.RefreshHome) },
                    onQuickActionClick = { action ->
                        when (action) {
                            "liked" -> navController.navigate(Screen.LikedSongs.route)
                            "recent" -> navController.navigate(Screen.RecentlyPlayed.route)
                            "playlists" -> navController.navigate(Screen.Playlists.route)
                            "artists" -> android.widget.Toast.makeText(context, "Opening Top Artists...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSeeAllClick = { section ->
                        navController.navigate("see_all/$section")
                    }
                )
            }

            composable(
                route = "see_all/{section}",
                arguments = listOf(androidx.navigation.navArgument("section") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val section = backStackEntry.arguments?.getString("section") ?: ""
                SeeAllScreen(
                    sectionName = section,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    likedSongIds = likedSongIds,
                    onSongClick = { song ->
                        songInteractionViewModel.addToRecentlyPlayed(song)
                        playerViewModel.onIntent(PlayerIntent.PlaySong(song))
                    },
                    onLikeClick = { song ->
                        songInteractionViewModel.toggleLike(song)
                    }
                )
            }

            composable(Screen.Downloads.route) { PlaceholderScreen("Downloads") }
            composable(Screen.Playlists.route) { PlaceholderScreen("Playlists") }
            composable(Screen.Profile.route) { ProfilePlaceholderScreen(uiState = mainUiState, onIntent = onMainIntent) }

            // --- Secondary destinations ----------------------------------
            composable(Screen.NowPlaying.route) { PlaceholderScreen("Now Playing") }
            composable(Screen.Settings.route) { PlaceholderScreen("Settings") }
            composable(Screen.FollowedUsers.route) { PlaceholderScreen("Followed Users") }

            // صفحات اسپرینت ۳
            composable(Screen.LikedSongs.route) {
                LikedSongsScreen(
                    onSongClick = { song ->
                        songInteractionViewModel.addToRecentlyPlayed(song)
                        playerViewModel.onIntent(PlayerIntent.PlaySong(song))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.RecentlyPlayed.route) {
                RecentSongsScreen(
                    onSongClick = { song ->
                        songInteractionViewModel.addToRecentlyPlayed(song)
                        playerViewModel.onIntent(PlayerIntent.PlaySong(song))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ChatList.route) { PlaceholderScreen("Chats") }
            composable(Screen.ChatConversation.route) { PlaceholderScreen("Conversation") }
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
        Text(text = "$profileTitle — $comingSoonText", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(id = R.string.settings_language), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
            FilterChip(selected = uiState.languageCode == LocaleManager.LANGUAGE_ENGLISH, onClick = { onIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_ENGLISH)) }, label = { Text(text = "English") })
            FilterChip(selected = uiState.languageCode == LocaleManager.LANGUAGE_PERSIAN, onClick = { onIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_PERSIAN)) }, label = { Text(text = "فارسی") })
        }
        Text(text = stringResource(id = R.string.settings_theme), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
            FilterChip(selected = uiState.themeMode == ThemeMode.LIGHT, onClick = { onIntent(MainIntent.ChangeThemeMode(ThemeMode.LIGHT)) }, label = { Text(text = stringResource(id = R.string.settings_theme_light)) })
            FilterChip(selected = uiState.themeMode == ThemeMode.DARK, onClick = { onIntent(MainIntent.ChangeThemeMode(ThemeMode.DARK)) }, label = { Text(text = stringResource(id = R.string.settings_theme_dark)) })
            FilterChip(selected = uiState.themeMode == ThemeMode.SYSTEM, onClick = { onIntent(MainIntent.ChangeThemeMode(ThemeMode.SYSTEM)) }, label = { Text(text = stringResource(id = R.string.settings_theme_system)) })
        }
    }
}