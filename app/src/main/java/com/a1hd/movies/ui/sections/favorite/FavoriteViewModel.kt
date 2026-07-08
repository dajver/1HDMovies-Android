package com.a1hd.movies.ui.sections.favorite

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
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val watchedRepository: WatchedRepository
): ViewModel() {

    private val fetchFavoritesMutableLiveData = MutableLiveData<MutableList<MoviesDetailsDataModel>>()
    val fetchFavoritesLiveData: LiveData<MutableList<MoviesDetailsDataModel>> = fetchFavoritesMutableLiveData

    fun fetchAllFavorites() = launch {
        val watched = watchedRepository.watchedLinks()
        // The Favorites screen hides show-level-watched items; they live on the Watched screen.
        val favorites = favoriteRepository.fetchAllFavorites()
            .filterNot { watched.contains(it.linkToDetails) }
            .toMutableList()
        fetchFavoritesMutableLiveData.postValue(favorites)
    }
}