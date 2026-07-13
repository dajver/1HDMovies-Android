package com.a1hd.movies.ui.sections.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.etc.extensions.launch
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.api.repository.ParseJsonSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val parseJsonSearchRepository: ParseJsonSearchRepository
) : ViewModel() {

    private val fetchSearchResultMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchSearchResultLiveData: LiveData<List<MoviesDataModel>> = fetchSearchResultMutableLiveData

    private val isLoadingMutableLiveData = MutableLiveData(false)
    val isLoadingLiveData: LiveData<Boolean> = isLoadingMutableLiveData

    fun search(keyword: String) = launch {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) {
            // Blank query — clear results and don't hit the network.
            isLoadingMutableLiveData.postValue(false)
            fetchSearchResultMutableLiveData.postValue(emptyList())
            return@launch
        }
        isLoadingMutableLiveData.postValue(true)
        val searchKeyword = trimmed.replace(" ", "+")
        val searchResult = parseJsonSearchRepository.fetchSearchResult(searchKeyword)
        fetchSearchResultMutableLiveData.postValue(searchResult)
        isLoadingMutableLiveData.postValue(false)
    }
}
