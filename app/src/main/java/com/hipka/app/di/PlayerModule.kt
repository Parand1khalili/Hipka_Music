package com.hipka.app.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

private const val CACHE_SIZE_BYTES = 300L * 1024 * 1024 // 300MB of streamed audio kept for offline replay

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        @ApplicationContext context: Context,
        cache: SimpleCache
    ): CacheDataSource.Factory {
        // DefaultDataSource هم file:// (آهنگ‌های دانلودشده) و هم http(s):// (استریم) را پشتیبانی می‌کند.
        // اگر مستقیم از DefaultHttpDataSource استفاده شود، پخش فایل لوکال با خطای Source شکست می‌خورد.
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        val upstreamFactory = DefaultDataSource.Factory(context, httpFactory)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
            // FLAG_IGNORE_CACHE_ON_ERROR: اگر خواندن از کش خطا داد، مستقیم از شبکه ادامه بده.
            // FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS: بعضی پاسخ‌های استریم صدا
            // بدون Content-Length می‌آیند؛ بدون این پرچم، CacheDataSource روی مرز بخش
            // کش‌شده/باقیمانده با «Source error» شکست می‌خورد.
            .setFlags(
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or
                    CacheDataSource.FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS
            )
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        cacheDataSourceFactory: CacheDataSource.Factory
    ): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
