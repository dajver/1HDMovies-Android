package com.a1hd.movies.ui.sections.allmovies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.etc.extensions.launch
import com.a1hd.movies.api.repository.FilterOptions
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.api.repository.ParseJsonFilterRepository
import com.a1hd.movies.api.repository.ParseJsonMoviesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AllMoviesViewModel @Inject constructor(
    private val parseJsonMoviesRepository: ParseJsonMoviesRepository,
    private val parseJsonFilterRepository: ParseJsonFilterRepository
): ViewModel() {

    private val fetchMoviesMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchMoviesMutableLiveData

    var currentPage = 1
    private var allMoviesList = emptyList<MoviesDataModel>()

    var filterOptions = FilterOptions()
    val isFiltering: Boolean get() = !filterOptions.isEmpty

    fun fetchMovies() = launch {
        if (allMoviesList.isEmpty()) {
            allMoviesList = parseJsonMoviesRepository.fetchMovies(page = currentPage)
        }
        fetchMoviesMutableLiveData.postValue(allMoviesList)
    }

    fun fetchPaginationMovies() = launch {
        if (isFiltering) {
            val filtered = parseJsonFilterRepository.fetchFiltered(filterOptions, currentPage)
            allMoviesList = filtered
        } else {
            allMoviesList = parseJsonMoviesRepository.fetchMovies(page = currentPage)
        }
        fetchMoviesMutableLiveData.postValue(allMoviesList)
    }

    fun applyFilters(options: FilterOptions) {
        filterOptions = options
        currentPage = 1
        allMoviesList = emptyList()
        launch {
            val results = parseJsonFilterRepository.fetchFiltered(filterOptions, currentPage)
            allMoviesList = results
            fetchMoviesMutableLiveData.postValue(allMoviesList)
        }
    }

    fun resetFilters() {
        filterOptions.reset()
        currentPage = 1
        allMoviesList = emptyList()
        fetchMovies()
    }
}
