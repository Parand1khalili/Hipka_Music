package com.hipka.app.di

import com.hipka.app.data.remote.api.FollowApi
import com.hipka.app.data.remote.api.MessageApi
import com.hipka.app.data.remote.api.PlaylistApi
import com.hipka.app.data.remote.api.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Reuses the Retrofit instance from AppModule — no second OkHttpClient/Retrofit is created here. */
@Module
@InstallIn(SingletonComponent::class)
object SocialModule {

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideFollowApi(retrofit: Retrofit): FollowApi = retrofit.create(FollowApi::class.java)

    @Provides
    @Singleton
    fun providePlaylistApi(retrofit: Retrofit): PlaylistApi = retrofit.create(PlaylistApi::class.java)

    @Provides
    @Singleton
    fun provideMessageApi(retrofit: Retrofit): MessageApi = retrofit.create(MessageApi::class.java)
}