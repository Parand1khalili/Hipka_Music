package com.hipka.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hipka.app.R
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.features.ArtistDetail.ArtistDetailScreen
import com.hipka.app.presentation.features.auth.AuthScreen
import com.hipka.app.presentation.features.chat.ChatScreen
import com.hipka.app.presentation.features.downloads.DownloadsScreen
import com.hipka.app.presentation.features.followedusers.FollowedUsersScreen
import com.hipka.app.presentation.features.home.HomeIntent
import com.hipka.app.presentation.features.home.HomeScreen
import com.hipka.app.presentation.features.home.HomeViewModel
import com.hipka.app.presentation.features.likedsongs.LikedSongsScreen
import com.hipka.app.presentation.features.player.MiniPlayerBar
import com.hipka.app.presentation.features.player.PlayerIntent
import com.hipka.app.presentation.features.player.PlayerScreen
import com.hipka.app.presentation.features.player.PlayerViewModel
import com.hipka.app.presentation.features.playlists.PlaylistsScreen
import com.hipka.app.presentation.features.profile.ProfileScreen
import com.hipka.app.presentation.features.recent.RecentSongsScreen
import com.hipka.app.presentation.features.search.SearchScreen
import com.hipka.app.presentation.features.see_all.SeeAllScreen
import com.hipka.app.presentation.features.settings.SettingsScreen
import com.hipka.app.presentation.features.topartists.TopArtistsScreen
import com.hipka.app.presentation.main.MainIntent
import com.hipka.app.presentation.main.MainUiState
import com.hipka.app.presentation.main.SongInteractionViewModel
import com.hipka.app.presentation.theme.HipkaTheme
import java.net.URLDecoder

