package com.hipka.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * کلاینت مخصوص دانلود فایل صوتی. نمی‌توان از OkHttpClient اصلی استفاده کرد چون آن
 * کلاینت هدرهای احراز هویت Supabase را به همه درخواست‌ها اضافه می‌کند و ارسال آن
 * هدرها به CDN آهنگ‌ها باعث رد شدن درخواست می‌شود.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    @Provides
    @Singleton
    @DownloadHttpClient
    fun provideDownloadOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // فایل‌های صوتی ممکن است بزرگ باشند، پس مهلت خواندن سخت‌گیرانه نیست
        .readTimeout(5, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()
}
