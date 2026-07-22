package com.hipka.app.presentation.features.likedsongs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.hipka.app.R
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.common.ShimmerSongList
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedSongsScreen(
    viewModel: LikedSongsViewModel = hiltViewModel(),
    onSongClick: (List<Song>, Song) -> Unit,
    onShuffleAllClick: (List<Song>) -> Unit, // ✨ کالبک جدید برای شافل سراسری در گراف ناوبری
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.liked_songs_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    ShimmerSongList()
                }
                state.error != null -> {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.songs.isEmpty() -> {
                    EmptyLikedSongsState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    val songsWithLikeStatus = state.songs.map { it.copy(isLiked = true) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = HipkaTheme.dimens.spaceM,
                            end = HipkaTheme.dimens.spaceM,
                            bottom = HipkaTheme.dimens.spaceXL
                        ),
                        verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
                    ) {
                        // 1. هدر جذاب صفحه شامل تعداد آهنگ‌ها و دکمه شافل بزرگ
                        item {
                            LikedSongsHeader(
                                totalSongs = songsWithLikeStatus.size,
                                onShuffleClick = { onShuffleAllClick(songsWithLikeStatus) }
                            )
                        }

                        // 2. لیست موزیک‌ها
                        items(songsWithLikeStatus, key = { it.id }) { song ->
                            SongItemPlaceholder(
                                song = song,
                                onClick = { onSongClick(songsWithLikeStatus, song) },
                                onLikeClick = { viewModel.onIntent(LikedSongsIntent.ToggleLike(song.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedSongsHeader(
    totalSongs: Int,
    onShuffleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HipkaTheme.dimens.spaceM),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(HipkaTheme.dimens.albumCoverM)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary
                        )
                    ),
                    shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.size(HipkaTheme.dimens.albumCoverS),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))

        Text(
            text = "$totalSongs ${if (totalSongs == 1) "Song" else "Songs"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))

        // دکمه بزرگ و مدرن Shuffle Play
        Button(
            onClick = onShuffleClick,
            contentPadding = PaddingValues(
                horizontal = HipkaTheme.dimens.spaceXL,
                vertical = HipkaTheme.dimens.spaceM
            ),
            shape = CircleShape,
            modifier = Modifier.height(HipkaTheme.dimens.miniPlayerHeight)
        ) {
            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceS))
            Text(
                text = "SHUFFLE PLAY ALL",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
    }
}

@Composable
fun SongItemPlaceholder(song: Song, onClick: () -> Unit, onLikeClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HipkaTheme.dimens.cornerM),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(HipkaTheme.dimens.spaceM)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onLikeClick) {
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
fun EmptyLikedSongsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(HipkaTheme.dimens.spaceXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(HipkaTheme.dimens.albumCoverS),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))
        Text(
            text = stringResource(id = R.string.no_liked_songs),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
        Text(
            text = stringResource(id = R.string.no_liked_songs_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}