@Composable
fun HipkaNavGraph(
    mainUiState: MainUiState,
    onMainIntent: (MainIntent) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val songInteractionViewModel: SongInteractionViewModel = hiltViewModel()
    val likedSongIds by songInteractionViewModel.likedSongIds.collectAsStateWithLifecycle()
    val downloadedSongIds by songInteractionViewModel.downloadedSongIds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(playerViewModel) {
        playerViewModel.playbackErrors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    val downloadStartedMessage = stringResource(id = R.string.download_started)
    val premiumRequiredMessage = stringResource(id = R.string.download_premium_required)
    LaunchedEffect(songInteractionViewModel) {
        songInteractionViewModel.downloadStarted.collect {
            snackbarHostState.showSnackbar(downloadStartedMessage)
        }
    }
    LaunchedEffect(songInteractionViewModel) {
        songInteractionViewModel.premiumRequired.collect {
            snackbarHostState.showSnackbar(premiumRequiredMessage)
        }
    }

    // پخش صف کامل لیست فعلی از نقطه کلیک‌شده — نه فقط یک آهنگ تنها — تا دکمه‌های
    // بعدی/قبلی در Now Playing در همه‌ی لیست‌های آهنگ (نه فقط هوم) کار کنند
    val playQueueAndTrack: (List<Song>, Song) -> Unit = { songs, song ->
        songInteractionViewModel.addToRecentlyPlayed(song)
        val startIndex = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playerViewModel.onIntent(PlayerIntent.PlayQueue(songs, startIndex))
    }

    LaunchedEffect(mainUiState.isLoggedIn, mainUiState.isSessionChecked) {
        if (mainUiState.isSessionChecked && !mainUiState.isLoggedIn) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // مخفی کردن منوی پایین در صفحه ورود / ثبت نام
            if (currentRoute != Screen.Auth.route) {
                Column {
                    // مینی‌پلیر داخل خود صفحه Now Playing نمایش داده نمی‌شود چون پلیر کامل باز است
                    if (currentRoute != Screen.NowPlaying.route) {
                        playerUiState.currentSong?.let { song ->
                            MiniPlayerBar(
                                song = song,
                                isPlaying = playerUiState.isPlaying,
                                onTogglePlayPause = { playerViewModel.onIntent(PlayerIntent.TogglePlayPause) },
                                isBuffering = playerUiState.isBuffering,
                                isShuffleEnabled = playerUiState.isShuffleEnabled,
                                repeatMode = playerUiState.repeatMode,
                                onToggleShuffle = { playerViewModel.onIntent(PlayerIntent.ToggleShuffle) },
                                onCycleRepeatMode = { playerViewModel.onIntent(PlayerIntent.CycleRepeatMode) },
                                onSkipNext = { playerViewModel.onIntent(PlayerIntent.SkipNext) },
                                onSkipPrevious = { playerViewModel.onIntent(PlayerIntent.SkipPrevious) },
                                onClose = { playerViewModel.onIntent(PlayerIntent.Stop) },
                                onClick = { navController.navigate(Screen.NowPlaying.route) },
                                modifier = Modifier.padding(
                                    horizontal = HipkaTheme.dimens.spaceS,
                                    vertical = HipkaTheme.dimens.spaceS
                                )
                            )
                        }
                    }
                    HipkaBottomBar(navController)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (mainUiState.isLoggedIn) Screen.Home.route else Screen.Auth.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            // --- صفحه ورود / ثبت نام ---
            composable(Screen.Auth.route) {
                AuthScreen(
                    onMainIntent = onMainIntent,
                    onAuthSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            // --- Bottom nav destinations --------------------------------
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

                HomeScreen(
                    uiState = homeUiState,
                    likedSongIds = likedSongIds,
                    onSongClick = { song ->
                        songInteractionViewModel.addToRecentlyPlayed(song)
                        val queue = listOf(homeUiState.popularSongs, homeUiState.newReleases, homeUiState.carouselSongs)
                            .firstOrNull { list -> list.any { it.id == song.id } }
                            ?: listOf(song)
                        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                        playerViewModel.onIntent(PlayerIntent.PlayQueue(queue, startIndex))
                    },
                    onLikeClick = { song -> songInteractionViewModel.toggleLike(song) },
                    onRefresh = { homeViewModel.onIntent(HomeIntent.RefreshHome) },
                    onQuickActionClick = { action ->
                        when (action) {
                            "liked" -> navController.navigate(Screen.LikedSongs.route)
                            "recent" -> navController.navigate(Screen.RecentlyPlayed.route)
                            "playlists" -> navController.navigate(Screen.Playlists.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            "artists" -> navController.navigate(Screen.TopArtists.route)
                        }
                    },
                    onSeeAllClick = { section -> navController.navigate("see_all/$section") },
                    onPlaylistClick = { _ ->
                        navController.navigate(Screen.Playlists.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onGoToDownloads = {
                        navController.navigate(Screen.Downloads.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // --- صفحه هنرمندان برتر همراه با هدایت به پروفایل ---
            composable(Screen.TopArtists.route) {
                TopArtistsScreen(
                    onBackClick = { navController.popBackStack() },
                    onArtistClick = { artist ->
                        navController.navigate(Screen.ArtistDetail.createRoute(artist.name, artist.imageUrl))
                    }
                )
            }

            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(
                    navArgument("artistName") { type = NavType.StringType },
                    navArgument("imageUrl") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
                val rawImageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                val imageUrl = try { URLDecoder.decode(rawImageUrl, "UTF-8") } catch (e: Exception) { rawImageUrl }

                ArtistDetailScreen(
                    artistName = artistName,
                    imageUrl = imageUrl,
                    likedSongIds = likedSongIds,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = playQueueAndTrack,
                    onLikeClick = { song -> songInteractionViewModel.toggleLike(song) },
                    onShuffleClick = { songList ->
                        playerViewModel.onIntent(PlayerIntent.ShufflePlayList(songList))
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
                    likedSongIds = likedSongIds,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = playQueueAndTrack,
                    onLikeClick = { song -> songInteractionViewModel.toggleLike(song) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    likedSongIds = likedSongIds,
                    onSongClick = playQueueAndTrack,
                    onLikeClick = { song -> songInteractionViewModel.toggleLike(song) },
                    onNavigateToDiscoverUsers = { navController.navigate("followed_users/discover") }
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onSongClick = playQueueAndTrack
                )
            }

            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    onPlaylistClick = { _ -> }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToFollowedUsers = { type -> navController.navigate("followed_users/$type") },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // --- Secondary destinations ----------------------------------
            composable(Screen.NowPlaying.route) {
                val currentSongId = playerUiState.currentSong?.id
                PlayerScreen(
                    uiState = playerUiState,
                    onIntent = { playerViewModel.onIntent(it) },
                    onBackClick = { navController.popBackStack() },
                    isDownloaded = currentSongId != null && downloadedSongIds.contains(currentSongId),
                    onDownloadClick = {
                        playerUiState.currentSong?.let { songInteractionViewModel.downloadSong(it) }
                    },
                    isLiked = currentSongId != null && likedSongIds.contains(currentSongId),
                    onLikeClick = {
                        playerUiState.currentSong?.let { songInteractionViewModel.toggleLike(it) }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    mainUiState = mainUiState,
                    onMainIntent = onMainIntent,
                    onNavigateBack = { navController.popBackStack() },
                    onLoggedOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "followed_users/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "following"
                FollowedUsersScreen(
                    viewType = type,
                    onOpenChat = { peerId ->
                        navController.navigate(Screen.ChatConversation.createRoute(peerId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.LikedSongs.route) {
                LikedSongsScreen(
                    onSongClick = playQueueAndTrack,
                    onShuffleAllClick = { songList ->
                        playerViewModel.onIntent(PlayerIntent.ShufflePlayList(songList))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.RecentlyPlayed.route) {
                RecentSongsScreen(
                    onSongClick = playQueueAndTrack,
                    onShuffleAllClick = { songList ->
                        playerViewModel.onIntent(PlayerIntent.ShufflePlayList(songList))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ChatList.route) { PlaceholderScreen("Chats") }

            composable(
                route = Screen.ChatConversation.route,
                arguments = listOf(navArgument(Screen.ChatConversation.ARG_PEER_USER_ID) { type = NavType.StringType })
            ) {
                ChatScreen(
                    onBackClick = { navController.popBackStack() },
                    onPlaySong = { song ->
                        songInteractionViewModel.addToRecentlyPlayed(song)
                        playerViewModel.onIntent(PlayerIntent.PlaySong(song))
                    }
                )
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
    val comingSoonText = if (isPersian) "به‌زودی" else "coming soon"

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