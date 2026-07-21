package com.hipka.app.data.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.hipka.app.domain.model.PlaybackProgress
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.PlayerRepository
import com.hipka.app.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// طول محو صدا در ابتدا/انتهای هر آهنگ (میلی‌ثانیه)
private const val CROSSFADE_DURATION_MS = 3000L
private const val CROSSFADE_TICK_MS = 200L
private const val SLEEP_TIMER_TICK_MS = 1000L

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayerRepository {

    private var mediaController: MediaController? = null

    // آهنگ‌های صف فعلی، برای بازیابی Song کامل هنگام تغییر آیتم در پلیر (چون MediaItem فقط metadata محدود دارد)
    private var queuedSongsById: Map<String, Song> = emptyMap()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var crossfadeJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _progress = MutableStateFlow(PlaybackProgress())
    override val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private val _playbackErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val playbackErrors: SharedFlow<String> = _playbackErrors

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    override val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    private var sleepTimerJob: Job? = null

    private val _playbackSpeed = MutableStateFlow(1f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentSong.value = mediaItem?.mediaId?.let { queuedSongsById[it] }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isPlaying.value = false
            _playbackErrors.tryEmit(error.message ?: "Playback failed")
        }
    }

    private suspend fun controller(): MediaController {
        mediaController?.let { return it }

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()

        return suspendCancellableCoroutine { continuation ->
            future.addListener(
                { continuation.resume(future.get()) },
                MoreExecutors.directExecutor()
            )
            continuation.invokeOnCancellation { future.cancel(false) }
        }.also {
            it.addListener(playerListener)
            mediaController = it
            startPlaybackTicker(it)
        }
    }

    private fun startPlaybackTicker(controller: MediaController) {
        crossfadeJob?.cancel()
        crossfadeJob = repositoryScope.launch {
            while (isActive) {
                delay(CROSSFADE_TICK_MS)
                applyCrossfadeVolume(controller)
                updateProgress(controller)
            }
        }
    }

    private fun updateProgress(controller: MediaController) {
        val duration = controller.duration.coerceAtLeast(0L)
        _progress.value = PlaybackProgress(
            positionMs = controller.currentPosition.coerceAtLeast(0L),
            durationMs = duration
        )
    }

    private fun applyCrossfadeVolume(controller: MediaController) {
        if (!controller.isPlaying) return

        val duration = controller.duration
        if (duration <= 0) return

        val position = controller.currentPosition
        val remaining = duration - position

        val targetVolume = when {
            // محو ورود: شروع هر آهنگ (از جمله اولین آهنگ صف) با صدای کم شروع و در چند ثانیه به حداکثر می‌رسد
            position < CROSSFADE_DURATION_MS ->
                (position.toFloat() / CROSSFADE_DURATION_MS).coerceIn(0f, 1f)

            // محو خروج: فقط اگر آهنگ بعدی در صف وجود دارد، صدا در ثانیه‌های پایانی کم می‌شود
            remaining < CROSSFADE_DURATION_MS && controller.hasNextMediaItem() ->
                (remaining.toFloat() / CROSSFADE_DURATION_MS).coerceIn(0f, 1f)

            else -> 1f
        }

        if (controller.volume != targetVolume) {
            controller.volume = targetVolume
        }
    }

    override suspend fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    override suspend fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return

        queuedSongsById = songs.associateBy { it.id }
        val safeStartIndex = startIndex.coerceIn(songs.indices)

        controller().apply {
            volume = 0f
            setMediaItems(songs.map { it.toMediaItem() }, safeStartIndex, 0L)
            prepare()
            play()
        }
        _currentSong.value = songs[safeStartIndex]
    }

    override suspend fun pause() {
        controller().pause()
    }

    override suspend fun resume() {
        controller().play()
    }

    override suspend fun skipToNext() {
        controller().seekToNextMediaItem()
    }

    override suspend fun skipToPrevious() {
        controller().seekToPreviousMediaItem()
    }

    override suspend fun seekTo(positionMs: Long) {
        val controller = controller()
        controller.seekTo(positionMs)
        updateProgress(controller)
    }

    override fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        sleepTimerJob = repositoryScope.launch {
            var remaining = durationMs
            _sleepTimerRemainingMs.value = remaining
            while (remaining > 0) {
                delay(SLEEP_TIMER_TICK_MS)
                remaining = (remaining - SLEEP_TIMER_TICK_MS).coerceAtLeast(0)
                _sleepTimerRemainingMs.value = remaining
            }
            pause()
            _sleepTimerRemainingMs.value = null
        }
    }

    override fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        controller().setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }
}

private fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artistName)
        .setArtworkUri(Uri.parse(coverImageUrl))
        .build()

    // پخش هوشمند: اگر فایل آفلاین آهنگ دانلود شده باشد، به جای مصرف اینترنت
    // مستقیماً از روی فایل لوکال پخش می‌شود
    val playbackUri = localFilePath
        ?.takeIf { isDownloaded && File(it).exists() }
        ?.let { Uri.fromFile(File(it)) }
        ?: Uri.parse(audioUrl)

    return MediaItem.Builder()
        .setUri(playbackUri)
        .setMediaId(id)
        .setMediaMetadata(metadata)
        .build()
}
