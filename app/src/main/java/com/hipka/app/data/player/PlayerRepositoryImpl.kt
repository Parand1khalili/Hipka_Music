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

// خطاهای IO شبکه (کد ۲۰۰۰-۲۹۹۹) اغلب موقتی‌اند (قطعی لحظه‌ای شبکه/CDN)؛ قبل از
// نمایش خطا به کاربر چند بار با تأخیر دوباره تلاش می‌کنیم
private const val MAX_IO_ERROR_RETRIES = 2
private const val RETRY_BACKOFF_MS = 800L

// مدت انتظار بعد از قطع اینترنت قبل از نمایش خطای «آفلاین هستید» به کاربر
private const val OFFLINE_ERROR_TIMEOUT_MS = 5000L

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor
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
        if (desiredPlaying && !_isPlaying.value) {
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
            _isBuffering.value = false
        }
    }

    private val playerListener = object : Player.Listener {
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

            if (isIoError && currentMediaId != null) {
                if (ioRetryMediaId != currentMediaId) {
                    ioRetryMediaId = currentMediaId
                    ioRetryCount = 0
                }

                if (ioRetryCount < MAX_IO_ERROR_RETRIES) {
                    ioRetryCount++
                    repositoryScope.launch {
                        delay(RETRY_BACKOFF_MS * ioRetryCount)
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

        desiredPlaying = true
        controller().apply {
            volume = 0f
            setMediaItems(songs.map { it.toMediaItem() }, safeStartIndex, 0L)
            prepare()
            play()
        }
        _currentSong.value = songs[safeStartIndex]
        evaluateNetworkPlaybackState()
    }

    override suspend fun pause() {
        desiredPlaying = false
        controller().pause()
        evaluateNetworkPlaybackState()
    }

    override suspend fun resume() {
        desiredPlaying = true
        controller().play()
        evaluateNetworkPlaybackState()
    }

    override suspend fun stop() {
        desiredPlaying = false
        offlineWatchdogJob?.cancel()
        offlineWatchdogJob = null
        mediaController?.apply {
            stop()
            clearMediaItems()
        }
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
