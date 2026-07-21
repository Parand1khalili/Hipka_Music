package com.hipka.app.presentation.features.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
    onPlaylistClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit, // ✨ اضافه شدن اتصال به تنظیمات
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
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = HipkaTheme.dimens.spaceXL)
            ) {
                // 1. Top App Bar همراه با لوگوی سفارشی و اکشن تنظیمات
                item { HomeTopBar(onNavigateToSettings = onNavigateToSettings) }

                item { Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS)) }

                // 2. Featured Carousel Pager
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
                                pageSpacing = HipkaTheme.dimens.spaceM
                            ) { page ->
                                FeaturedBanner(
                                    song = uiState.carouselSongs[page],
                                    onClick = { onSongClick(uiState.carouselSongs[page]) },
                                    modifier = Modifier.padding(vertical = HipkaTheme.dimens.spaceXS)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = HipkaTheme.dimens.spaceS, bottom = HipkaTheme.dimens.spaceXS),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(uiState.carouselSongs.size) { index ->
                                    val selected = pagerState.currentPage == index
                                    val color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Quick Actions
                item {
                    QuickActionsRow(onActionClick = onQuickActionClick)
                }

                // 4. Popular Songs
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

                // 5. New Releases
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

                // 6. Global Playlists
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

                // 7. Local Playlists
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
private fun HomeTopBar(onNavigateToSettings: () -> Unit) {
    val isPersian = LocalConfiguration.current.locales[0].language == "fa"
    val logoResId = if (isPersian) R.drawable.ic_logo_fa else R.drawable.ic_logo_en

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HipkaTheme.dimens.spaceM, vertical = HipkaTheme.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(id = logoResId),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .height(38.dp)
                .width(120.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            TopBarIconButton(
                icon = Icons.Default.Notifications,
                contentDescription = "Notifications",
                onClick = { /* TODO Notifications */ }
            )
            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceS))
            TopBarIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onNavigateToSettings
            )
            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceS))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun TopBarIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(38.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FeaturedBanner(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(HipkaTheme.dimens.cornerL),
                clip = false
            )
            .clip(RoundedCornerShape(HipkaTheme.dimens.cornerL))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.80f)
                        ),
                        startY = 0f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.20f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(HipkaTheme.dimens.spaceM)
        ) {
            Text(
                text = stringResource(id = R.string.home_featured_today),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(HipkaTheme.dimens.spaceM),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceS))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
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
            shape = RoundedCornerShape(HipkaTheme.dimens.cornerL),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shadowElevation = 2.dp,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceXS))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
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
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
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
    onLikeClick: (Song) -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(148.dp),
        shape = RoundedCornerShape(HipkaTheme.dimens.cornerM),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(HipkaTheme.dimens.spaceXS)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
            ) {
                AsyncImage(
                    model = song.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { onLikeClick(song) }, modifier = Modifier.size(22.dp)) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Toggle Like",
                        tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
    Card(
        onClick = onClick,
        modifier = Modifier.width(232.dp),
        shape = RoundedCornerShape(HipkaTheme.dimens.cornerM),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(HipkaTheme.dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
            ) {
                AsyncImage(
                    model = playlist.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceM))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "پلی‌لیست • ${playlist.category.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}