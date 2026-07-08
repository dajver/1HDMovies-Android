package com.a1hd.movies.ui.sections.genre

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.api.repository.GenresEnum
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.api.repository.ParseJsonMoviesGenresRepository
import com.a1hd.movies.etc.extensions.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MovieByGenreViewModel @Inject constructor(
    private val parseJsonMoviesGenresRepository: ParseJsonMoviesGenresRepository
) : ViewModel() {

    private val fetchMoviesGenreMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchMoviesGenreLiveData: LiveData<List<MoviesDataModel>> = fetchMoviesGenreMutableLiveData

    var currentPage = 1
    private var moviesGenreList = emptyList<MoviesDataModel>()

    fun fetchMoviesByUrl(url: String) = launch {
        if (moviesGenreList.isEmpty()) {
            moviesGenreList = parseJsonMoviesGenresRepository.fetchMoviesByUrl(url, page = currentPage)
        }
        fetchMoviesGenreMutableLiveData.postValue(moviesGenreList)
    }

    fun fetchPaginationMoviesByUrl(url: String) = launch {
        moviesGenreList = parseJsonMoviesGenresRepository.fetchMoviesByUrl(url, page = currentPage)
        fetchMoviesGenreMutableLiveData.postValue(moviesGenreList)
    }
}