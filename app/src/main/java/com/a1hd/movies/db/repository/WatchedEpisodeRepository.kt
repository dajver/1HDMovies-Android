package com.a1hd.movies.db.repository

import com.a1hd.movies.db.dao.WatchedEpisodeDao
import com.a1hd.movies.db.entity.WatchedEpisodeEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-episode watched tracking. An episode is marked watched after 5 minutes of playback
 * or manually via long-press. Mirrors the iOS `WatchedEpisodeRepository`.
 */
@Singleton
class WatchedEpisodeRepository @Inject constructor(
    private val dao: WatchedEpisodeDao
) {

    var onWatchedChanged: ((WatchedEpisodeEntity) -> Unit)? = null
    var onWatchedRemoved: ((String) -> Unit)? = null

    suspend fun markWatched(episodeLink: String, showLink: String, seasonNumber: String, episodeNumber: String) {
        if (episodeLink.isBlank()) return
        val entity = WatchedEpisodeEntity(
            episodeLink = episodeLink,
            showLink = showLink,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsert(entity)
        onWatchedChanged?.invoke(entity)
    }

    suspend fun removeWatched(episodeLink: String) {
        dao.delete(episodeLink)
        onWatchedRemoved?.invoke(episodeLink)
    }

    suspend fun isWatched(episodeLink: String): Boolean = dao.get(episodeLink) != null

    suspend fun getForShow(showLink: String): List<WatchedEpisodeEntity> = dao.getForShow(showLink)

    suspend fun watchedLinksForShow(showLink: String): Set<String> =
        dao.getForShow(showLink).map { it.episodeLink }.toSet()

    suspend fun getAll(): List<WatchedEpisodeEntity> = dao.getAll()
}
