package com.hipka.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hipka.app.R
import com.hipka.app.presentation.features.home.HomeIntent
import com.hipka.app.presentation.features.home.HomeScreen
import com.hipka.app.presentation.features.home.HomeViewModel
import com.hipka.app.presentation.features.home.SeeAllScreen
import com.hipka.app.presentation.features.player.MiniPlayerBar
import com.hipka.app.presentation.features.player.PlayerIntent
import com.hipka.app.presentation.features.player.PlayerViewModel
import com.hipka.app.presentation.features.search.SearchScreen
import com.hipka.app.presentation.features.playlists.PlaylistsScreen
import com.hipka.app.presentation.features.profile.ProfileScreen
import com.hipka.app.presentation.features.followedusers.FollowedUsersScreen
import com.hipka.app.presentation.features.chat.ChatScreen
import com.hipka.app.presentation.features.likedsongs.LikedSongsScreen
import com.hipka.app.presentation.features.recent.RecentSongsScreen
import com.hipka.app.presentation.main.SongInteractionViewModel
import com.hipka.app.presentation.main.MainIntent
import com.hipka.app.presentation.main.MainUiState
import com.hipka.app.presentation.theme.HipkaTheme
import androidx.compose.material3.MaterialTheme

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
                    likedSongIds = likedSongIds,
                    onSongClick = { song ->
                        songInteractionViewModel.addToRecentlyPlayed(song)
                        playerViewModel.onIntent(PlayerIntent.PlaySong(song))
                    },
                    onLikeClick = { song ->
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
                arguments = listOf(navArgument("section") { type = NavType.StringType })
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
            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    onPlaylistClick = { playlistId ->
                        // نویگیشن به صفحه جزئیات پلی‌لیست در اسپرینت ۳ در صورت نیاز
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    mainUiState = mainUiState,
                    onMainIntent = onMainIntent,
                    onNavigateToFollowedUsers = { navController.navigate(Screen.FollowedUsers.route) }
                )
            }

            // --- Secondary destinations ----------------------------------
            composable(Screen.NowPlaying.route) { PlaceholderScreen("Now Playing") }
            composable(Screen.Settings.route) { PlaceholderScreen("Settings") }

            composable(Screen.FollowedUsers.route) {
                FollowedUsersScreen(
                    onOpenChat = { peerId ->
                        navController.navigate(Screen.ChatConversation.createRoute(peerId))
                    }
                )
            }

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

            composable(
                route = Screen.ChatConversation.route,
                arguments = listOf(navArgument(Screen.ChatConversation.ARG_PEER_USER_ID) { type = NavType.StringType })
            ) {
                ChatScreen()
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

    val isPersian = LocalConfiguration.current.locales[0].language == "fa"
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
            style = MaterialTheme.typography.titleMedium
        )
    }
}