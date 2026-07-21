package com.hipka.app.presentation.features.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun DownloadsScreen(
    onSongClick: (Song) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.downloads_title),
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> ShimmerSongList()

                state.error != null -> Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )

                state.songs.isEmpty() -> EmptyDownloadsState(modifier = Modifier.align(Alignment.Center))

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = HipkaTheme.dimens.spaceM,
                        end = HipkaTheme.dimens.spaceM,
                        bottom = HipkaTheme.dimens.spaceXL
                    ),
                    verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
                ) {
                    item {
                        DownloadsHeader(
                            totalSongs = state.songs.size,
                            sortOrder = state.sortOrder,
                            onSortOrderChange = { viewModel.onIntent(DownloadsIntent.ChangeSortOrder(it)) }
                        )
                    }

                    items(state.songs, key = { it.id }) { song ->
                        SwipeToDeleteSongItem(
                            song = song,
                            onClick = { onSongClick(song) },
                            onDelete = { viewModel.onIntent(DownloadsIntent.DeleteDownload(song.id)) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteSongItem(
    song: Song,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // فقط کشیدن کامل به هر طرف باعث حذف می‌شود
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // پس‌زمینه کمی تودررفته و پررنگ‌تر است تا هنگام کشیدن، از خود کارت جدا دیده شود
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = HipkaTheme.dimens.spaceXXS)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
                    )
                    .padding(horizontal = HipkaTheme.dimens.spaceL),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(id = R.string.downloads_delete_cd),
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Text(
                        text = stringResource(id = R.string.downloads_delete_cd),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    ) {
        DownloadedSongItem(song = song, onClick = onClick, onDelete = onDelete)
    }
}

@Composable
private fun DownloadedSongItem(song: Song, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HipkaTheme.dimens.cornerM),
        // کارت باید کاملاً مات باشد تا هنگام کشیدن، پس‌زمینه قرمز حذف از پشت آن دیده نشود
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
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

            Icon(
                imageVector = Icons.Filled.DownloadDone,
                contentDescription = stringResource(id = R.string.downloaded_cd),
                tint = MaterialTheme.colorScheme.primary
            )

            // علاوه بر Swipe to Dismiss، یک دکمه حذف همیشه‌دیده‌شده هم داریم
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.downloads_delete_cd),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsHeader(
    totalSongs: Int,
    sortOrder: DownloadsSortOrder,
    onSortOrderChange: (DownloadsSortOrder) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = HipkaTheme.dimens.spaceS)) {
        Text(
            text = stringResource(id = R.string.downloads_count, totalSongs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))

        Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
            FilterChip(
                selected = sortOrder == DownloadsSortOrder.TITLE,
                onClick = { onSortOrderChange(DownloadsSortOrder.TITLE) },
                label = { Text(text = stringResource(id = R.string.downloads_sort_by_title)) }
            )
            FilterChip(
                selected = sortOrder == DownloadsSortOrder.ARTIST,
                onClick = { onSortOrderChange(DownloadsSortOrder.ARTIST) },
                label = { Text(text = stringResource(id = R.string.downloads_sort_by_artist)) }
            )
        }
    }
}

@Composable
private fun EmptyDownloadsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(HipkaTheme.dimens.spaceXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.DownloadDone,
            contentDescription = null,
            modifier = Modifier.size(HipkaTheme.dimens.albumCoverS),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))
        Text(
            text = stringResource(id = R.string.no_downloads),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
        Text(
            text = stringResource(id = R.string.no_downloads_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
