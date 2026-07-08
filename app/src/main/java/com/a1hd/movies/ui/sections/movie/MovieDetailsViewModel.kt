package com.a1hd.movies.ui.sections.movie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.api.repository.MovieSeasonDataModel
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.etc.extensions.launch
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.api.repository.ParseJsonMovieDetailsRepository
import com.a1hd.movies.api.repository.ParseJsonYouMayAlsoLikeRepository
import com.a1hd.movies.db.repository.WatchedEpisodeRepository
import com.a1hd.movies.db.repository.WatchedRepository
import com.a1hd.movies.db.repository.WatchingShowRepository
import com.a1hd.movies.ui.sections.favorite.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val parseJsonMovieDetailsRepository: ParseJsonMovieDetailsRepository,
    private val parseJsonYouMayAlsoLikeRepository: ParseJsonYouMayAlsoLikeRepository,
    private val favoriteRepository: FavoriteRepository,
    private val watchedRepository: WatchedRepository,
    private val watchedEpisodeRepository: WatchedEpisodeRepository,
    private val watchingShowRepository: WatchingShowRepository
): ViewModel() {

    private val fetchDetailsMoviesMutableLiveData = MutableLiveData<MoviesDetailsDataModel>()
    val fetchDetailsMoviesLiveData: LiveData<MoviesDetailsDataModel> = fetchDetailsMoviesMutableLiveData

    private val fetchYouMayAlsoLikeMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchYouMayAlsoLikeLiveData: LiveData<List<MoviesDataModel>> = fetchYouMayAlsoLikeMutableLiveData

    private val watchedEpisodesMutableLiveData = MutableLiveData<Set<String>>()
    val watchedEpisodesLiveData: LiveData<Set<String>> = watchedEpisodesMutableLiveData

    private val watchedMovieMutableLiveData = MutableLiveData<Boolean>()
    val watchedMovieLiveData: LiveData<Boolean> = watchedMovieMutableLiveData

    private var moviesDetailsDataModel: MoviesDetailsDataModel? = null

    fun fetchDetails(html: String) = launch {
        if (moviesDetailsDataModel == null) {
            moviesDetailsDataModel = parseJsonMovieDetailsRepository.fetchDetails(html)
        }
        val model = moviesDetailsDataModel ?: return@launch

        val seasons = model.seasonsList
        if (!seasons.isNullOrEmpty() && seasons.firstOrNull { it.isSelected } == null) {
            // Land on the current-watching season instead of always the last one.
            val watched = watchedEpisodeRepository.watchedLinksForShow(model.linkToDetails)
            val selectedIndex = currentWatchingSeasonIndex(seasons, watched)
            seasons.onEach { it.isSelected = false }
            seasons.getOrNull(selectedIndex)?.isSelected = true

            val episodes = seasons.getOrNull(selectedIndex)?.episodes
            if (!episodes.isNullOrEmpty()) {
                episodes.onEach { it.isSelected = false }
                episodes.firstOrNull()?.isSelected = true
            }
        }

        fetchDetailsMoviesMutableLiveData.postValue(model)
        refreshWatchedEpisodes(model.linkToDetails)
        watchedMovieMutableLiveData.postValue(watchedRepository.isWatched(model.linkToDetails))
    }

    /**
     * Mirrors iOS `currentWatchingSeason`: no episodes watched → season 0; the furthest-watched
     * season if it still has unwatched episodes; otherwise the next season.
     */
    private fun currentWatchingSeasonIndex(seasons: List<MovieSeasonDataModel>, watched: Set<String>): Int {
        if (watched.isEmpty()) return 0
        var furthest = -1
        seasons.forEachIndexed { index, season ->
            if (season.episodes.any { watched.contains(it.link) }) furthest = index
        }
        if (furthest == -1) return 0
        val hasUnwatched = seasons[furthest].episodes.any { !watched.contains(it.link) }
        return if (hasUnwatched) furthest else minOf(furthest + 1, seasons.size - 1)
    }

    fun fetchYouMayAlsoLike(url: String) = launch {
        val parsedData = parseJsonYouMayAlsoLikeRepository.fetchYouMayAlsoLike(url)
        fetchYouMayAlsoLikeMutableLiveData.postValue(parsedData)
    }

    fun favorite(movie: MoviesDetailsDataModel) {
        favoriteRepository.favorite(movie)
    }

    fun hasMovie(movie: MoviesDetailsDataModel): Boolean {
        return favoriteRepository.hasMovie(movie)
    }

    fun toggleEpisodeWatched(episode: MovieEpisodesDataModel, seasonNumber: String) = launch {
        val showLink = moviesDetailsDataModel?.linkToDetails ?: return@launch
        if (watchedEpisodeRepository.isWatched(episode.link)) {
            watchedEpisodeRepository.removeWatched(episode.link)
        } else {
            watchedEpisodeRepository.markWatched(episode.link, showLink, seasonNumber, episode.episodeNumber)
        }
        refreshWatchedEpisodes(showLink)
    }

    fun toggleMovieWatched() = launch {
        val model = moviesDetailsDataModel ?: return@launch
        if (watchedRepository.isWatched(model.linkToDetails)) {
            watchedRepository.removeWatched(model.linkToDetails)
            watchedMovieMutableLiveData.postValue(false)
        } else {
            watchedRepository.markWatched(model)
            watchedMovieMutableLiveData.postValue(true)
        }
    }

    private fun refreshWatchedEpisodes(showLink: String) = launch {
        watchedEpisodesMutableLiveData.postValue(watchedEpisodeRepository.watchedLinksForShow(showLink))
    }

    fun getMovieName(): String? {
        return moviesDetailsDataModel?.name
    }

    fun getDetails(): MoviesDetailsDataModel? = moviesDetailsDataModel

    /** Remember this show as "currently watching" so it shows up in Continue Watching. */
    fun rememberWatchingShow() {
        moviesDetailsDataModel?.let { watchingShowRepository.remember(it) }
    }

    fun getSelectedSeason(): MovieSeasonDataModel? {
        return moviesDetailsDataModel?.seasonsList?.firstOrNull { it.isSelected }
    }
}
