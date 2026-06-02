package com.a1hd.movies.ui.sections.alltvshows

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.etc.extensions.launch
import com.a1hd.movies.api.repository.models.FilterOptions
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.api.repository.ParseJsonFilterRepository
import com.a1hd.movies.api.repository.ParseJsonTvShowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AllTvShowsViewModel @Inject constructor(
    private val parseJsonTvShowsRepository: ParseJsonTvShowsRepository,
    private val parseJsonFilterRepository: ParseJsonFilterRepository
): ViewModel() {

    private val fetchTvShowsMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchTvShowsLiveData: LiveData<List<MoviesDataModel>> = fetchTvShowsMutableLiveData

    var currentPage = 1
    private var allTvShowsList = emptyList<MoviesDataModel>()

    var filterOptions = FilterOptions()
    val isFiltering: Boolean get() = !filterOptions.isEmpty

    fun fetchTvShows() = launch {
        if (allTvShowsList.isEmpty()) {
            allTvShowsList = parseJsonTvShowsRepository.fetchTvShows(page = currentPage)
        }
        fetchTvShowsMutableLiveData.postValue(allTvShowsList)
    }

    fun fetchPaginationTvShows() = launch {
        if (isFiltering) {
            val filtered = parseJsonFilterRepository.fetchFiltered(filterOptions, currentPage)
            allTvShowsList = filtered
        } else {
            allTvShowsList = parseJsonTvShowsRepository.fetchTvShows(page = currentPage)
        }
        fetchTvShowsMutableLiveData.postValue(allTvShowsList)
    }

    fun applyFilters(options: FilterOptions) {
        filterOptions = options
        currentPage = 1
        allTvShowsList = emptyList()
        launch {
            val results = parseJsonFilterRepository.fetchFiltered(filterOptions, currentPage)
            allTvShowsList = results
            fetchTvShowsMutableLiveData.postValue(allTvShowsList)
        }
    }

    fun resetFilters() {
        filterOptions.reset()
        currentPage = 1
        allTvShowsList = emptyList()
        fetchTvShows()
    }
}
