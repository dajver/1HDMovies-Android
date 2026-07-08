package com.a1hd.movies.ui.navigation.route

import androidx.fragment.app.Fragment
import com.a1hd.movies.api.repository.GenresEnum
import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.ui.sections.dashboard.DashboardFragment
import com.a1hd.movies.ui.sections.SplashFragment
import com.a1hd.movies.ui.sections.account.AccountFragment
import com.a1hd.movies.ui.sections.allmovies.AllMoviesFragment
import com.a1hd.movies.ui.sections.filter.FilterFragment
import com.a1hd.movies.ui.sections.alltvshows.AllTvShowsFragment
import com.a1hd.movies.ui.sections.favorite.FavoriteFragment
import com.a1hd.movies.ui.sections.genre.MovieByGenreFragment
import com.a1hd.movies.ui.sections.movie.MovieDetailsFragment
import com.a1hd.movies.ui.sections.movie.watch.WatchMovieFragment
import com.a1hd.movies.ui.sections.notifications.NotificationsFragment
import com.a1hd.movies.ui.sections.search.SearchFragment
import com.a1hd.movies.ui.sections.watched.WatchedFragment

sealed class Router(val clearStack: Boolean = false) {
    object Splash : Router(clearStack = true)
    object Dashboard : Router(clearStack = true)
    class MovieDetails(val movieUrl: String?) : Router()
    class WatchMovie(
        val movieUrl: String?,
        val title: String? = null,
        val episodes: ArrayList<MovieEpisodesDataModel>? = null,
        val episodeIndex: Int = 0,
        val showLink: String? = null,
        val thumbnail: String? = null,
        val contentType: String? = null,
        val seasonNumber: String? = null
    ) : Router()
    object AllMovies : Router()
    object AllTvShows : Router()
    class MovieByGenre(val movieGenre: GenresEnum?) : Router()
    class Tag(val title: String, val url: String) : Router()
    object Search : Router()
    object Favorites : Router()
    object Watched : Router()
    object Notifications : Router()
    object Account : Router()
    object Filter : Router()
}

fun Router.toFragment(): Fragment {
    return when (this) {
        Router.Splash -> SplashFragment()
        Router.Dashboard -> DashboardFragment()
        is Router.MovieDetails -> MovieDetailsFragment.newInstance(movieUrl)
        is Router.WatchMovie -> WatchMovieFragment.newInstance(movieUrl, title, episodes, episodeIndex, showLink, thumbnail, contentType, seasonNumber)
        Router.AllMovies -> AllMoviesFragment()
        Router.AllTvShows -> AllTvShowsFragment()
        is Router.MovieByGenre -> MovieByGenreFragment.newInstance(movieGenre)
        is Router.Tag -> MovieByGenreFragment.newInstanceForTag(title, url)
        Router.Search -> SearchFragment()
        Router.Favorites -> FavoriteFragment()
        Router.Watched -> WatchedFragment()
        Router.Notifications -> NotificationsFragment()
        Router.Account -> AccountFragment()
        Router.Filter -> FilterFragment()
    }
}

fun Fragment.toRouter(): Router {
    return when (this) {
        is SplashFragment -> Router.Splash
        is DashboardFragment -> Router.Dashboard
        is MovieDetailsFragment -> Router.MovieDetails(movieUrl)
        is WatchMovieFragment -> Router.WatchMovie(movieUrl, movieTitle, episodes, currentEpisodeIndex, showLink, thumbnail, contentType, seasonNumber)
        is AllMoviesFragment -> Router.AllMovies
        is AllTvShowsFragment -> Router.AllTvShows
        is MovieByGenreFragment -> if (movieGenre != null) Router.MovieByGenre(movieGenre) else Router.Tag(tagTitle ?: "", tagUrl ?: "")
        is SearchFragment -> Router.Search
        is FavoriteFragment -> Router.Favorites
        is WatchedFragment -> Router.Watched
        is NotificationsFragment -> Router.Notifications
        is AccountFragment -> Router.Account
        is FilterFragment -> Router.Filter
        else -> throw RuntimeException("Not found such fragment in router $this")
    }
}