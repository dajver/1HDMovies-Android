package com.a1hd.movies.db.repository

import android.content.SharedPreferences
import com.a1hd.movies.api.repository.MovieType
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.etc.extensions.mutableList
import com.google.gson.Gson
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val WATCHING_SHOWS = "WATCHING_SHOWS"
private const val MAX_WATCHING_SHOWS = 30

/**
 * Remembers TV shows the user has started watching (even if not favorited) so they can appear
 * in the Continue Watching row. Stores the full details model (with seasons/episodes) so the
 * "next episode" card can be built without a network fetch. Mirrors the FavoriteRepository style.
 */
@Singleton
class WatchingShowRepository @Inject constructor(prefs: SharedPreferences, gson: Gson) {

    private var shows: MutableList<MoviesDetailsDataModel> by prefs.mutableList(WATCHING_SHOWS, gson, "")

    /** Records (or refreshes) a show as "currently watching". No-op for movies / empty season lists. */
    fun remember(show: MoviesDetailsDataModel) {
        if (show.type != MovieType.TV_SHOW) return
        if (show.seasonsList.isNullOrEmpty()) return
        val list = shows.filterNot { it.linkToDetails == show.linkToDetails }.toMutableList()
        show.addedAt = Date().time
        list.add(0, show)
        shows = list.take(MAX_WATCHING_SHOWS).toMutableList()
    }

    fun getAll(): List<MoviesDetailsDataModel> = shows

    fun remove(linkToDetails: String) {
        shows = shows.filterNot { it.linkToDetails == linkToDetails }.toMutableList()
    }
}
