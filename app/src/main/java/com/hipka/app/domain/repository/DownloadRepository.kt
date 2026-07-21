package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    /** آهنگ‌هایی که فایل آفلاینشان روی گوشی ذخیره شده است */
    fun getDownloadedSongs(): Flow<List<Song>>

    /** شناسه آهنگ‌های دانلودشده، برای مشخص کردن وضعیت دکمه دانلود در لیست‌ها */
    fun getDownloadedSongIds(): Flow<List<String>>

    /** دانلود را به WorkManager می‌سپارد تا در پس‌زمینه انجام شود */
    fun enqueueDownload(song: Song)

    /** حذف فایل آفلاین و بازگرداندن وضعیت آهنگ به حالت استریم */
    suspend fun deleteDownload(songId: String)
}
