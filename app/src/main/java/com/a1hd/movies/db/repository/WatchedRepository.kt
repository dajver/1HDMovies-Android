package com.a1hd.movies.db.repository

import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.db.dao.WatchedMovieDao
import com.a1hd.movies.db.entity.WatchedMovieEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Show-level (or movie-level) "watched" marker. A watched show/movie is excluded from
 * Favorites and shown on the separate Watched screen. Mirrors the iOS `WatchedRepository`.
 */
@Singleton
class WatchedRepository @Inject constructor(
    private val dao: WatchedMovieDao
) {

    var onWatchedChanged: ((WatchedMovieEntity) -> Unit)? = null
    var onWatchedRemoved: ((String) -> Unit)? = null

    suspend fun markWatched(linkToDetails: String, name: String, thumbnail: String, type: String) {
        if (linkToDetails.isBlank()) return
        val entity = WatchedMovieEntity(
            linkToDetails = linkToDetails,
            name = name,
            thumbnail = thumbnail,
            type = type,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsert(entity)
        onWatchedChanged?.invoke(entity)
    }

    suspend fun markWatched(movie: MoviesDetailsDataModel) =
        markWatched(movie.linkToDetails, movie.name, movie.thumbnail, movie.type.name)

    suspend fun removeWatched(linkToDetails: String) {
        dao.delete(linkToDetails)
        onWatchedRemoved?.invoke(linkToDetails)
    }

    suspend fun isWatched(linkToDetails: String): Boolean = dao.get(linkToDetails) != null

    suspend fun getAll(): List<WatchedMovieEntity> = dao.getAll()

    suspend fun watchedLinks(): Set<String> = dao.getAll().map { it.linkToDetails }.toSet()
}
