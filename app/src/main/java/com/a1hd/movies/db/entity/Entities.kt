package com.a1hd.movies.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Resume-playback position for a single piece of content, keyed by the stable
 * content link (episode link for shows / movie watch URL for movies) — never the
 * per-session .m3u8. Mirrors the iOS `PlaybackProgress` SwiftData model.
 *
 * title / thumbnail / contentType are populated only for movies (episodes get theirs
 * from the favorited show) so the Continue Watching row can render a movie card.
 */
@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val contentLink: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val title: String? = null,
    val thumbnail: String? = null,
    val contentType: String? = null
)

/**
 * Show-level (or movie-level) "watched" marker. Keyed by the details URL.
 * Mirrors the iOS `WatchedMovie` model.
 */
@Entity(tableName = "watched_movie")
data class WatchedMovieEntity(
    @PrimaryKey val linkToDetails: String,
    val name: String,
    val thumbnail: String,
    val type: String,
    val updatedAt: Long
)

/**
 * Per-episode watched marker. Keyed by the episode link.
 * Mirrors the iOS `WatchedEpisode` model.
 */
@Entity(tableName = "watched_episode")
data class WatchedEpisodeEntity(
    @PrimaryKey val episodeLink: String,
    val showLink: String,
    val seasonNumber: String,
    val episodeNumber: String,
    val updatedAt: Long
)

/**
 * Snapshot of the episode links known for a favorited show, used to diff against
 * fresh data to detect new episodes. Mirrors the iOS `ShowEpisodeSnapshot` model.
 */
@Entity(tableName = "show_episode_snapshot")
data class ShowEpisodeSnapshotEntity(
    @PrimaryKey val showLink: String,
    val episodeLinksJson: String,
    val lastCheckedAt: Long
)

/**
 * One notification per favorited show carrying a count of new episodes since last read.
 * Mirrors the iOS `ShowNotification` model.
 */
@Entity(tableName = "show_notification")
data class ShowNotificationEntity(
    @PrimaryKey val showLink: String,
    val showName: String,
    val thumbnail: String,
    val newCount: Int,
    val latestEpisodeLabel: String,
    val detectedAt: Long,
    val isRead: Boolean
)
