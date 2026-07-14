package com.hipka.app.di

import com.hipka.app.data.player.PlayerRepositoryImpl
import com.hipka.app.data.repository.ChatRepositoryImpl
import com.hipka.app.data.repository.FollowRepositoryImpl
import com.hipka.app.data.repository.PlaylistRepositoryImpl
import com.hipka.app.data.repository.UserRepositoryImpl
import com.hipka.app.domain.repository.ChatRepository
import com.hipka.app.domain.repository.FollowRepository
import com.hipka.app.domain.repository.PlayerRepository
import com.hipka.app.domain.repository.PlaylistRepository
import com.hipka.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindFollowRepository(impl: FollowRepositoryImpl): FollowRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
