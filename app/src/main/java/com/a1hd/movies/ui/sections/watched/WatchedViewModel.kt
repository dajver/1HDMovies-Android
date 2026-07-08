package com.a1hd.movies.ui.sections.watched

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.db.repository.WatchedRepository
import com.a1hd.movies.etc.extensions.launch
import com.a1hd.movies.ui.sections.favorite.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WatchedViewModel @Inject constructor(
    private val watchedRepository: WatchedRepository,
    private val favoriteRepository: FavoriteRepository
): ViewModel() {

    private val watchedMutableLiveData = MutableLiveData<MutableList<MoviesDetailsDataModel>>()
    val watchedLiveData: LiveData<MutableList<MoviesDetailsDataModel>> = watchedMutableLiveData

    /**
     * Mirrors iOS: the Watched screen is the set of favorites that are marked show-level watched.
     * (The `watched` records only store the linkToDetails; the name/thumbnail come from the favorite.)
     */
    fun fetchAllWatched() = launch {
        val watchedLinks = watchedRepository.watchedLinks()
        val items = favoriteRepository.fetchAllFavorites()
            .filter { watchedLinks.contains(it.linkToDetails) }
            .toMutableList()
        watchedMutableLiveData.postValue(items)
    }
}
