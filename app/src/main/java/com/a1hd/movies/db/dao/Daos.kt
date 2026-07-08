package com.a1hd.movies.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.a1hd.movies.db.entity.PlaybackProgressEntity
import com.a1hd.movies.db.entity.ShowEpisodeSnapshotEntity
import com.a1hd.movies.db.entity.ShowNotificationEntity
import com.a1hd.movies.db.entity.WatchedEpisodeEntity
import com.a1hd.movies.db.entity.WatchedMovieEntity

@Dao
interface PlaybackProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlaybackProgressEntity)

    @Query("SELECT * FROM playback_progress WHERE contentLink = :contentLink LIMIT 1")
    suspend fun get(contentLink: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress")
    suspend fun getAll(): List<PlaybackProgressEntity>

    @Query("DELETE FROM playback_progress WHERE contentLink = :contentLink")
    suspend fun delete(contentLink: String)
}

@Dao
interface WatchedMovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(watched: WatchedMovieEntity)

    @Query("SELECT * FROM watched_movie WHERE linkToDetails = :linkToDetails LIMIT 1")
    suspend fun get(linkToDetails: String): WatchedMovieEntity?

    @Query("SELECT * FROM watched_movie")
    suspend fun getAll(): List<WatchedMovieEntity>

    @Query("DELETE FROM watched_movie WHERE linkToDetails = :linkToDetails")
    suspend fun delete(linkToDetails: String)
}

@Dao
interface WatchedEpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(episode: WatchedEpisodeEntity)

    @Query("SELECT * FROM watched_episode WHERE episodeLink = :episodeLink LIMIT 1")
    suspend fun get(episodeLink: String): WatchedEpisodeEntity?

    @Query("SELECT * FROM watched_episode WHERE showLink = :showLink")
    suspend fun getForShow(showLink: String): List<WatchedEpisodeEntity>

    @Query("SELECT * FROM watched_episode")
    suspend fun getAll(): List<WatchedEpisodeEntity>

    @Query("DELETE FROM watched_episode WHERE episodeLink = :episodeLink")
    suspend fun delete(episodeLink: String)
}

@Dao
interface ShowEpisodeSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: ShowEpisodeSnapshotEntity)

    @Query("SELECT * FROM show_episode_snapshot WHERE showLink = :showLink LIMIT 1")
    suspend fun get(showLink: String): ShowEpisodeSnapshotEntity?

    @Query("SELECT * FROM show_episode_snapshot")
    suspend fun getAll(): List<ShowEpisodeSnapshotEntity>

    @Query("DELETE FROM show_episode_snapshot WHERE showLink = :showLink")
    suspend fun delete(showLink: String)
}

@Dao
interface ShowNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: ShowNotificationEntity)

    @Query("SELECT * FROM show_notification WHERE showLink = :showLink LIMIT 1")
    suspend fun get(showLink: String): ShowNotificationEntity?

    @Query("SELECT * FROM show_notification ORDER BY detectedAt DESC")
    suspend fun getAll(): List<ShowNotificationEntity>

    @Query("SELECT COALESCE(SUM(newCount), 0) FROM show_notification WHERE isRead = 0")
    suspend fun unreadCount(): Int

    @Query("UPDATE show_notification SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM show_notification WHERE showLink = :showLink")
    suspend fun delete(showLink: String)
}
