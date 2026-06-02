package com.a1hd.movies.ui.sections.favorite.repository

import android.content.SharedPreferences
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.etc.extensions.mutableList
import com.google.gson.Gson
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val FAVORITE_LIST = "FAVORITE_LIST"

@Singleton
class FavoriteRepository @Inject constructor(prefs: SharedPreferences, gson: Gson) {

    private var favorites: MutableList<MoviesDetailsDataModel> by prefs.mutableList(FAVORITE_LIST, gson, "")

    var onFavoriteAdded: ((MoviesDetailsDataModel) -> Unit)? = null
    var onFavoriteRemoved: ((MoviesDetailsDataModel) -> Unit)? = null

    fun fetchAllFavorites(): MutableList<MoviesDetailsDataModel> {
        return favorites.sortedByDescending { it.addedAt }.toMutableList()
    }

    fun favorite(movie: MoviesDetailsDataModel) {
        if (hasMovie(movie)) {
            remove(movie)
            onFavoriteRemoved?.invoke(movie)
        } else {
            save(movie)
            onFavoriteAdded?.invoke(movie)
        }
    }

    fun hasMovie(movie: MoviesDetailsDataModel): Boolean {
        return favorites.any { it.linkToDetails == movie.linkToDetails }
    }

    fun addWithoutSync(movie: MoviesDetailsDataModel) {
        if (favorites.any { it.linkToDetails == movie.linkToDetails }) return
        val favoritesList = mutableListOf<MoviesDetailsDataModel>()
        favoritesList.addAll(favorites)
        if (movie.addedAt == null) {
            movie.addedAt = Date().time
        }
        favoritesList.add(movie)
        favorites = favoritesList
    }

    private fun save(movie: MoviesDetailsDataModel) {
        val favoritesList = mutableListOf<MoviesDetailsDataModel>()
        favoritesList.addAll(favorites)
        movie.addedAt = Date().time
        favoritesList.add(movie)
        favorites = favoritesList
    }

    private fun remove(movie: MoviesDetailsDataModel) {
        val favoritesList = mutableListOf<MoviesDetailsDataModel>()
        favoritesList.addAll(favorites)
        favoritesList.removeAll { it.linkToDetails == movie.linkToDetails }
        favorites = favoritesList
    }
}
