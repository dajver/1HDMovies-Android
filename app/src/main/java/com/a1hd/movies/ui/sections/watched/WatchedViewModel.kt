package com.a1hd.movies.ui.sections.watched

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.api.repository.MovieType
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.db.entity.WatchedMovieEntity
import com.a1hd.movies.db.repository.WatchedRepository
import com.a1hd.movies.etc.extensions.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WatchedViewModel @Inject constructor(
    private val watchedRepository: WatchedRepository
): ViewModel() {

    private val watchedMutableLiveData = MutableLiveData<MutableList<MoviesDetailsDataModel>>()
    val watchedLiveData: LiveData<MutableList<MoviesDetailsDataModel>> = watchedMutableLiveData

    fun fetchAllWatched() = launch {
        val items = watchedRepository.getAll()
            .sortedByDescending { it.updatedAt }
            .map { it.toDetailsModel() }
            .toMutableList()
        watchedMutableLiveData.postValue(items)
    }

    private fun WatchedMovieEntity.toDetailsModel(): MoviesDetailsDataModel {
        val movieType = runCatching { MovieType.valueOf(type) }.getOrDefault(MovieType.MOVIE)
        return MoviesDetailsDataModel(
            name = name,
            thumbnail = thumbnail,
            linkToWatch = "",
            linkToDetails = linkToDetails,
            watchMovieLinkWithEpisodeId = "",
            type = movieType,
            description = "",
            quality = "",
            cast = "",
            genre = "",
            duration = "",
            country = "",
            imdb = "",
            release = "",
            production = ""
        )
    }
}
