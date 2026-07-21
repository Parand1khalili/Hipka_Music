package com.hipka.app.presentation.features.player

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.hipka.app.R
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.theme.HipkaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onBackClick: () -> Unit,
    isDownloaded: Boolean = false,
    onDownloadClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val song = uiState.currentSong

    var dominantColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(song?.coverImageUrl) {
        dominantColor = song?.coverImageUrl?.let { extractDominantColor(context, it) }
    }

    val backgroundColor by animateColorAsState(
        targetValue = dominantColor ?: MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 600),
        label = "nowPlayingBackground"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor.copy(alpha = 0.55f), MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.player_back_cd)
                )
            }

            SleepTimerButton(
                remainingMs = uiState.sleepTimerRemainingMs,
                onSetTimer = { durationMs -> onIntent(PlayerIntent.SetSleepTimer(durationMs)) },
                onCancelTimer = { onIntent(PlayerIntent.CancelSleepTimer) }
            )
        }

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
                isDownloaded = isDownloaded,
                onDownloadClick = onDownloadClick,
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
    isDownloaded: Boolean,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
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

        AudioVisualizer(
            isPlaying = uiState.isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(HipkaTheme.dimens.visualizerHeight)
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

        Box(modifier = Modifier.height(HipkaTheme.dimens.spaceM))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
        ) {
            PlaybackSpeedButton(
                currentSpeed = uiState.playbackSpeed,
                onSpeedChange = { onIntent(PlayerIntent.SetPlaybackSpeed(it)) }
            )

            DownloadButton(
                isDownloaded = isDownloaded,
                onClick = onDownloadClick
            )
        }
    }
}

@Composable
private fun DownloadButton(
    isDownloaded: Boolean,
    onClick: () -> Unit
) {
    // برای کاربر عادی هم دکمه دیده می‌شود اما با کلیک، پیام نیاز به ارتقاء حساب نمایش داده می‌شود
    IconButton(onClick = onClick, enabled = !isDownloaded) {
        Icon(
            imageVector = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
            contentDescription = stringResource(
                id = if (isDownloaded) R.string.downloaded_cd else R.string.download_cd
            ),
            tint = if (isDownloaded) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private val PLAYBACK_SPEED_OPTIONS = listOf(1f, 1.5f, 2f)

@Composable
private fun PlaybackSpeedButton(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    TextButton(
        onClick = {
            // چرخش بین سرعت‌های مجاز: 1x -> 1.5x -> 2x -> 1x
            val currentIndex = PLAYBACK_SPEED_OPTIONS.indexOfFirst { it == currentSpeed }.coerceAtLeast(0)
            val nextSpeed = PLAYBACK_SPEED_OPTIONS[(currentIndex + 1) % PLAYBACK_SPEED_OPTIONS.size]
            onSpeedChange(nextSpeed)
        }
    ) {
        Text(
            text = stringResource(id = R.string.player_speed_label, formatSpeed(currentSpeed)),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()

private val SLEEP_TIMER_OPTIONS_MINUTES = listOf(1, 5, 15, 30, 60)

@Composable
private fun SleepTimerButton(
    remainingMs: Long?,
    onSetTimer: (Long) -> Unit,
    onCancelTimer: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isActive = remainingMs != null

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (remainingMs != null) {
            Text(
                text = formatDuration(remainingMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = stringResource(id = R.string.player_sleep_timer_cd),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                SLEEP_TIMER_OPTIONS_MINUTES.forEach { minutes ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.player_sleep_timer_minutes, minutes)) },
                        onClick = {
                            onSetTimer(minutes * 60_000L)
                            menuExpanded = false
                        }
                    )
                }

                if (isActive) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.player_sleep_timer_off)) },
                        onClick = {
                            onCancelTimer()
                            menuExpanded = false
                        }
                    )
                }
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

// رنگ غالب کاور آهنگ را استخراج می‌کند تا به صورت گرادیانت نرم روی پس‌زمینه صفحه پلیر اعمال شود
private suspend fun extractDominantColor(context: Context, imageUrl: String): Color? = withContext(Dispatchers.IO) {
    try {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // Palette باید بایت‌های واقعی بیت‌مپ را بخواند، نه بافر GPU
            .build()

        val bitmap = (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
            ?: return@withContext null

        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
        swatch?.rgb?.let { Color(it) }
    } catch (e: Exception) {
        null
    }
}
