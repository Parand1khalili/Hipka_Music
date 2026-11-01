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
import com.hipka.app.R
import com.hipka.app.core.network.NetworkMonitor
import com.hipka.app.domain.model.PlaybackProgress
import com.hipka.app.domain.model.RepeatMode
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// طول محو صدا در ابتدا/انتهای هر آهنگ (میلی‌ثانیه)
private const val SLEEP_TIMER_TICK_MS = 1000L

// خطاهای IO شبکه (کد ۲۰۰۰-۲۹۹۹) اغلب موقتی‌اند (قطعی لحظه‌ای شبکه/CDN)؛ قبل از
// نمایش خطا به کاربر چند بار با تأخیر دوباره تلاش می‌کنیم
private const val MAX_IO_ERROR_RETRIES = 2
private const val RETRY_BACKOFF_MS = 800L

// مدت انتظار بعد از قطع اینترنت قبل از نمایش خطای «آفلاین هستید» به کاربر
private const val OFFLINE_ERROR_TIMEOUT_MS = 5000L

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: com.hipka.app.data.local.dao.SongDao,
    private val networkMonitor: NetworkMonitor
) : PlayerRepository {

    private var mediaController: MediaController? = null
    private val controllerMutex = Mutex()
    private var retryJob: Job? = null

    // آهنگ‌های صف فعلی، برای بازیابی Song کامل هنگام تغییر آیتم در پلیر (چون MediaItem فقط metadata محدود دارد)
    private var queuedSongsById: Map<String, Song> = emptyMap()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null

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

    private val _isShuffleEnabled = MutableStateFlow(false)
    override val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    override val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    override val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // آخرین وضعیت شناخته‌شده اتصال اینترنت و اینکه آیا کاربر خواسته پخش انجام شود
    // (چه در حال پخش باشد چه هنوز منتظر بافر/شبکه)؛ این دو با هم تعیین می‌کنند که
    // آیا باید لودینگ روی دکمه پخش نشان داده شود یا خطای «آفلاین هستید» بعد از چند ثانیه
    private var isNetworkOnline = true
    private var desiredPlaying = false
    private var offlineWatchdogJob: Job? = null

    // شمارنده تلاش مجدد برای هر آیتم پخش، جدا نگه داشته می‌شود تا آهنگ بعدی از صفر شروع کند
    private var ioRetryCount = 0
    private var ioRetryMediaId: String? = null

    init {
        repositoryScope.launch {
            networkMonitor.isOnline.collect { online ->
                isNetworkOnline = online
                evaluateNetworkPlaybackState()
            }
        }
    }

    private fun evaluateNetworkPlaybackState() {
        if (desiredPlaying && !_isPlaying.value && _currentSong.value?.downloadedFile() == null) {
            if (!isNetworkOnline) {
                _isBuffering.value = true
                if (offlineWatchdogJob?.isActive != true) {
                    offlineWatchdogJob = repositoryScope.launch {
                        delay(OFFLINE_ERROR_TIMEOUT_MS)
                        if (desiredPlaying && !_isPlaying.value && !isNetworkOnline) {
                            _isBuffering.value = false
                            _playbackErrors.tryEmit(context.getString(R.string.player_offline_error))
                        }
                    }
                }
            }
        } else {
            offlineWatchdogJob?.cancel()
            offlineWatchdogJob = null
            _isBuffering.value = mediaController?.playbackState == Player.STATE_BUFFERING
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            desiredPlaying = playWhenReady
            if (!playWhenReady) retryJob?.cancel()
            evaluateNetworkPlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_ENDED) desiredPlaying = false
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                // پخش موفق از سر گرفته شد؛ شمارنده تلاش مجدد را ریست کن
                ioRetryCount = 0
                ioRetryMediaId = null
            }
            evaluateNetworkPlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            retryJob?.cancel()
            _progress.value = PlaybackProgress()
            _currentSong.value = mediaItem?.mediaId?.let { queuedSongsById[it] }
            ioRetryCount = 0
            ioRetryMediaId = null
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _isShuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode.toDomainRepeatMode()
        }

        override fun onPlayerError(error: PlaybackException) {
            _isPlaying.value = false

            val currentMediaId = mediaController?.currentMediaItem?.mediaId
            // همه‌ی کدهای خطای دسته IO بین ۲۰۰۰ تا ۲۹۹۹ هستند (شبکه، HTTP، محدوده کش و ...)
            val isIoError = error.errorCode in 2000..2999

            if (isIoError && currentMediaId != null && desiredPlaying) {
                if (ioRetryMediaId != currentMediaId) {
                    ioRetryMediaId = currentMediaId
                    ioRetryCount = 0
                }

                if (ioRetryCount < MAX_IO_ERROR_RETRIES) {
                    ioRetryCount++
                    retryJob?.cancel()
                    retryJob = repositoryScope.launch {
                        delay(RETRY_BACKOFF_MS * ioRetryCount)
                        if (!desiredPlaying || mediaController?.currentMediaItem?.mediaId != currentMediaId) return@launch
                        // prepare() دوباره از موقعیت فعلی لود می‌کند؛ خطاهای موقتی شبکه/CDN
                        // اغلب همین‌جا خودشان را حل می‌کنند بدون اینکه کاربر چیزی ببیند
                        mediaController?.apply {
                            prepare()
                            play()
                        }
                    }
                    return
                }
            }

            _isBuffering.value = false
            _playbackErrors.tryEmit(context.getString(R.string.playback_failed))
        }
    }

    private suspend fun controller(): MediaController = withContext(Dispatchers.Main.immediate) {
        controllerMutex.withLock {
            mediaController?.takeIf { it.isConnected }?.let { return@withLock it }
            mediaController?.release()
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token)
                .setListener(object : MediaController.Listener {
                    override fun onDisconnected(controller: MediaController) {
                        if (mediaController !== controller) return
                        _isPlaying.value = false
                        _isBuffering.value = false
                        desiredPlaying = false
                        progressJob?.cancel()
                        retryJob?.cancel()
                    }
                }).buildAsync()
            val connected = suspendCancellableCoroutine<MediaController> { continuation ->
                future.addListener({
                    try {
                        val result = future.get()
                        if (continuation.isActive) continuation.resume(result)
                        else result.release()
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(context))
                continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
            }
            connected.addListener(playerListener)
            mediaController = connected
            val queued = queuedSongsById.values.toList()
            val previousSong = _currentSong.value
            if (connected.mediaItemCount == 0 && queued.isNotEmpty() && previousSong != null) {
                val index = queued.indexOfFirst { it.id == previousSong.id }.coerceAtLeast(0)
                val position = _progress.value.positionMs
                connected.setMediaItems(queued.map { it.toMediaItem() }, index, position)
                connected.shuffleModeEnabled = _isShuffleEnabled.value
                connected.repeatMode = _repeatMode.value.toPlayerRepeatMode()
                connected.setPlaybackSpeed(_playbackSpeed.value)
                connected.prepare()
            }
            startPlaybackTicker(connected)
            connected
        }
    }

    private fun startPlaybackTicker(controller: MediaController) {
        progressJob?.cancel()
        progressJob = repositoryScope.launch {
            while (isActive && controller.isConnected) {
                delay(if (controller.isPlaying) 500L else 1_000L)
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

    override suspend fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    override suspend fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return

        retryJob?.cancel()
        val localDownloads = songDao.getDownloadedSongs().first().associateBy { it.id }
        val playableSongs = songs.map { song ->
            val local = localDownloads[song.id]
            song.copy(isDownloaded = local?.isDownloaded ?: song.isDownloaded,
                localFilePath = local?.localFilePath ?: song.localFilePath)
        }
        queuedSongsById = playableSongs.associateBy { it.id }
        val safeStartIndex = startIndex.coerceIn(songs.indices)

        desiredPlaying = true
        controller().apply {
            volume = 1f
            setMediaItems(playableSongs.map { it.toMediaItem() }, safeStartIndex, 0L)
            prepare()
            play()
        }
        _currentSong.value = playableSongs[safeStartIndex]
        evaluateNetworkPlaybackState()
    }

    override suspend fun pause() {
        desiredPlaying = false
        retryJob?.cancel()
        controller().pause()
        evaluateNetworkPlaybackState()
    }

    override suspend fun resume() {
        desiredPlaying = true
        controller().apply {
            if (playbackState == Player.STATE_IDLE) prepare()
            if (playbackState == Player.STATE_ENDED) seekToDefaultPosition()
            play()
        }
        evaluateNetworkPlaybackState()
    }

    override suspend fun stop() {
        retryJob?.cancel()
        cancelSleepTimer()
        desiredPlaying = false
        offlineWatchdogJob?.cancel()
        offlineWatchdogJob = null
        mediaController?.apply {
            stop()
            clearMediaItems()
        }
        progressJob?.cancel()
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
        queuedSongsById = emptyMap()
        _currentSong.value = null
        _isPlaying.value = false
        _progress.value = PlaybackProgress()
        _isBuffering.value = false
    }

    override suspend fun skipToNext() {
        controller().seekToNextMediaItem()
    }

    override suspend fun skipToPrevious() {
        controller().seekToPreviousMediaItem()
    }

    override suspend fun seekTo(positionMs: Long) {
        val controller = controller()
        controller.seekTo(positionMs.coerceIn(0L, controller.duration.coerceAtLeast(0L)))
        updateProgress(controller)
    }

    override fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        sleepTimerJob = repositoryScope.launch {
            val deadline = android.os.SystemClock.elapsedRealtime() + durationMs.coerceIn(0L, 24 * 60 * 60_000L)
            var remaining = (deadline - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)
            _sleepTimerRemainingMs.value = remaining
            while (remaining > 0) {
                delay(minOf(SLEEP_TIMER_TICK_MS, remaining))
                remaining = (deadline - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)
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
        require(speed.isFinite() && speed in 0.25f..3f)
        controller().setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        controller().shuffleModeEnabled = enabled
        _isShuffleEnabled.value = enabled
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        controller().repeatMode = mode.toPlayerRepeatMode()
        _repeatMode.value = mode
    }
}

private fun Int.toDomainRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}

private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
}

private fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artistName)
        .setArtworkUri(Uri.parse(coverImageUrl))
        .build()

    // پخش هوشمند: اگر فایل آفلاین آهنگ دانلود شده باشد، به جای مصرف اینترنت
    // مستقیماً از روی فایل لوکال پخش می‌شود
    val playbackUri = downloadedFile()?.let(Uri::fromFile)
        ?: Uri.parse(audioUrl)

    return MediaItem.Builder()
        .setUri(playbackUri)
        .setMediaId(id)
        .setMediaMetadata(metadata)
        .build()
}
