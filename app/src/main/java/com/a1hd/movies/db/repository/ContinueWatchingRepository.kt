package com.a1hd.movies.db.repository

import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.api.repository.MovieType
import com.a1hd.movies.etc.extensions.io
import com.a1hd.movies.ui.sections.favorite.repository.FavoriteRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One entry in the dashboard "Continue Watching" row. Holds everything needed to resume
 * playback (for the card tap) and to open details / remove (for the long-press menu).
 */
data class ContinueWatchingItem(
    val id: String,               // details URL (show) / movie watch URL — used to open details
    val title: String,
    val thumbnail: String,
    val subtitle: String,         // "Resume: …" / "Next: …" for shows, empty for movies
    val isShow: Boolean,
    val recency: Long,
    // Playback payload (matches Router.WatchMovie):
    val watchUrl: String,         // episode link / movie watch URL
    val episodes: ArrayList<MovieEpisodesDataModel>?,
    val episodeIndex: Int,
    val showLink: String?,
    val seasonNumber: String?,
    val contentType: String?
)

/**
 * Builds the Continue Watching list from favorites + watched-episode + playback-progress state,
 * and handles removal. Mirrors the iOS `ContinueWatchingViewModel` logic.
 */
@Singleton
class ContinueWatchingRepository @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val watchedRepository: WatchedRepository,
    private val watchedEpisodeRepository: WatchedEpisodeRepository,
    private val playbackProgressRepository: PlaybackProgressRepository
) {

    private data class EpisodeRef(
        val episode: MovieEpisodesDataModel,
        val seasonNumber: String,
        val seasonEpisodes: List<MovieEpisodesDataModel>
    )

    suspend fun build(): List<ContinueWatchingItem> = io {
        val items = mutableListOf<ContinueWatchingItem>()
        val watchedShows = watchedRepository.watchedLinks()

        // --- TV shows: favorited, not show-level-watched, with something to continue ---
        for (fav in favoriteRepository.fetchAllFavorites()) {
            if (fav.type != MovieType.TV_SHOW) continue
            if (watchedShows.contains(fav.linkToDetails)) continue
            val seasons = fav.seasonsList ?: continue

            val ordered = seasons.flatMap { season ->
                season.episodes.map { EpisodeRef(it, season.seasonNumber, season.episodes) }
            }
            if (ordered.isEmpty()) continue

            val watchedEntities = watchedEpisodeRepository.getForShow(fav.linkToDetails)
            val watchedSet = watchedEntities.map { it.episodeLink }.toSet()
            val furthestIndex = ordered.indexOfLast { watchedSet.contains(it.episode.link) }

            var target: EpisodeRef? = null
            var isResume = false
            var recency = 0L

            if (furthestIndex >= 0) {
                val furthest = ordered[furthestIndex]
                val progress = playbackProgressRepository.get(furthest.episode.link)
                if (progress != null && playbackProgressRepository.isResumable(progress.positionMs, progress.durationMs)) {
                    // Abandoned mid-way — propose resuming the same episode.
                    target = furthest
                    isResume = true
                    recency = progress.updatedAt
                } else {
                    // Advance to the episode after the furthest watched one.
                    val next = ordered.getOrNull(furthestIndex + 1)
                    if (next != null) {
                        target = next
                        isResume = false
                        recency = watchedEntities.find { it.episodeLink == furthest.episode.link }?.updatedAt ?: 0L
                    }
                }
            } else {
                // Never marked-watched, but maybe started (resumable progress on some episode).
                for (ref in ordered) {
                    val progress = playbackProgressRepository.get(ref.episode.link)
                    if (progress != null && playbackProgressRepository.isResumable(progress.positionMs, progress.durationMs)) {
                        target = ref
                        isResume = true
                        recency = progress.updatedAt
                        break
                    }
                }
            }

            val ref = target ?: continue
            val seasonEpisodes = ArrayList(ref.seasonEpisodes)
            val index = seasonEpisodes.indexOfFirst { it.link == ref.episode.link }.coerceAtLeast(0)
            val prefix = if (isResume) "Resume: " else "Next: "
            val label = prefix + "${ref.episode.episodeNumber} ${ref.episode.episodeName}".trim()

            items.add(
                ContinueWatchingItem(
                    id = fav.linkToDetails,
                    title = fav.name,
                    thumbnail = fav.thumbnail,
                    subtitle = label,
                    isShow = true,
                    recency = recency,
                    watchUrl = ref.episode.link,
                    episodes = seasonEpisodes,
                    episodeIndex = index,
                    showLink = fav.linkToDetails,
                    seasonNumber = ref.seasonNumber,
                    contentType = MovieType.TV_SHOW.name
                )
            )
        }

        // --- Movies: any partially-watched (resumable) movie, favorited or not ---
        for (movie in playbackProgressRepository.inProgressMovies()) {
            items.add(
                ContinueWatchingItem(
                    id = movie.contentLink,
                    title = movie.title ?: "",
                    thumbnail = movie.thumbnail ?: "",
                    subtitle = "",
                    isShow = false,
                    recency = movie.updatedAt,
                    watchUrl = movie.contentLink,
                    episodes = null,
                    episodeIndex = 0,
                    showLink = movie.contentLink,
                    seasonNumber = null,
                    contentType = movie.contentType ?: MovieType.MOVIE.name
                )
            )
        }

        items.sortedByDescending { it.recency }
    }

    /**
     * Remove from Continue Watching: a movie clears its progress; a TV show is marked
     * show-level watched (so it leaves the row / lands in Watched) and its target
     * episode's progress is cleared.
     */
    suspend fun remove(item: ContinueWatchingItem) {
        if (item.isShow) {
            watchedRepository.markWatched(item.id, item.title, item.thumbnail, MovieType.TV_SHOW.name)
            playbackProgressRepository.clear(item.watchUrl)
        } else {
            playbackProgressRepository.clear(item.id)
        }
    }
}
