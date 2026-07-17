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
import androidx.compose.material.icons.automirrored.filled.ArrowBack // ایمپورت جدید برای دکمه بازگشت
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
import com.hipka.app.presentation.common.shimmerEffect
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onPlaylistClick: (String) -> Unit = {},
    onBackClick: (() -> Unit)? = null, // ✨ اضافه شدن اکشن بک به صورت آپشنال برای حل باگ برگشت به هوم
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val globalSectionTitle = stringResource(id = R.string.home_section_global_playlists)
    val localSectionTitle = stringResource(id = R.string.home_section_local_playlists)
    val userSectionTitle = stringResource(id = R.string.home_quick_playlists)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.nav_playlists),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                // ✨ اگر دکمه بک پاس داده شده باشد، فلش بازگشت هوشمند نمایش داده می‌شود
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
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
                uiState.isLoading -> PlaylistsGridShimmer()

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

                uiState.worldPlaylists.isEmpty() && uiState.localPlaylists.isEmpty() && uiState.userPlaylists.isEmpty() -> {
                    PlaylistsEmptyState()
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(HipkaTheme.dimens.spaceM),
                    verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM),
                    horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM)
                ) {
                    playlistSection(
                        title = globalSectionTitle,
                        playlists = uiState.worldPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        indexOffset = 0
                    )

                    playlistSection(
                        title = localSectionTitle,
                        playlists = uiState.localPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        indexOffset = uiState.worldPlaylists.size
                    )

                    playlistSection(
                        title = userSectionTitle,
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
                        .shimmerEffect()
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
            text = stringResource(id = R.string.no_playlists_yet),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(HipkaTheme.dimens.spaceXS))
        Text(
            text = stringResource(id = R.string.no_playlists_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}