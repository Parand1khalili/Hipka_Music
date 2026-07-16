package com.hipka.app.presentation.features.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.domain.model.Playlist
import com.hipka.app.presentation.common.shimmerEffect // استفاده از افکت شیمر مشترک گروه شما
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onPlaylistClick: (String) -> Unit = {}, // اکشن کلیک برای باز کردن جزئیات پلی‌لیست
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.nav_playlists),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // ۱. افکت لودینگ بر اساس ابزار طراحی‌شده توسط هم‌تیمی‌تان
                uiState.isLoading -> PlaylistsGridShimmer()

                // ۲. نمایش خطای احتمالی شبکه
                uiState.errorMessage != null -> Box(
                    modifier = Modifier.fillMaxSize().padding(HipkaTheme.dimens.spaceM),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // ۳. حالت خالی بودن پلی‌لیست‌ها
                uiState.worldPlaylists.isEmpty() && uiState.localPlaylists.isEmpty() && uiState.userPlaylists.isEmpty() -> {
                    PlaylistsEmptyState()
                }

                // ۴. نمایش کارت‌ها به صورت ۲ ستونه بر اساس رنگ‌بندی داینامیک تم
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(HipkaTheme.dimens.spaceM),
                    verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM),
                    horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM)
                ) {
                    // --- الف) موسیقی جهان ---
                    playlistSection(
                        title = "World music",
                        playlists = uiState.worldPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        indexOffset = 0
                    )
                    // --- ب) موسیقی داخلی ---
                    playlistSection(
                        title = "Local music",
                        playlists = uiState.localPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        indexOffset = uiState.worldPlaylists.size
                    )
                    // --- ج) پلی‌لیست‌های شخصی کاربر ---
                    playlistSection(
                        title = "Your playlists",
                        playlists = uiState.userPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        indexOffset = uiState.worldPlaylists.size + uiState.localPlaylists.size
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.playlistSection(
    title: String,
    playlists: List<Playlist>,
    onPlaylistClick: (String) -> Unit,
    indexOffset: Int
) {
    if (playlists.isEmpty()) return

    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = HipkaTheme.dimens.spaceS)
        )
    }
    itemsIndexed(playlists, key = { _, p -> p.id }) { index, playlist ->
        PlaylistCard(
            playlist = playlist,
            index = index + indexOffset,
            onClick = { onPlaylistClick(playlist.id) }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    index: Int,
    onClick: () -> Unit
) {
    // انتخاب پویای گرادیانت‌های رنگی از پالت تم بدون نوشتن مقادیر هگزادسیمال متفرقه
    val cardBackground = when (index % 3) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (index % 3) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(HipkaTheme.dimens.cornerM))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBackground)
                .padding(HipkaTheme.dimens.spaceM)
        ) {
            if (!playlist.coverImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = playlist.coverImageUrl,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
                )
            }

            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(
                        color = cardBackground.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(HipkaTheme.dimens.cornerS)
                    )
                    .padding(horizontal = HipkaTheme.dimens.spaceXS, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PlaylistsGridShimmer() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(HipkaTheme.dimens.spaceM),
        horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM)
    ) {
        items(6) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .shimmerEffect() // اعمال مودیفایر انیمیشن شیمر هم‌گروهی‌ات
                )
                Spacer(Modifier.height(HipkaTheme.dimens.spaceXS))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
private fun PlaylistsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HipkaTheme.dimens.spaceL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(HipkaTheme.dimens.albumCoverS)
        )
        Spacer(Modifier.height(HipkaTheme.dimens.spaceM))
        Text(
            text = "No playlists yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(HipkaTheme.dimens.spaceXS))
        Text(
            text = "World, local, and your own playlists will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}