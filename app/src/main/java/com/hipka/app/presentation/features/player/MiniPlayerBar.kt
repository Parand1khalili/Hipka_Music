package com.hipka.app.presentation.features.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hipka.app.R
import com.hipka.app.domain.model.RepeatMode
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.common.CoverImage
import com.hipka.app.presentation.theme.HipkaTheme

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    isBuffering: Boolean = false,
    isShuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onClose: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(HipkaTheme.dimens.miniPlayerHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(HipkaTheme.dimens.cornerM),
        tonalElevation = HipkaTheme.dimens.spaceXS,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = HipkaTheme.dimens.spaceM, end = HipkaTheme.dimens.spaceM + 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverImage(
                    model = song.coverImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(HipkaTheme.dimens.albumCoverS)
                        .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
                )

                Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceS))

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

                ShuffleIconButton(
                    isEnabled = isShuffleEnabled,
                    onClick = onToggleShuffle,
                    buttonSize = 32.dp,
                    iconSize = 18.dp
                )

                RepeatIconButton(
                    repeatMode = repeatMode,
                    onClick = onCycleRepeatMode,
                    buttonSize = 32.dp,
                    iconSize = 18.dp
                )

                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(id = R.string.player_skip_previous_cd)
                    )
                }

                IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(44.dp)) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                id = if (isPlaying) R.string.player_pause_cd else R.string.player_play_cd
                            )
                        )
                    }
                }

                IconButton(onClick = onSkipNext, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(id = R.string.player_skip_next_cd)
                    )
                }
            }

            // دکمه بستن کوچک در گوشه بالا-راست: توقف کامل پخش و بستن مینی‌پلیر
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.player_close_cd),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ShuffleIconButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp,
    iconSize: Dp
) {
    IconButton(onClick = onClick, modifier = Modifier.size(buttonSize)) {
        Icon(
            imageVector = Icons.Filled.Shuffle,
            contentDescription = stringResource(
                id = if (isEnabled) R.string.player_shuffle_on_cd else R.string.player_shuffle_off_cd
            ),
            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun RepeatIconButton(
    repeatMode: RepeatMode,
    onClick: () -> Unit,
    buttonSize: Dp,
    iconSize: Dp
) {
    IconButton(onClick = onClick, modifier = Modifier.size(buttonSize)) {
        Icon(
            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = stringResource(
                id = when (repeatMode) {
                    RepeatMode.OFF -> R.string.player_repeat_off_cd
                    RepeatMode.ALL -> R.string.player_repeat_all_cd
                    RepeatMode.ONE -> R.string.player_repeat_one_cd
                }
            ),
            tint = if (repeatMode == RepeatMode.OFF) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(iconSize)
        )
    }
}
