package com.a1hd.movies.services

import android.content.SharedPreferences
import com.a1hd.movies.api.repository.MovieType
import com.a1hd.movies.api.repository.ParseJsonMovieDetailsRepository
import com.a1hd.movies.db.entity.ShowNotificationEntity
import com.a1hd.movies.db.repository.NotificationRepository
import com.a1hd.movies.db.repository.WatchedEpisodeRepository
import com.a1hd.movies.db.repository.WatchedRepository
import com.a1hd.movies.etc.extensions.long
import com.a1hd.movies.ui.sections.favorite.repository.FavoriteRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects new episodes for favorited shows by diffing fresh episode data against a stored
 * snapshot. Mirrors the iOS `NewEpisodeService`: runs on launch (6h throttle); a manual
 * refresh in the Notifications screen bypasses the throttle with force=true.
 *
 * When a favorited show that is currently show-level-watched gets new episodes, the previously
 * known episodes are marked watched (baseline) and the show is un-watched, so it returns to
 * Favorites / Continue Watching landing on the first new episode.
 */
@Singleton
class NewEpisodeService @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val movieDetailsRepository: ParseJsonMovieDetailsRepository,
    private val watchedRepository: WatchedRepository,
    private val watchedEpisodeRepository: WatchedEpisodeRepository,
    private val notificationRepository: NotificationRepository,
    prefs: SharedPreferences
) {

    private var lastCheck: Long by prefs.long(LAST_CHECK_KEY, 0L)

    suspend fun checkForNewEpisodes(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastCheck < THROTTLE_MS) return

        val favoriteShows = favoriteRepository.fetchAllFavorites().filter { it.type == MovieType.TV_SHOW }
        for (fav in favoriteShows) {
            try {
                val fresh = movieDetailsRepository.fetchDetails(fav.linkToDetails)
                val freshEpisodes = fresh.seasonsList
                    ?.flatMap { season -> season.episodes.map { it to season } }
                    ?: continue
                val currentLinks = freshEpisodes.map { it.first.link }
                if (currentLinks.isEmpty()) continue

                val snapshot = notificationRepository.getSnapshot(fav.linkToDetails)
                if (snapshot == null) {
                    // First time seen — establish a baseline, no notification.
                    favoriteRepository.update(fresh)
                    notificationRepository.saveSnapshot(fav.linkToDetails, currentLinks)
                    continue
                }

                val oldLinks = notificationRepository.parseSnapshotLinks(snapshot).toSet()
                val newEpisodes = freshEpisodes.filter { !oldLinks.contains(it.first.link) }
                if (newEpisodes.isNotEmpty()) {
                    // Refresh the stored favorite so the new episodes show up in Continue Watching.
                    favoriteRepository.update(fresh)

                    // Resurface a show that was marked show-level watched.
                    if (watchedRepository.isWatched(fav.linkToDetails)) {
                        freshEpisodes
                            .filter { oldLinks.contains(it.first.link) }
                            .forEach { (episode, season) ->
                                watchedEpisodeRepository.markWatched(
                                    episode.link, fav.linkToDetails, season.seasonNumber, episode.episodeNumber
                                )
                            }
                        watchedRepository.removeWatched(fav.linkToDetails)
                    }

                    val existing = notificationRepository.get(fav.linkToDetails)
                    val baseCount = if (existing != null && !existing.isRead) existing.newCount else 0
                    val latest = newEpisodes.last().first
                    val label = "${latest.episodeNumber} ${latest.episodeName}".trim()
                    notificationRepository.upsert(
                        ShowNotificationEntity(
                            showLink = fav.linkToDetails,
                            showName = fav.name,
                            thumbnail = fav.thumbnail,
                            newCount = baseCount + newEpisodes.size,
                            latestEpisodeLabel = label,
                            detectedAt = now,
                            isRead = false
                        )
                    )
                }
                notificationRepository.saveSnapshot(fav.linkToDetails, currentLinks)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        lastCheck = System.currentTimeMillis()
    }

    companion object {
        private const val LAST_CHECK_KEY = "lastEpisodeCheck"
        private const val THROTTLE_MS = 6 * 60 * 60 * 1000L // 6 hours
    }
}
