package com.a1hd.movies.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.a1hd.movies.db.dao.PlaybackProgressDao
import com.a1hd.movies.db.dao.ShowEpisodeSnapshotDao
import com.a1hd.movies.db.dao.ShowNotificationDao
import com.a1hd.movies.db.dao.WatchedEpisodeDao
import com.a1hd.movies.db.dao.WatchedMovieDao
import com.a1hd.movies.db.entity.PlaybackProgressEntity
import com.a1hd.movies.db.entity.ShowEpisodeSnapshotEntity
import com.a1hd.movies.db.entity.ShowNotificationEntity
import com.a1hd.movies.db.entity.WatchedEpisodeEntity
import com.a1hd.movies.db.entity.WatchedMovieEntity

@Database(
    entities = [
        PlaybackProgressEntity::class,
        WatchedMovieEntity::class,
        WatchedEpisodeEntity::class,
        ShowEpisodeSnapshotEntity::class,
        ShowNotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun watchedMovieDao(): WatchedMovieDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao
    abstract fun showEpisodeSnapshotDao(): ShowEpisodeSnapshotDao
    abstract fun showNotificationDao(): ShowNotificationDao
}
