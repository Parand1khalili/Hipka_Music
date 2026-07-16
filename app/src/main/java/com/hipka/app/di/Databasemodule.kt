package com.hipka.app.di

import android.content.Context
import androidx.room.Room
import com.hipka.app.data.local.database.HipkaDatabase
import com.hipka.app.data.local.dao.SongDao
import com.hipka.app.data.local.dao.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHipkaDatabase(@ApplicationContext context: Context): HipkaDatabase {
        return Room.databaseBuilder(
            context,
            HipkaDatabase::class.java,
            "hipka_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: HipkaDatabase): SongDao = database.songDao

    @Provides
    @Singleton
    fun provideSearchHistoryDao(database: HipkaDatabase): SearchHistoryDao =
        database.searchHistoryDao
}