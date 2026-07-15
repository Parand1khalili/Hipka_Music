package com.hipka.app.presentation.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.theme.HipkaTheme

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        uiState.songs.isEmpty() -> {
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
                contentPadding = PaddingValues(HipkaTheme.dimens.spaceM),
                verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
            ) {
                items(uiState.songs, key = { it.id }) { song ->
                    SongRow(song = song, onClick = { onSongClick(song) })
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
            .clickable(onClick = onClick)
            .padding(HipkaTheme.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(HipkaTheme.dimens.albumCoverS)
                .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = HipkaTheme.dimens.spaceM)
        ) {
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
    }
}
