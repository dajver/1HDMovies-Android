package com.a1hd.movies.ui.sections.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.api.repository.FilterOptions
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.api.repository.ParseJsonFilterRepository
import com.a1hd.movies.etc.extensions.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val filterRepository: ParseJsonFilterRepository
) : ViewModel() {

    private val resultsMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val resultsLiveData: LiveData<List<MoviesDataModel>> = resultsMutableLiveData

    var filterOptions = FilterOptions()
    var hasSearched = false
    var currentPage = 1
    private var canLoadMore = true
    private var isLoading = false

    fun applyFilters(options: FilterOptions) {
        filterOptions = options
        hasSearched = true
        currentPage = 1
        canLoadMore = true
        launch {
            isLoading = true
            val results = filterRepository.fetchFiltered(filterOptions, 1)
            canLoadMore = results.isNotEmpty()
            currentPage = 2
            isLoading = false
            resultsMutableLiveData.postValue(results)
        }
    }

    fun loadMore() {
        if (isLoading || !canLoadMore) return
        launch {
            isLoading = true
            val results = filterRepository.fetchFiltered(filterOptions, currentPage)
            canLoadMore = results.isNotEmpty()
            currentPage++
            isLoading = false
            val current = resultsMutableLiveData.value?.toMutableList() ?: mutableListOf()
            current.addAll(results)
            resultsMutableLiveData.postValue(current)
        }
    }
}
