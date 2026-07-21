package com.hipka.app.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hipka.app.data.local.dao.SongDao
import com.hipka.app.data.local.entity.LocalSongEntity
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) : DownloadRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun getDownloadedSongs(): Flow<List<Song>> =
        songDao.getDownloadedSongs().map { entities -> entities.map { it.toDomain() } }

    override fun getDownloadedSongIds(): Flow<List<String>> = songDao.getDownloadedSongIds()

    override fun enqueueDownload(song: Song) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_SONG_ID to song.id,
                    DownloadWorker.KEY_AUDIO_URL to song.audioUrl
                )
            )
            // بدون اینترنت اصلاً شروع نمی‌شود و به محض وصل شدن خودکار اجرا می‌شود
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // KEEP یعنی اگر همین آهنگ از قبل در صف دانلود است، دوباره صف نشود
        workManager.enqueueUniqueWork(
            downloadWorkName(song.id),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override suspend fun deleteDownload(songId: String) {
        workManager.cancelUniqueWork(downloadWorkName(songId))

        songDao.getSongById(songId)?.localFilePath?.let { path ->
            File(path).delete()
        }
        songDao.updateDownloadStatus(songId = songId, isDownloaded = false, localFilePath = null)
    }

    private fun downloadWorkName(songId: String) = "download_$songId"
}

private fun LocalSongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl,
    isLiked = isLiked,
    likesCount = likesCount,
    playCount = playCount,
    releaseDate = releaseDate,
    isDownloaded = isDownloaded,
    localFilePath = localFilePath
)
