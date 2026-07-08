package com.a1hd.movies.services

import android.content.SharedPreferences
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

/**
 * Cross-device sync using the SAME Firestore schema as the iOS app, so the two clients share data:
 * - favorites: presence sync (upload new / download missing / delete orphaned)
 * - watched / watchedEpisodes: presence sync (watchedAt Timestamp; Android adds a few extra fields)
 * - playbackProgress / episodeSnapshots / showNotifications: last-writer-wins by a date field
 *
 * IMPORTANT: iOS stores `position`/`duration` in **seconds** (Double) and dates as Firestore
 * `Timestamp`s — Android's Room stores millis, so we convert on the boundary. Docs are found by a
 * `whereEqualTo(<key>)` query (matching iOS's `.addDocument` + `whereField`) so there's one doc per
 * key across both platforms.
 */
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
        private const val SNAPSHOT_SEP = "\n"
    }

    var isSyncing = false
        private set

    var lastSyncError: String? = null
        private set

    var lastSyncDate: Long
        get() = prefs.getLong(LAST_SYNC_KEY, 0)
        private set(value) = prefs.edit().putLong(LAST_SYNC_KEY, value).apply()

    private val uid: String? get() = auth.currentUser?.uid

    private fun col(uid: String, name: String) =
        db.collection("users").document(uid).collection(name)

    // MARK: - Full sync (mirrors iOS order & behavior)

    suspend fun syncAll() {
        val uid = uid ?: run { Log.w(TAG, "Sync skipped — no user ID"); return }
        if (isSyncing) { Log.w(TAG, "Sync skipped — already syncing"); return }
        isSyncing = true
        lastSyncError = null
        try {
            uploadNewFavorites(uid); downloadFavorites(uid); syncDeletedFavorites(uid)
            uploadWatched(uid); downloadWatched(uid); syncDeletedWatched(uid)
            uploadWatchedEpisodes(uid); downloadWatchedEpisodes(uid)
            uploadPlaybackProgresses(uid); downloadPlaybackProgress(uid)
            uploadEpisodeSnapshots(uid); downloadEpisodeSnapshots(uid)
            uploadShowNotifications(uid); downloadShowNotifications(uid)
            Log.i(TAG, "Full sync completed")
        } catch (e: Exception) {
            lastSyncError = e.message ?: e.toString()
            Log.e(TAG, "Sync failed: ${e.message}", e)
        } finally {
            // Like iOS's `defer`, always stamp the sync time.
            lastSyncDate = System.currentTimeMillis()
            isSyncing = false
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Favorites  (collection "favorites", presence sync)
    // ---------------------------------------------------------------------------------------------

    suspend fun uploadFavorite(movie: MoviesDetailsDataModel) {
        val uid = uid ?: return
        try {
            col(uid, "favorites").add(favoriteToFirestore(movie)).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload favorite: ${e.message}")
        }
    }

    suspend fun deleteFavorite(movie: MoviesDetailsDataModel) {
        val uid = uid ?: return
        try {
            val snapshot = col(uid, "favorites")
                .whereEqualTo("linkToDetails", movie.linkToDetails).get().await()
            for (doc in snapshot.documents) doc.reference.delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete favorite: ${e.message}")
        }
    }

    private suspend fun uploadNewFavorites(uid: String) {
        val local = favoriteRepository.fetchAllFavorites()
        val cloud = col(uid, "favorites").get().await()
        val cloudLinks = cloud.documents.mapNotNull { it.getString("linkToDetails") }.toSet()
        for (favorite in local) {
            if (!cloudLinks.contains(favorite.linkToDetails)) {
                col(uid, "favorites").add(favoriteToFirestore(favorite)).await()
            }
        }
    }

    private suspend fun downloadFavorites(uid: String) {
        val cloud = col(uid, "favorites").get().await()
        val localLinks = favoriteRepository.fetchAllFavorites().map { it.linkToDetails }.toSet()
        for (doc in cloud.documents) {
            val data = doc.data ?: continue
            val link = data["linkToDetails"] as? String ?: continue
            if (localLinks.contains(link)) continue
            favoriteRepository.addWithoutSync(firestoreToFavorite(data))
        }
    }

    private suspend fun syncDeletedFavorites(uid: String) {
        val cloud = col(uid, "favorites").get().await()
        val localLinks = favoriteRepository.fetchAllFavorites().map { it.linkToDetails }.toSet()
        for (doc in cloud.documents) {
            val link = doc.getString("linkToDetails") ?: continue
            if (!localLinks.contains(link)) doc.reference.delete().await()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Watched  (collection "watched", presence sync; iOS keys: linkToDetails + watchedAt)
    // ---------------------------------------------------------------------------------------------

    suspend fun uploadWatchedMovie(entity: WatchedMovieEntity) {
        val uid = uid ?: return
        try {
            val existing = col(uid, "watched")
                .whereEqualTo("linkToDetails", entity.linkToDetails).get().await()
            if (existing.documents.isEmpty()) {
                col(uid, "watched").add(watchedToMap(entity)).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadWatchedMovie failed: ${e.message}")
        }
    }

    suspend fun deleteWatchedMovie(linkToDetails: String) {
        val uid = uid ?: return
        try {
            val snapshot = col(uid, "watched")
                .whereEqualTo("linkToDetails", linkToDetails).get().await()
            for (doc in snapshot.documents) doc.reference.delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deleteWatchedMovie failed: ${e.message}")
        }
    }

    private suspend fun uploadWatched(uid: String) {
        val cloud = col(uid, "watched").get().await()
        val cloudLinks = cloud.documents.mapNotNull { it.getString("linkToDetails") }.toSet()
        for (item in watchedMovieDao.getAll()) {
            if (!cloudLinks.contains(item.linkToDetails)) {
                col(uid, "watched").add(watchedToMap(item)).await()
            }
        }
    }

    private suspend fun downloadWatched(uid: String) {
        val cloud = col(uid, "watched").get().await()
        val localLinks = watchedMovieDao.getAll().map { it.linkToDetails }.toSet()
        for (doc in cloud.documents) {
            val link = doc.getString("linkToDetails") ?: continue
            if (localLinks.contains(link)) continue
            watchedMovieDao.upsert(
                WatchedMovieEntity(
                    linkToDetails = link,
                    name = doc.getString("name") ?: "",
                    thumbnail = doc.getString("thumbnail") ?: "",
                    type = doc.getString("type") ?: MovieType.MOVIE.name,
                    updatedAt = doc.getTimestamp("watchedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun syncDeletedWatched(uid: String) {
        val cloud = col(uid, "watched").get().await()
        val localLinks = watchedMovieDao.getAll().map { it.linkToDetails }.toSet()
        for (doc in cloud.documents) {
            val link = doc.getString("linkToDetails") ?: continue
            if (!localLinks.contains(link)) doc.reference.delete().await()
        }
    }

    private fun watchedToMap(e: WatchedMovieEntity): Map<String, Any> = mapOf(
        "linkToDetails" to e.linkToDetails,
        "watchedAt" to Timestamp(Date(e.updatedAt)),
        "name" to e.name,
        "thumbnail" to e.thumbnail,
        "type" to e.type
    )

    // ---------------------------------------------------------------------------------------------
    // Watched episodes  (collection "watchedEpisodes", presence sync; iOS key: episodeLink)
    // ---------------------------------------------------------------------------------------------

    suspend fun uploadWatchedEpisode(entity: WatchedEpisodeEntity) {
        val uid = uid ?: return
        try {
            val existing = col(uid, "watchedEpisodes")
                .whereEqualTo("episodeLink", entity.episodeLink).get().await()
            if (existing.documents.isEmpty()) {
                col(uid, "watchedEpisodes").add(watchedEpisodeToMap(entity)).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadWatchedEpisode failed: ${e.message}")
        }
    }

    suspend fun deleteWatchedEpisode(episodeLink: String) {
        val uid = uid ?: return
        try {
            val snapshot = col(uid, "watchedEpisodes")
                .whereEqualTo("episodeLink", episodeLink).get().await()
            for (doc in snapshot.documents) doc.reference.delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deleteWatchedEpisode failed: ${e.message}")
        }
    }

    private suspend fun uploadWatchedEpisodes(uid: String) {
        val cloud = col(uid, "watchedEpisodes").get().await()
        val cloudLinks = cloud.documents.mapNotNull { it.getString("episodeLink") }.toSet()
        for (item in watchedEpisodeDao.getAll()) {
            if (!cloudLinks.contains(item.episodeLink)) {
                col(uid, "watchedEpisodes").add(watchedEpisodeToMap(item)).await()
            }
        }
    }

    private suspend fun downloadWatchedEpisodes(uid: String) {
        val cloud = col(uid, "watchedEpisodes").get().await()
        val localLinks = watchedEpisodeDao.getAll().map { it.episodeLink }.toSet()
        for (doc in cloud.documents) {
            val link = doc.getString("episodeLink") ?: continue
            if (localLinks.contains(link)) continue
            watchedEpisodeDao.upsert(
                WatchedEpisodeEntity(
                    episodeLink = link,
                    showLink = doc.getString("showLink") ?: "",
                    seasonNumber = doc.getString("seasonNumber") ?: "",
                    episodeNumber = doc.getString("episodeNumber") ?: "",
                    updatedAt = doc.getTimestamp("watchedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            )
        }
    }

    private fun watchedEpisodeToMap(e: WatchedEpisodeEntity): Map<String, Any> = mapOf(
        "episodeLink" to e.episodeLink,
        "watchedAt" to Timestamp(Date(e.updatedAt)),
        "showLink" to e.showLink,
        "seasonNumber" to e.seasonNumber,
        "episodeNumber" to e.episodeNumber
    )

    // ---------------------------------------------------------------------------------------------
    // Playback progress  (collection "playbackProgress", last-writer-wins by updatedAt)
    // iOS fields: contentLink, position(sec Double), duration(sec Double), updatedAt(Timestamp), title, thumbnail, contentType
    // ---------------------------------------------------------------------------------------------

    suspend fun uploadPlaybackProgress(entity: PlaybackProgressEntity) {
        val uid = uid ?: return
        try {
            val snap = col(uid, "playbackProgress")
                .whereEqualTo("contentLink", entity.contentLink).get().await()
            val doc = snap.documents.firstOrNull()
            if (doc != null) {
                val cloudDate = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                if (entity.updatedAt >= cloudDate) doc.reference.set(playbackToMap(entity)).await()
            } else {
                col(uid, "playbackProgress").add(playbackToMap(entity)).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadPlaybackProgress failed: ${e.message}")
        }
    }

    suspend fun deletePlaybackProgress(contentLink: String) {
        val uid = uid ?: return
        try {
            val snapshot = col(uid, "playbackProgress")
                .whereEqualTo("contentLink", contentLink).get().await()
            for (doc in snapshot.documents) doc.reference.delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deletePlaybackProgress failed: ${e.message}")
        }
    }

    private suspend fun uploadPlaybackProgresses(uid: String) {
        for (item in playbackProgressDao.getAll()) uploadPlaybackProgress(item)
    }

    private suspend fun downloadPlaybackProgress(uid: String) {
        val cloud = col(uid, "playbackProgress").get().await()
        val localByLink = playbackProgressDao.getAll().associateBy { it.contentLink }
        for (doc in cloud.documents) {
            val link = doc.getString("contentLink") ?: continue
            val positionMs = ((doc.getDouble("position") ?: 0.0) * 1000).toLong()
            val durationMs = ((doc.getDouble("duration") ?: 0.0) * 1000).toLong()
            val date = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            val local = localByLink[link]
            if (local == null || date > local.updatedAt) {
                playbackProgressDao.upsert(
                    PlaybackProgressEntity(
                        contentLink = link,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        updatedAt = date,
                        title = doc.getString("title"),
                        thumbnail = doc.getString("thumbnail"),
                        contentType = doc.getString("contentType")
                    )
                )
            }
        }
    }

    private fun playbackToMap(e: PlaybackProgressEntity): Map<String, Any?> = mapOf(
        "contentLink" to e.contentLink,
        "position" to e.positionMs / 1000.0,
        "duration" to e.durationMs / 1000.0,
        "updatedAt" to Timestamp(Date(e.updatedAt)),
        "title" to (e.title ?: ""),
        "thumbnail" to (e.thumbnail ?: ""),
        "contentType" to (e.contentType ?: "")
    )

    // ---------------------------------------------------------------------------------------------
    // Episode snapshots  (collection "episodeSnapshots", last-writer-wins by lastCheckedAt)
    // iOS fields: linkToDetails, knownEpisodeLinks([String]), lastCheckedAt(Timestamp)
    // ---------------------------------------------------------------------------------------------

    private suspend fun uploadEpisodeSnapshots(uid: String) {
        for (item in showEpisodeSnapshotDao.getAll()) {
            try {
                val snap = col(uid, "episodeSnapshots")
                    .whereEqualTo("linkToDetails", item.showLink).get().await()
                val doc = snap.documents.firstOrNull()
                if (doc != null) {
                    val cloudDate = doc.getTimestamp("lastCheckedAt")?.toDate()?.time ?: 0L
                    if (item.lastCheckedAt > cloudDate) doc.reference.set(snapshotToMap(item)).await()
                } else {
                    col(uid, "episodeSnapshots").add(snapshotToMap(item)).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "uploadEpisodeSnapshot failed: ${e.message}")
            }
        }
    }

    private suspend fun downloadEpisodeSnapshots(uid: String) {
        val cloud = col(uid, "episodeSnapshots").get().await()
        val localByLink = showEpisodeSnapshotDao.getAll().associateBy { it.showLink }
        for (doc in cloud.documents) {
            val link = doc.getString("linkToDetails") ?: continue
            @Suppress("UNCHECKED_CAST")
            val links = (doc.get("knownEpisodeLinks") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val date = doc.getTimestamp("lastCheckedAt")?.toDate()?.time ?: 0L
            val local = localByLink[link]
            if (local == null || date > local.lastCheckedAt) {
                showEpisodeSnapshotDao.upsert(
                    ShowEpisodeSnapshotEntity(
                        showLink = link,
                        episodeLinksJson = links.joinToString(SNAPSHOT_SEP),
                        lastCheckedAt = date
                    )
                )
            }
        }
    }

    private fun snapshotToMap(e: ShowEpisodeSnapshotEntity): Map<String, Any> = mapOf(
        "linkToDetails" to e.showLink,
        "knownEpisodeLinks" to if (e.episodeLinksJson.isEmpty()) emptyList() else e.episodeLinksJson.split(SNAPSHOT_SEP),
        "lastCheckedAt" to Timestamp(Date(e.lastCheckedAt))
    )

    // ---------------------------------------------------------------------------------------------
    // Show notifications  (collection "showNotifications", last-writer-wins by detectedAt + isRead)
    // iOS fields: showLinkToDetails, showName, showThumbnail, newEpisodeCount,
    //             latestEpisodeNumber, latestEpisodeName, detectedAt(Timestamp), isRead
    // ---------------------------------------------------------------------------------------------

    suspend fun uploadNotification(entity: ShowNotificationEntity) {
        val uid = uid ?: return
        try {
            val snap = col(uid, "showNotifications")
                .whereEqualTo("showLinkToDetails", entity.showLink).get().await()
            val doc = snap.documents.firstOrNull()
            if (doc != null) {
                val cloudDate = doc.getTimestamp("detectedAt")?.toDate()?.time ?: 0L
                val cloudRead = doc.getBoolean("isRead") ?: false
                if (entity.detectedAt > cloudDate) {
                    doc.reference.set(notificationToMap(entity)).await()
                } else if (entity.detectedAt == cloudDate && entity.isRead && !cloudRead) {
                    doc.reference.update("isRead", true).await()
                }
            } else {
                col(uid, "showNotifications").add(notificationToMap(entity)).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadNotification failed: ${e.message}")
        }
    }

    private suspend fun uploadShowNotifications(uid: String) {
        for (item in showNotificationDao.getAll()) uploadNotification(item)
    }

    /** Immediately flags every cloud notification as read (mirrors iOS `markShowNotificationsRead`). */
    suspend fun markAllNotificationsRead() {
        val uid = uid ?: return
        try {
            val snapshot = col(uid, "showNotifications").get().await()
            for (doc in snapshot.documents) {
                if (doc.getBoolean("isRead") != true) {
                    doc.reference.update("isRead", true).await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "markAllNotificationsRead failed: ${e.message}")
        }
    }

    private suspend fun downloadShowNotifications(uid: String) {
        val cloud = col(uid, "showNotifications").get().await()
        val localByLink = showNotificationDao.getAll().associateBy { it.showLink }
        for (doc in cloud.documents) {
            val link = doc.getString("showLinkToDetails") ?: continue
            val cloudDate = doc.getTimestamp("detectedAt")?.toDate()?.time ?: 0L
            val cloudRead = doc.getBoolean("isRead") ?: false
            val label = listOf(doc.getString("latestEpisodeNumber") ?: "", doc.getString("latestEpisodeName") ?: "")
                .joinToString(" ").trim()
            val local = localByLink[link]
            if (local == null || cloudDate > local.detectedAt) {
                showNotificationDao.upsert(
                    ShowNotificationEntity(
                        showLink = link,
                        showName = doc.getString("showName") ?: "",
                        thumbnail = doc.getString("showThumbnail") ?: "",
                        newCount = (doc.getLong("newEpisodeCount") ?: 0L).toInt(),
                        latestEpisodeLabel = label,
                        detectedAt = cloudDate,
                        isRead = cloudRead
                    )
                )
            } else if (cloudDate == local.detectedAt && cloudRead && !local.isRead) {
                showNotificationDao.upsert(local.copy(isRead = true))
            }
        }
    }

    private fun notificationToMap(e: ShowNotificationEntity): Map<String, Any> = mapOf(
        "showLinkToDetails" to e.showLink,
        "showName" to e.showName,
        "showThumbnail" to e.thumbnail,
        "newEpisodeCount" to e.newCount,
        "latestEpisodeNumber" to "",
        "latestEpisodeName" to e.latestEpisodeLabel,
        "detectedAt" to Timestamp(Date(e.detectedAt)),
        "isRead" to e.isRead
    )

    // ---------------------------------------------------------------------------------------------
    // Favorite serialization (matches iOS `favoriteToFirestore` / `firestoreToFavorite`)
    // ---------------------------------------------------------------------------------------------

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
        movie.seasonsList?.takeIf { it.isNotEmpty() }?.let { seasons ->
            data["seasonsList"] = seasons.map { season ->
                mapOf(
                    "seasonId" to season.seasonId,
                    "seasonNumber" to season.seasonNumber,
                    "episodes" to season.episodes.map { ep ->
                        mapOf(
                            "episodeNumber" to ep.episodeNumber,
                            "episodeName" to ep.episodeName,
                            "link" to ep.link
                        )
                    }
                )
            }
        }
        return data
    }

    @Suppress("UNCHECKED_CAST")
    private fun firestoreToFavorite(data: Map<String, Any?>): MoviesDetailsDataModel {
        val type = if ((data["type"] as? String ?: "Movie") == "Movie") MovieType.MOVIE else MovieType.TV_SHOW
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
        (data["addedAt"] as? Timestamp)?.let { movie.addedAt = it.toDate().time }
        return movie
    }
}
