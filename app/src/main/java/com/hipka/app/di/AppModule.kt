package com.hipka.app.di

import android.content.Context
import com.hipka.app.BuildConfig
import com.hipka.app.data.local.dao.SongDao
import com.hipka.app.data.local.datastore.SettingsDataStore
import com.hipka.app.data.remote.api.SongApi
import com.hipka.app.data.repository.SongRepositoryImpl
import com.hipka.app.domain.repository.SongRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.hipka.app.core.network.NetworkMonitor
import com.hipka.app.data.local.dao.SearchHistoryDao
import com.hipka.app.data.local.datastore.SessionManager

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", BuildConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_KEY}")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideSongApi(retrofit: Retrofit): SongApi {
        return retrofit.create(SongApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSongRepository(
        songApi: SongApi,
        songDao: SongDao,
        searchHistoryDao: SearchHistoryDao,
        networkMonitor: NetworkMonitor,
        sessionManager: SessionManager
    ): SongRepository {
        return SongRepositoryImpl(
            songApi,
            songDao,
            searchHistoryDao,
            networkMonitor,
            sessionManager
        )
    }
}