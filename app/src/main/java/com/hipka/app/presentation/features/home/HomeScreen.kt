package com.hipka.app.presentation.features.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.domain.model.Playlist
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.theme.HipkaTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    likedSongIds: Set<String>,
    onSongClick: (Song) -> Unit,
    onRefresh: () -> Unit,
    onQuickActionClick: (String) -> Unit,
    onSeeAllClick: (String) -> Unit,
    onLikeClick: (Song) -> Unit,
    onPlaylistClick: (String) -> Unit, // ✨ اضافه شدن این کالبک برای زنده کردن دکمه‌های کلیک روی ردیف پلی‌لیست‌ها
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onRefresh() }

    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        uiState.errorMessage != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
        uiState.carouselSongs.isEmpty() && uiState.popularSongs.isEmpty() && uiState.newReleases.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = R.string.empty_state_no_results),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = HipkaTheme.dimens.spaceXL)
            ) {
                item { HomeTopBar() }

                if (uiState.carouselSongs.isNotEmpty()) {
                    item {
                        val pagerState = rememberPagerState(pageCount = { uiState.carouselSongs.size })
                        val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

                        if (uiState.carouselSongs.size > 1) {
                            LaunchedEffect(isDragged) {
                                if (!isDragged) {
                                    while (true) {
                                        delay(3500L)
                                        val nextPage = (pagerState.currentPage + 1) % uiState.carouselSongs.size
                                        pagerState.animateScrollToPage(
                                            page = nextPage,
                                            animationSpec = tween(
                                                durationMillis = 800,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = HipkaTheme.dimens.spaceL),
                                pageSpacing = HipkaTheme.dimens.spaceS
                            ) { page ->
                                FeaturedBanner(
                                    song = uiState.carouselSongs[page],
                                    onClick = { onSongClick(uiState.carouselSongs[page]) },
                                    modifier = Modifier.padding(vertical = HipkaTheme.dimens.spaceS)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = HipkaTheme.dimens.spaceS),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(uiState.carouselSongs.size) { index ->
                                    val color = if (pagerState.currentPage == index) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    QuickActionsRow(onActionClick = onQuickActionClick)
                }

                if (uiState.popularSongs.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(id = R.string.home_section_popular),
                            onSeeAllClick = { onSeeAllClick("popular") }
                        )
                    }
                    item {
                        SongHorizontalList(
                            songs = uiState.popularSongs.map { it.copy(isLiked = likedSongIds.contains(it.id)) },
                            onSongClick = onSongClick,
                            onLikeClick = onLikeClick
                        )
                    }
                }

                if (uiState.newReleases.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(id = R.string.home_section_new_releases),
                            onSeeAllClick = { onSeeAllClick("new_releases") }
                        )
                    }
                    item {
                        SongHorizontalList(
                            songs = uiState.newReleases.map { it.copy(isLiked = likedSongIds.contains(it.id)) },
                            onSongClick = onSongClick,
                            onLikeClick = onLikeClick
                        )
                    }
                }

                // ✨ اعمال کالبک جدید روی کلیک لیست افقی موسیقی جهان
                if (uiState.globalPlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(id = R.string.home_section_global_playlists),
                            onSeeAllClick = { onSeeAllClick("global_playlists") }
                        )
                    }
                    item {
                        PlaylistHorizontalList(
                            playlists = uiState.globalPlaylists,
                            onClick = { playlist -> onPlaylistClick(playlist.id) }
                        )
                    }
                }

                // ✨ اعمال کالبک جدید روی کلیک لیست افقی موسیقی محلی
                if (uiState.localPlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(id = R.string.home_section_local_playlists),
                            onSeeAllClick = { onSeeAllClick("local_playlists") }
                        )
                    }
                    item {
                        PlaylistHorizontalList(
                            playlists = uiState.localPlaylists,
                            onClick = { playlist -> onPlaylistClick(playlist.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HipkaTheme.dimens.spaceM, vertical = HipkaTheme.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceS))
            IconButton(onClick = { /* TODO */ }) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications")
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun FeaturedBanner(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HipkaTheme.dimens.spaceM, vertical = HipkaTheme.dimens.spaceS)
            .clip(RoundedCornerShape(HipkaTheme.dimens.cornerL))
            .clickable(onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(HipkaTheme.dimens.spaceM)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = song.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(HipkaTheme.dimens.cornerM))
            )

            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceM))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.home_featured_today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceXS))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceXS))
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(onActionClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HipkaTheme.dimens.spaceM, vertical = HipkaTheme.dimens.spaceL),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionItem(
            icon = Icons.Default.Favorite,
            label = stringResource(id = R.string.home_quick_liked),
            onClick = { onActionClick("liked") }
        )
        QuickActionItem(
            icon = Icons.Default.Schedule,
            label = stringResource(id = R.string.home_quick_recent),
            onClick = { onActionClick("recent") }
        )
        QuickActionItem(
            icon = Icons.Default.LibraryMusic,
            label = stringResource(id = R.string.home_quick_playlists),
            onClick = { onActionClick("playlists") }
        )
        QuickActionItem(
            icon = Icons.Default.Star,
            label = stringResource(id = R.string.home_quick_artists),
            onClick = { onActionClick("artists") }
        )
    }
}

@Composable
private fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(HipkaTheme.dimens.cornerM),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HipkaTheme.dimens.spaceM, vertical = HipkaTheme.dimens.spaceS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(id = R.string.home_see_all),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onSeeAllClick)
        )
    }
}

@Composable
private fun SongHorizontalList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HipkaTheme.dimens.spaceM),
        horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM)
    ) {
        items(songs, key = { it.id + "_square" }) { song ->
            SquareSongCard(
                song = song,
                onClick = { onSongClick(song) },
                onLikeClick = { onLikeClick(song) }
            )
        }
    }
}

@Composable
private fun SquareSongCard(
    song: Song,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(HipkaTheme.dimens.cornerM))
        )
        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onLikeClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Toggle Like",
                    tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaylistHorizontalList(playlists: List<Playlist>, onClick: (Playlist) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HipkaTheme.dimens.spaceM),
        horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM)
    ) {
        items(playlists, key = { it.id + "_wide" }) { playlist ->
            WidePlaylistCard(playlist = playlist, onClick = { onClick(playlist) })
        }
    }
}

@Composable
private fun WidePlaylistCard(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(HipkaTheme.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
        )
        Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceM))
        Column {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Playlist • ${playlist.category.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}