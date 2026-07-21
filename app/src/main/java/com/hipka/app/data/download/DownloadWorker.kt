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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * دانلود فایل صوتی یک آهنگ در پس‌زمینه. طبق مستندات پروژه، فرآیند دانلود به
 * WorkManager سپرده می‌شود تا با بسته شدن اپ ادامه پیدا کند و در صورت قطع شدن
 * اینترنت دوباره تلاش شود (Result.retry).
 *
 * برای بازخورد به کاربر، یک نوتیفیکیشن پیشرفت نمایش داده می‌شود. عمداً از
 * setForeground استفاده نمی‌کنیم چون اجرای Foreground Service از پس‌زمینه در
 * اندروید ۱۲+ محدودیت‌های سختی دارد و می‌تواند باعث کرش شود؛ نوتیفیکیشن ساده
 * همان بازخورد را بدون آن ریسک می‌دهد.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val songDao: SongDao,
    @DownloadHttpClient private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val audioUrl = inputData.getString(KEY_AUDIO_URL) ?: return@withContext Result.failure()
        val songTitle = inputData.getString(KEY_SONG_TITLE).orEmpty()

        val notificationId = songId.hashCode()
        showProgressNotification(notificationId, songTitle, progress = 0, indeterminate = true)

        val downloadsDir = File(applicationContext.filesDir, DOWNLOADS_DIR_NAME).apply { mkdirs() }
        val targetFile = File(downloadsDir, "$songId.mp3")

        try {
            val request = Request.Builder().url(audioUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // خطای سمت سرور (مثل 403/404) با تلاش مجدد درست نمی‌شود
                    showFinishedNotification(notificationId, R.string.download_failed, songTitle)
                    return@withContext Result.failure()
                }

                val body = response.body ?: run {
                    showFinishedNotification(notificationId, R.string.download_failed, songTitle)
                    return@withContext Result.failure()
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                var lastReportedProgress = 0

                targetFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                // فقط در تغییرات محسوس نوتیفیکیشن را به‌روزرسانی می‌کنیم
                                if (progress >= lastReportedProgress + PROGRESS_STEP) {
                                    lastReportedProgress = progress
                                    showProgressNotification(notificationId, songTitle, progress, indeterminate = false)
                                }
                            }
                        }
                    }
                }
            }

            songDao.updateDownloadStatus(
                songId = songId,
                isDownloaded = true,
                localFilePath = targetFile.absolutePath
            )
            showFinishedNotification(notificationId, R.string.download_complete, songTitle)
            Result.success()
        } catch (e: Exception) {
            // فایل ناقص را پاک می‌کنیم تا در تلاش بعدی از ابتدا دانلود شود
            targetFile.delete()
            if (runAttemptCount >= MAX_ATTEMPTS) {
                showFinishedNotification(notificationId, R.string.download_failed, songTitle)
                Result.failure()
            } else {
                Result.retry()
            }
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
    }
}
