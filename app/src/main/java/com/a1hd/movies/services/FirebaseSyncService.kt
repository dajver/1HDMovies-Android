package com.a1hd.movies.services

import android.content.SharedPreferences
import android.util.Log
import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.api.repository.MovieSeasonDataModel
import com.a1hd.movies.api.repository.MovieType
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.ui.sections.favorite.repository.FavoriteRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncService @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val favoriteRepository: FavoriteRepository,
    private val prefs: SharedPreferences
) {

    companion object {
        private const val TAG = "FirebaseSync"
        private const val LAST_SYNC_KEY = "lastSyncDate"
    }

    var isSyncing = false
        private set

    var lastSyncDate: Long
        get() = prefs.getLong(LAST_SYNC_KEY, 0)
        private set(value) = prefs.edit().putLong(LAST_SYNC_KEY, value).apply()

    private val uid: String? get() = auth.currentUser?.uid

    suspend fun syncAll() {
        val uid = uid ?: run {
            Log.w(TAG, "Sync skipped — no user ID")
            return
        }
        if (isSyncing) {
            Log.w(TAG, "Sync skipped — already syncing")
            return
        }
        isSyncing = true
        try {
            uploadNewFavorites(uid)
            downloadFavorites(uid)
            syncDeletedFavorites(uid)
            lastSyncDate = System.currentTimeMillis()
            Log.i(TAG, "Full sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
        } finally {
            isSyncing = false
        }
    }

    suspend fun uploadFavorite(movie: MoviesDetailsDataModel) {
        val uid = uid ?: return
        try {
            uploadSingleFavorite(movie, uid)
            Log.i(TAG, "Uploaded favorite: ${movie.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload favorite: ${e.message}")
        }
    }

    suspend fun deleteFavorite(movie: MoviesDetailsDataModel) {
        val uid = uid ?: return
        try {
            val snapshot = db.collection("users").document(uid)
                .collection("favorites")
                .whereEqualTo("linkToDetails", movie.linkToDetails)
                .get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Log.i(TAG, "Deleted favorite from cloud: ${movie.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete favorite: ${e.message}")
        }
    }

    private suspend fun uploadNewFavorites(uid: String) {
        val localFavorites = favoriteRepository.fetchAllFavorites()
        val snapshot = db.collection("users").document(uid)
            .collection("favorites").get().await()

        val cloudLinks = snapshot.documents.mapNotNull { it.getString("linkToDetails") }.toSet()

        var uploaded = 0
        for (favorite in localFavorites) {
            if (!cloudLinks.contains(favorite.linkToDetails)) {
                uploadSingleFavorite(favorite, uid)
                uploaded++
            }
        }
        Log.i(TAG, "Uploaded $uploaded new favorites to cloud")
    }

    private suspend fun uploadSingleFavorite(movie: MoviesDetailsDataModel, uid: String) {
        val data = favoriteToFirestore(movie)
        db.collection("users").document(uid)
            .collection("favorites").add(data).await()
    }

    private suspend fun downloadFavorites(uid: String) {
        val snapshot = db.collection("users").document(uid)
            .collection("favorites").get().await()

        val localFavorites = favoriteRepository.fetchAllFavorites()
        val localLinks = localFavorites.map { it.linkToDetails }.toSet()

        var downloaded = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val linkToDetails = data["linkToDetails"] as? String ?: continue

            if (localLinks.contains(linkToDetails)) continue

            val movie = firestoreToFavorite(data)
            favoriteRepository.addWithoutSync(movie)
            downloaded++
        }
        Log.i(TAG, "Downloaded $downloaded favorites from cloud")
    }

    private suspend fun syncDeletedFavorites(uid: String) {
        val snapshot = db.collection("users").document(uid)
            .collection("favorites").get().await()

        val localFavorites = favoriteRepository.fetchAllFavorites()
        val localLinks = localFavorites.map { it.linkToDetails }.toSet()

        for (doc in snapshot.documents) {
            val linkToDetails = doc.getString("linkToDetails") ?: continue
            if (!localLinks.contains(linkToDetails)) {
                doc.reference.delete().await()
                Log.i(TAG, "Deleted orphaned cloud favorite: $linkToDetails")
            }
        }
    }

    private fun favoriteToFirestore(movie: MoviesDetailsDataModel): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "name" to movie.name,
            "thumbnail" to movie.thumbnail,
            "linkToWatch" to movie.linkToWatch,
            "linkToDetails" to movie.linkToDetails,
            "watchMovieLinkWithEpisodeId" to movie.watchMovieLinkWithEpisodeId,
            "type" to if (movie.type == MovieType.MOVIE) "Movie" else "TV Show",
            "description" to movie.description,
            "quality" to movie.quality,
            "cast" to movie.cast,
            "genre" to movie.genre,
            "duration" to movie.duration,
            "country" to movie.country,
            "imdb" to movie.imdb,
            "release" to movie.release,
            "production" to movie.production,
            "addedAt" to Timestamp(Date(movie.addedAt ?: System.currentTimeMillis()))
        )

        val seasons = movie.seasonsList
        if (!seasons.isNullOrEmpty()) {
            val seasonsData = seasons.map { season ->
                val episodes = season.episodes.map { ep ->
                    mapOf(
                        "episodeNumber" to ep.episodeNumber,
                        "episodeName" to ep.episodeName,
                        "link" to ep.link
                    )
                }
                mapOf(
                    "seasonId" to season.seasonId,
                    "seasonNumber" to season.seasonNumber,
                    "episodes" to episodes
                )
            }
            data["seasonsList"] = seasonsData
        }

        return data
    }

    @Suppress("UNCHECKED_CAST")
    private fun firestoreToFavorite(data: Map<String, Any?>): MoviesDetailsDataModel {
        val typeString = data["type"] as? String ?: "Movie"
        val type = if (typeString == "Movie") MovieType.MOVIE else MovieType.TV_SHOW

        val seasonsList = (data["seasonsList"] as? List<Map<String, Any?>>)?.map { seasonData ->
            val episodes = (seasonData["episodes"] as? List<Map<String, Any?>> ?: emptyList()).map { epData ->
                MovieEpisodesDataModel(
                    episodeNumber = epData["episodeNumber"] as? String ?: "",
                    episodeName = epData["episodeName"] as? String ?: "",
                    link = epData["link"] as? String ?: ""
                )
            }.toMutableList()
            MovieSeasonDataModel(
                seasonId = seasonData["seasonId"] as? String ?: "",
                seasonNumber = seasonData["seasonNumber"] as? String ?: "",
                episodes = episodes
            )
        }?.toMutableList()

        val movie = MoviesDetailsDataModel(
            name = data["name"] as? String ?: "",
            thumbnail = data["thumbnail"] as? String ?: "",
            linkToWatch = data["linkToWatch"] as? String ?: "",
            linkToDetails = data["linkToDetails"] as? String ?: "",
            watchMovieLinkWithEpisodeId = data["watchMovieLinkWithEpisodeId"] as? String ?: "",
            type = type,
            description = data["description"] as? String ?: "",
            quality = data["quality"] as? String ?: "",
            cast = data["cast"] as? String ?: "",
            genre = data["genre"] as? String ?: "",
            duration = data["duration"] as? String ?: "",
            country = data["country"] as? String ?: "",
            imdb = data["imdb"] as? String ?: "",
            release = data["release"] as? String ?: "",
            production = data["production"] as? String ?: "",
            seasonsList = seasonsList
        )

        val addedAt = data["addedAt"]
        if (addedAt is Timestamp) {
            movie.addedAt = addedAt.toDate().time
        }

        return movie
    }
}
