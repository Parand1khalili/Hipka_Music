package com.hipka.app.presentation.features.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import com.hipka.app.R
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.common.CoverImage
import com.hipka.app.presentation.theme.HipkaTheme

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HipkaTheme.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverImage(
                model = song.coverImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(HipkaTheme.dimens.albumCoverS)
                    .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
            )

            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceM))

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

            IconButton(onClick = onSkipPrevious) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(id = R.string.player_skip_previous_cd)
                )
            }

            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        id = if (isPlaying) R.string.player_pause_cd else R.string.player_play_cd
                    )
                )
            }

            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(id = R.string.player_skip_next_cd)
                )
            }
        }
    }
}
