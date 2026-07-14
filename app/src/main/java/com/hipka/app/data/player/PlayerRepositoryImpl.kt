package com.hipka.app.data.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.PlayerRepository
import com.hipka.app.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayerRepository {

    private var mediaController: MediaController? = null

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
        }.also { mediaController = it }
    }

    override suspend fun playSong(song: Song) {
        controller().apply {
            setMediaItem(song.toMediaItem())
            prepare()
            play()
        }
    }

    override suspend fun pause() {
        controller().pause()
    }

    override suspend fun resume() {
        controller().play()
    }
}

private fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artistName)
        .setArtworkUri(Uri.parse(coverImageUrl))
        .build()

    return MediaItem.Builder()
        .setUri(audioUrl)
        .setMediaId(id)
        .setMediaMetadata(metadata)
        .build()
}
