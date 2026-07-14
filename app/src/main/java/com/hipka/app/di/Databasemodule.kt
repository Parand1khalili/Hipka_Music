package com.hipka.app.di

import android.content.Context
import androidx.room.Room
import com.hipka.app.data.local.database.HipkaDatabase
import com.hipka.app.data.local.dao.SongDao
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
            //  در صورت تغییر جداول در طول توسعه، برنامه کرش نکند
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: HipkaDatabase): SongDao = database.songDao
}