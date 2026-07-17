package com.hipka.app.presentation.features.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.theme.HipkaTheme
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.player_back_cd)
            )
        }

        val song = uiState.currentSong
        if (song == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.player_nothing_playing),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            NowPlayingContent(
                song = song,
                uiState = uiState,
                onIntent = onIntent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = HipkaTheme.dimens.spaceL)
            )
        }
    }
}

@Composable
private fun NowPlayingContent(
    song: Song,
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(HipkaTheme.dimens.albumCoverL)
                .clip(RoundedCornerShape(HipkaTheme.dimens.cornerL))
        )

        Box(modifier = Modifier.height(HipkaTheme.dimens.spaceL))

        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artistName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(modifier = Modifier.height(HipkaTheme.dimens.spaceL))

        var isDragging by remember { mutableStateOf(false) }
        var dragPositionMs by remember { mutableLongStateOf(0L) }
        val durationMs = uiState.progress.durationMs.coerceAtLeast(1L)
        val displayedPositionMs = if (isDragging) dragPositionMs else uiState.progress.positionMs

        Slider(
            value = displayedPositionMs.toFloat().coerceIn(0f, durationMs.toFloat()),
            valueRange = 0f..durationMs.toFloat(),
            onValueChange = {
                isDragging = true
                dragPositionMs = it.roundToLong()
            },
            onValueChangeFinished = {
                onIntent(PlayerIntent.SeekTo(dragPositionMs))
                isDragging = false
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatDuration(displayedPositionMs), style = MaterialTheme.typography.labelSmall)
            Text(text = formatDuration(uiState.progress.durationMs), style = MaterialTheme.typography.labelSmall)
        }

        Box(modifier = Modifier.height(HipkaTheme.dimens.spaceL))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceL)
        ) {
            IconButton(onClick = { onIntent(PlayerIntent.SkipPrevious) }) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(id = R.string.player_skip_previous_cd),
                    modifier = Modifier.size(36.dp)
                )
            }

            FilledIconButton(
                onClick = { onIntent(PlayerIntent.TogglePlayPause) },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        id = if (uiState.isPlaying) R.string.player_pause_cd else R.string.player_play_cd
                    ),
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = { onIntent(PlayerIntent.SkipNext) }) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(id = R.string.player_skip_next_cd),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
