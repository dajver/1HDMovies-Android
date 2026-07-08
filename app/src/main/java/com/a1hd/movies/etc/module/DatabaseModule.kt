package com.a1hd.movies.etc.module

import android.content.Context
import androidx.room.Room
import com.a1hd.movies.db.AppDatabase
import com.a1hd.movies.db.dao.PlaybackProgressDao
import com.a1hd.movies.db.dao.ShowEpisodeSnapshotDao
import com.a1hd.movies.db.dao.ShowNotificationDao
import com.a1hd.movies.db.dao.WatchedEpisodeDao
import com.a1hd.movies.db.dao.WatchedMovieDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "1hd-movies.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun providePlaybackProgressDao(db: AppDatabase): PlaybackProgressDao = db.playbackProgressDao()

    @Provides
    fun provideWatchedMovieDao(db: AppDatabase): WatchedMovieDao = db.watchedMovieDao()

    @Provides
    fun provideWatchedEpisodeDao(db: AppDatabase): WatchedEpisodeDao = db.watchedEpisodeDao()

    @Provides
    fun provideShowEpisodeSnapshotDao(db: AppDatabase): ShowEpisodeSnapshotDao = db.showEpisodeSnapshotDao()

    @Provides
    fun provideShowNotificationDao(db: AppDatabase): ShowNotificationDao = db.showNotificationDao()
}
