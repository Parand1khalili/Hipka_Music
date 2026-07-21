package com.hipka.app.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hipka.app.data.local.dao.SongDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * دانلود فایل صوتی یک آهنگ در پس‌زمینه. طبق مستندات پروژه، فرآیند دانلود باید به
 * WorkManager سپرده شود تا حتی با بسته شدن اپ ادامه پیدا کند و در صورت قطع شدن
 * اینترنت، خود WorkManager دوباره تلاش کند (Result.retry).
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val songDao: SongDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val audioUrl = inputData.getString(KEY_AUDIO_URL) ?: return@withContext Result.failure()

        val downloadsDir = File(applicationContext.filesDir, DOWNLOADS_DIR_NAME).apply { mkdirs() }
        val targetFile = File(downloadsDir, "$songId.mp3")

        try {
            val request = Request.Builder().url(audioUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // خطای سمت سرور (مثل 403/404) با تلاش مجدد درست نمی‌شود
                    return@withContext Result.failure()
                }

                val body = response.body ?: return@withContext Result.failure()
                targetFile.outputStream().use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
            }

            songDao.updateDownloadStatus(
                songId = songId,
                isDownloaded = true,
                localFilePath = targetFile.absolutePath
            )
            Result.success()
        } catch (e: Exception) {
            // فایل ناقص را پاک می‌کنیم تا در تلاش بعدی از ابتدا دانلود شود
            targetFile.delete()
            Result.retry()
        }
    }

    companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_AUDIO_URL = "audio_url"
        const val DOWNLOADS_DIR_NAME = "downloads"
    }
}
