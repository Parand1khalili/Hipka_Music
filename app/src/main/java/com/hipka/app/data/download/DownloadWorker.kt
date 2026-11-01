package com.hipka.app.data.download

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hipka.app.R
import com.hipka.app.data.local.dao.SongDao
import com.hipka.app.di.DownloadHttpClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** Bounded, retryable downloads; only complete private files become available offline. */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val songDao: SongDao,
    private val audiusApi: com.hipka.app.data.remote.api.AudiusApi,
    @DownloadHttpClient private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val audioUrl = inputData.getString(KEY_AUDIO_URL) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_SONG_TITLE).orEmpty()
        val notificationId = songId.hashCode()
        val directory = File(applicationContext.filesDir, DOWNLOADS_DIR_NAME).apply { mkdirs() }
        // Provider IDs are untrusted. A digest cannot escape the private download directory.
        val fileName = java.security.MessageDigest.getInstance("SHA-256")
            .digest(songId.toByteArray()).joinToString("") { "%02x".format(it) }
        val target = File(directory, "$fileName.audio")
        val partial = File(directory, "$fileName.$id.part")
        var call: okhttp3.Call? = null
        var finished = false
        fun finish(titleRes: Int) {
            finished = true
            showFinishedNotification(notificationId, titleRes, title)
        }
        try {
            val downloadUrl = if (songId.startsWith("audius:")) {
                val trackId = songId.removePrefix("audius:")
                val track = audiusApi.track(trackId).data
                if (track == null || !track.downloadable || track.downloadGated) return@withContext Result.failure()
                "https://api.audius.co/v1/tracks/$trackId/download?app_name=HipkaMusic"
            } else audioUrl
            require(downloadUrl.startsWith("https://"))
            showProgressNotification(notificationId, title, 0, true)
            call = okHttpClient.newCall(Request.Builder().url(downloadUrl).header("User-Agent", "HipkaMusic/1.0").build())
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    if ((response.code == 429 || response.code >= 500) && runAttemptCount < MAX_ATTEMPTS - 1) {
                        return@withContext Result.retry()
                    }
                    finish(R.string.download_failed)
                    return@withContext Result.failure()
                }
                val body = response.body ?: return@withContext Result.failure()
                val type = body.contentType()
                require(type?.type != "text" && type?.subtype?.contains("json") != true) { "Expected audio response" }
                val length = body.contentLength()
                require(length <= MAX_DOWNLOAD_BYTES) { "Download too large" }
                var count = 0L
                var reported = 0
                partial.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            count += read
                            require(count <= MAX_DOWNLOAD_BYTES) { "Download too large" }
                            output.write(buffer, 0, read)
                            if (length > 0) {
                                val progress = (count * 100 / length).toInt().coerceIn(0, 100)
                                if (progress >= reported + PROGRESS_STEP) {
                                    reported = progress
                                    showProgressNotification(notificationId, title, progress, false)
                                }
                            }
                        }
                    }
                }
                check(count > 0 && (length < 0 || count == length)) { "Incomplete download" }
            }
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            check(partial.renameTo(target)) { "Cannot finalize download" }
            songDao.insertSongsIfAbsent(listOf(com.hipka.app.data.local.entity.LocalSongEntity(
                id = songId, title = title, artistName = inputData.getString("artist").orEmpty(),
                coverImageUrl = inputData.getString("cover").orEmpty(), audioUrl = audioUrl,
                playCount = 0, releaseDate = "", isLiked = false, likesCount = 0,
                isDownloaded = false, localFilePath = null
            )))
            songDao.updateDownloadStatus(songId, true, target.absolutePath)
            finish(R.string.download_complete)
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            call?.cancel()
            throw cancelled
        } catch (error: java.io.IOException) {
            if (runAttemptCount < MAX_ATTEMPTS - 1) Result.retry() else {
                finish(R.string.download_failed)
                Result.failure()
            }
        } catch (error: Exception) {
            finish(R.string.download_failed)
            Result.failure()
        } finally {
            partial.delete()
            // Never leave a permanent ongoing notification after cancellation or retry.
            if (!finished) androidx.core.app.NotificationManagerCompat.from(applicationContext).cancel(notificationId)
        }
    }

    private fun showProgressNotification(
        notificationId: Int,
        songTitle: String,
        progress: Int,
        indeterminate: Boolean
    ) {
        val notification = baseNotification(
            title = applicationContext.getString(R.string.download_notification_title),
            text = songTitle,
            iconRes = android.R.drawable.stat_sys_download
        )
            .setOngoing(true)
            .setProgress(100, progress, indeterminate)
            .build()

        notify(notificationId, notification)
    }

    private fun showFinishedNotification(notificationId: Int, titleRes: Int, songTitle: String) {
        val notification = baseNotification(
            title = applicationContext.getString(titleRes),
            text = songTitle,
            iconRes = android.R.drawable.stat_sys_download_done
        )
            .setAutoCancel(true)
            .build()

        notify(notificationId, notification)
    }

    private fun baseNotification(title: String, text: String, iconRes: Int) =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(iconRes)
            .setPriority(NotificationCompat.PRIORITY_LOW)

    private fun notify(notificationId: Int, notification: android.app.Notification) {
        createNotificationChannel()

        // در اندروید ۱۳+ اگر کاربر اجازه نوتیفیکیشن نداده باشد، دانلود باید بی‌صدا ادامه یابد نه اینکه کرش کند
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.download_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_AUDIO_URL = "audio_url"
        const val KEY_SONG_TITLE = "song_title"
        const val DOWNLOADS_DIR_NAME = "downloads"

        private const val CHANNEL_ID = "hipka_downloads"
        private const val PROGRESS_STEP = 5
        private const val MAX_ATTEMPTS = 3
        private const val MAX_DOWNLOAD_BYTES = 250L * 1024 * 1024
    }
}
