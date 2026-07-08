package com.a1hd.movies.services

import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.api.repository.MovieSeasonDataModel
import com.a1hd.movies.api.repository.MovieType
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
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
    private val playbackProgressDao: PlaybackProgressDao,
    private val watchedMovieDao: WatchedMovieDao,
    private val watchedEpisodeDao: WatchedEpisodeDao,
    private val showEpisodeSnapshotDao: ShowEpisodeSnapshotDao,
    private val showNotificationDao: ShowNotificationDao,
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
            syncPlaybackProgress(uid)
            syncWatchedMovies(uid)
            syncWatchedEpisodes(uid)
            syncSnapshots(uid)
            syncNotifications(uid)
            lastSyncDate = System.currentTimeMillis()
            Log.i(TAG, "Full sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
        } finally {
            isSyncing = false
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Playback progress / watched / snapshots / notifications — last-writer-wins by a date field.
    // Firestore doc IDs can't contain '/', so the stable key is Base64-encoded into the doc id.
    // ---------------------------------------------------------------------------------------------

    private fun docId(key: String): String =
        Base64.encodeToString(key.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    // --- Playback progress ---
    suspend fun uploadPlaybackProgress(entity: PlaybackProgressEntity) {
        val uid = uid ?: return
        try {
            db.collection("users").document(uid).collection("playbackProgress")
                .document(docId(entity.contentLink)).set(playbackToMap(entity)).await()
        } catch (e: Exception) {
            Log.e(TAG, "uploadPlaybackProgress failed: ${e.message}")
        }
    }

    suspend fun deletePlaybackProgress(contentLink: String) {
        val uid = uid ?: return
        try {
            db.collection("users").document(uid).collection("playbackProgress")
                .document(docId(contentLink)).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deletePlaybackProgress failed: ${e.message}")
        }
    }

    private suspend fun syncPlaybackProgress(uid: String) {
        val cloud = db.collection("users").document(uid).collection("playbackProgress").get().await()
        val local = playbackProgressDao.getAll().associateBy { it.contentLink }
        val cloudByKey = cloud.documents.mapNotNull { d -> d.getString("contentLink")?.let { it to d } }.toMap()
        for ((key, d) in cloudByKey) {
            val cloudUpdated = d.getLong("updatedAt") ?: 0L
            val localEntity = local[key]
            if (localEntity == null || cloudUpdated > localEntity.updatedAt) {
                playbackProgressDao.upsert(
                    PlaybackProgressEntity(
                        contentLink = key,
                        positionMs = d.getLong("positionMs") ?: 0L,
                        durationMs = d.getLong("durationMs") ?: 0L,
                        updatedAt = cloudUpdated,
                        title = d.getString("title"),
                        thumbnail = d.getString("thumbnail"),
                        contentType = d.getString("contentType")
                    )
                )
            }
        }
        for (entity in local.values) {
            val cloudUpdated = cloudByKey[entity.contentLink]?.getLong("updatedAt")
            if (cloudUpdated == null || entity.updatedAt > cloudUpdated) uploadPlaybackProgress(entity)
        }
    }

    private fun playbackToMap(e: PlaybackProgressEntity): Map<String, Any?> = mapOf(
        "contentLink" to e.contentLink,
        "positionMs" to e.positionMs,
        "durationMs" to e.durationMs,
        "updatedAt" to e.updatedAt,
        "title" to e.title,
        "thumbnail" to e.thumbnail,
        "contentType" to e.contentType
    )

    // --- Watched movies (show-level) ---
    suspend fun uploadWatchedMovie(entity: WatchedMovieEntity) {
        val uid = uid ?: return
        try {
            db.collection("users").document(uid).collection("watchedMovies")
                .document(docId(entity.linkToDetails)).set(watchedMovieToMap(entity)).await()
        } catch (e: Exception) {
            Log.e(TAG, "uploadWatchedMovie failed: ${e.message}")
        }
    }

    suspend fun deleteWatchedMovie(linkToDetails: String) {
        val uid = uid ?: return
        try {
            db.collection("users").document(uid).collection("watchedMovies")
                .document(docId(linkToDetails)).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deleteWatchedMovie failed: ${e.message}")
        }
    }

    private suspend fun syncWatchedMovies(uid: String) {
        val cloud = db.collection("users").document(uid).collection("watchedMovies").get().await()
        val local = watchedMovieDao.getAll().associateBy { it.linkToDetails }
        val cloudByKey = cloud.documents.mapNotNull { d -> d.getString("linkToDetails")?.let { it to d } }.toMap()
        for ((key, d) in cloudByKey) {
            val cloudUpdated = d.getLong("updatedAt") ?: 0L
            val localEntity = local[key]
            if (localEntity == null || cloudUpdated > localEntity.updatedAt) {
                watchedMovieDao.upsert(
                    WatchedMovieEntity(
                        linkToDetails = key,
                        name = d.getString("name") ?: "",
                        thumbnail = d.getString("thumbnail") ?: "",
                        type = d.getString("type") ?: MovieType.MOVIE.name,
                        updatedAt = cloudUpdated
                    )
                )
            }
        }
        for (entity in local.values) {
            val cloudUpdated = cloudByKey[entity.linkToDetails]?.getLong("updatedAt")
            if (cloudUpdated == null || entity.updatedAt > cloudUpdated) uploadWatchedMovie(entity)
        }
    }

    private fun watchedMovieToMap(e: WatchedMovieEntity): Map<String, Any?> = mapOf(
        "linkToDetails" to e.linkToDetails,
        "name" to e.name,
        "thumbnail" to e.thumbnail,
        "type" to e.type,
        "updatedAt" to e.updatedAt
    )

    // --- Watched episodes ---
    suspend fun uploadWatchedEpisode(entity: WatchedEpisodeEntity) {
        val uid = uid ?: return
        try {
            db.collection("users").document(uid).collection("watchedEpisodes")
                .document(docId(entity.episodeLink)).set(watchedEpisodeToMap(entity)).await()
        } catch (e: Exception) {
            Log.e(TAG, "uploadWatchedEpisode failed: ${e.message}")
        }
    }

    suspend fun deleteWatchedEpisode(episodeLink: String) {
        val uid = uid ?: return
        try {
            db.collection("users").document(uid).collection("watchedEpisodes")
                .document(docId(episodeLink)).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deleteWatchedEpisode failed: ${e.message}")
        }
    }

    private suspend fun syncWatchedEpisodes(uid: String) {
        val cloud = db.collection("users").document(uid).collection("watchedEpisodes").get().await()
        val local = watchedEpisodeDao.getAll().associateBy { it.episodeLink }
        val cloudByKey = cloud.documents.mapNotNull { d -> d.getString("episodeLink")?.let { it to d } }.toMap()
        for ((key, d) in cloudByKey) {
            val cloudUpdated = d.getLong("updatedAt") ?: 0L
            val localEntity = local[key]
            if (localEntity == null || cloudUpdated > localEntity.updatedAt) {
                watchedEpisodeDao.upsert(
                    WatchedEpisodeEntity(
                        episodeLink = key,
                        showLink = d.getString("showLink") ?: "",
                        seasonNumber = d.getString("seasonNumber") ?: "",
                        episodeNumber = d.getString("episodeNumber") ?: "",
                        updatedAt = cloudUpdated
                    )
                )
            }
        }
        for (entity in local.values) {
            val cloudUpdated = cloudByKey[entity.episodeLink]?.getLong("updatedAt")
            if (cloudUpdated == null || entity.updatedAt > cloudUpdated) uploadWatchedEpisode(entity)
        }
    }

    private fun watchedEpisodeToMap(e: WatchedEpisodeEntity): Map<String, Any?> = mapOf(
        "episodeLink" to e.episodeLink,
        "showLink" to e.showLink,
        "seasonNumber" to e.seasonNumber,
        "episodeNumber" to e.episodeNumber,
        "updatedAt" to e.updatedAt
    )

    // --- Episode snapshots (last-writer-wins by lastCheckedAt) ---
    private suspend fun syncSnapshots(uid: String) {
        val cloud = db.collection("users").document(uid).collection("episodeSnapshots").get().await()
        val local = showEpisodeSnapshotDao.getAll().associateBy { it.showLink }
        val cloudByKey = cloud.documents.mapNotNull { d -> d.getString("showLink")?.let { it to d } }.toMap()
        for ((key, d) in cloudByKey) {
            val cloudChecked = d.getLong("lastCheckedAt") ?: 0L
            val localEntity = local[key]
            if (localEntity == null || cloudChecked > localEntity.lastCheckedAt) {
                showEpisodeSnapshotDao.upsert(
                    ShowEpisodeSnapshotEntity(
                        showLink = key,
                        episodeLinksJson = d.getString("episodeLinksJson") ?: "",
                        lastCheckedAt = cloudChecked
                    )
                )
            }
        }
        for (entity in local.values) {
            val cloudChecked = cloudByKey[entity.showLink]?.getLong("lastCheckedAt")
            if (cloudChecked == null || entity.lastCheckedAt > cloudChecked) {
                db.collection("users").document(uid).collection("episodeSnapshots")
                    .document(docId(entity.showLink))
                    .set(
                        mapOf(
                            "showLink" to entity.showLink,
                            "episodeLinksJson" to entity.episodeLinksJson,
                            "lastCheckedAt" to entity.lastCheckedAt
                        )
                    ).await()
            }
        }
    }

    // --- Show notifications (last-writer-wins by detectedAt) ---
    suspend fun uploadNotification(entity: ShowNotificationEntity) {
        val uid = uid ?: return
        try {
            db.collection("users").document(uid).collection("showNotifications")
                .document(docId(entity.showLink)).set(notificationToMap(entity)).await()
        } catch (e: Exception) {
            Log.e(TAG, "uploadNotification failed: ${e.message}")
        }
    }

    private suspend fun syncNotifications(uid: String) {
        val cloud = db.collection("users").document(uid).collection("showNotifications").get().await()
        val local = showNotificationDao.getAll().associateBy { it.showLink }
        val cloudByKey = cloud.documents.mapNotNull { d -> d.getString("showLink")?.let { it to d } }.toMap()
        for ((key, d) in cloudByKey) {
            val cloudDetected = d.getLong("detectedAt") ?: 0L
            val localEntity = local[key]
            if (localEntity == null || cloudDetected > localEntity.detectedAt) {
                showNotificationDao.upsert(
                    ShowNotificationEntity(
                        showLink = key,
                        showName = d.getString("showName") ?: "",
                        thumbnail = d.getString("thumbnail") ?: "",
                        newCount = (d.getLong("newCount") ?: 0L).toInt(),
                        latestEpisodeLabel = d.getString("latestEpisodeLabel") ?: "",
                        detectedAt = cloudDetected,
                        isRead = d.getBoolean("isRead") ?: false
                    )
                )
            }
        }
        for (entity in local.values) {
            val cloudDetected = cloudByKey[entity.showLink]?.getLong("detectedAt")
            if (cloudDetected == null || entity.detectedAt > cloudDetected) uploadNotification(entity)
        }
    }

    private fun notificationToMap(e: ShowNotificationEntity): Map<String, Any?> = mapOf(
        "showLink" to e.showLink,
        "showName" to e.showName,
        "thumbnail" to e.thumbnail,
        "newCount" to e.newCount,
        "latestEpisodeLabel" to e.latestEpisodeLabel,
        "detectedAt" to e.detectedAt,
        "isRead" to e.isRead
    )

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
