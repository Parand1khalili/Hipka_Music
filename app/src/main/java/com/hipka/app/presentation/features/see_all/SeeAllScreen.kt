package com.hipka.app.presentation.features.see_all

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.core.util.formatPlayCount
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    sectionName: String, // برای هماهنگی با متد فراخوانی قبلی شما نگه داشته شده است
    likedSongIds: Set<String>, // گرفتن وضعیت ست لایک‌ها به صورت لایو از اکتیویتی یا نویگیشن گراف مانند هوم اسکرین
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeeAllViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // تعیین عنوان اولیه بر اساس آرگومان (تا زمانی که داتا لود شود و از सक्सेस بیاید)
    val fallbackTitle = when (sectionName) {
        "popular" -> "Popular Songs"
        "new_releases" -> "New Releases"
        else -> "Songs"
    }

    val displayTitle = when (val state = uiState) {
        is SeeAllUiState.Success -> stringResource(id = state.titleResId)
        else -> fallbackTitle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = displayTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = HipkaTheme.dimens.spaceM)
        ) {
            when (val state = uiState) {
                is SeeAllUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is SeeAllUiState.Empty -> {
                    Text(
                        text = stringResource(id = R.string.empty_state_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SeeAllUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))
                        Button(onClick = { viewModel.loadSectionSongs() }) {
                            Text("Retry")
                        }
                    }
                }
                is SeeAllUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS),
                        contentPadding = PaddingValues(vertical = HipkaTheme.dimens.spaceS)
                    ) {
                        items(
                            items = state.songs,
                            key = { song -> song.id + "_see_all" }
                        ) { song ->
                            // بررسی لایو وضعیت لایک با کپی گرفتن از مدل کاملاً همگام با معماری هوم اسکرین شما
                            val isSongLiked = likedSongIds.contains(song.id)
                            val currentSong = song.copy(isLiked = isSongLiked)

                            SeeAllSongListItem(
                                song = currentSong,
                                onClick = { onSongClick(currentSong) },
                                onLikeClick = { onLikeClick(currentSong) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeeAllSongListItem(
    song: Song,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = HipkaTheme.dimens.spaceXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // کاور آهنگ (ابعاد از تم اختصاصی شما خوانده می‌شود: albumCoverS که برابر 48.dp است)
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(HipkaTheme.dimens.albumCoverS)
                .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
        )

        Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceM))

        // جزییات متنی آهنگ
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceXXS))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " • ${song.playCount.formatPlayCount()} plays",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // دکمه قلب (توجا / توخالی زنده بر اساس وضعیت دیتابیس)
        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Toggle Like",
                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}