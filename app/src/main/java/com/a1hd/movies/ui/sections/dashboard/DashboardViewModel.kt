package com.a1hd.movies.ui.sections.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.api.repository.GenresEnum
import com.a1hd.movies.etc.extensions.launch
import com.a1hd.movies.api.repository.MostPopularMoviesDataModel
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.api.repository.ParseJsonDashboardRepository
import com.a1hd.movies.api.repository.ParseJsonMostPopularRepository
import com.a1hd.movies.api.repository.ParseJsonMoviesGenresRepository
import com.a1hd.movies.api.repository.ParseJsonMoviesRepository
import com.a1hd.movies.api.repository.ParseJsonTvShowsRepository
import com.a1hd.movies.db.repository.ContinueWatchingItem
import com.a1hd.movies.db.repository.ContinueWatchingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val parseJsonDashboardRepository: ParseJsonDashboardRepository,
    private val parseJsonMoviesRepository: ParseJsonMoviesRepository,
    private val parseJsonTvShowsRepository: ParseJsonTvShowsRepository,
    private val parseJsonMostPopularRepository: ParseJsonMostPopularRepository,
    private val parseJsonMoviesGenresRepository: ParseJsonMoviesGenresRepository,
    private val continueWatchingRepository: ContinueWatchingRepository
): ViewModel() {

    private val fetchContinueWatchingMutableLiveData = MutableLiveData<List<ContinueWatchingItem>>()
    val fetchContinueWatchingLiveData: LiveData<List<ContinueWatchingItem>> = fetchContinueWatchingMutableLiveData

    private val fetchMostPopularMutableLiveData = MutableLiveData<List<MostPopularMoviesDataModel>>()
    val fetchMostPopularLiveData: LiveData<List<MostPopularMoviesDataModel>> = fetchMostPopularMutableLiveData

    private val fetchDashboardMoviesMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchDashboardMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchDashboardMoviesMutableLiveData

    private val fetchMoviesMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchMoviesMutableLiveData

    private val fetchTvShowsMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchTvShowsLiveData: LiveData<List<MoviesDataModel>> = fetchTvShowsMutableLiveData

    private val fetchActionMoviesMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchActionMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchActionMoviesMutableLiveData

    private val fetchComedyMoviesMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchComedyMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchComedyMoviesMutableLiveData

    private val fetchDramaMoviesMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchDramaMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchDramaMoviesMutableLiveData

    private val fetchFantasyMoviesMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchFantasyMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchFantasyMoviesMutableLiveData

    private val fetchFantasyHorrorMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchHorrorMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchFantasyHorrorMutableLiveData

    private val fetchFantasyMysteryMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchMysteryMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchFantasyMysteryMutableLiveData

    private val fetchTopIMDBMutableLiveData = MutableLiveData<List<MoviesDataModel>>()
    val fetchTopIMDBMoviesLiveData: LiveData<List<MoviesDataModel>> = fetchTopIMDBMutableLiveData

    private var mostPopularMovies = emptyList<MostPopularMoviesDataModel>()
    private var dashboardMovies = emptyList<MoviesDataModel>()
    private var allMoviesList = emptyList<MoviesDataModel>()
    private var allTvShowsList = emptyList<MoviesDataModel>()
    private var allActionMoviesList = emptyList<MoviesDataModel>()
    private var allComedyMoviesList = emptyList<MoviesDataModel>()
    private var allDramaMoviesList = emptyList<MoviesDataModel>()
    private var allFantasyMoviesList = emptyList<MoviesDataModel>()
    private var allHorrorMoviesList = emptyList<MoviesDataModel>()
    private var allMysteryMoviesList = emptyList<MoviesDataModel>()
    private var allTopIMDBMoviesList = emptyList<MoviesDataModel>()

    fun fetchContinueWatching() = launch {
        fetchContinueWatchingMutableLiveData.postValue(continueWatchingRepository.build())
    }

    fun removeFromContinueWatching(item: ContinueWatchingItem) = launch {
        continueWatchingRepository.remove(item)
        fetchContinueWatchingMutableLiveData.postValue(continueWatchingRepository.build())
    }

    fun fetchMostPopular() = launch {
        if (mostPopularMovies.isEmpty()) {
            mostPopularMovies = parseJsonMostPopularRepository.fetchMostPopular()
        }
        fetchMostPopularMutableLiveData.postValue(mostPopularMovies)
    }

    fun fetchDashboard() = launch {
        if (dashboardMovies.isEmpty()) {
            dashboardMovies = parseJsonDashboardRepository.fetchDashboard()
        }
        fetchDashboardMoviesMutableLiveData.postValue(dashboardMovies)
    }

    fun fetchMovies() = launch {
        if (allMoviesList.isEmpty()) {
            allMoviesList = parseJsonMoviesRepository.fetchMovies(page = 1)
        }
        fetchMoviesMutableLiveData.postValue(allMoviesList)
    }

    fun fetchTvShows() = launch {
        if (allTvShowsList.isEmpty()) {
            allTvShowsList = parseJsonTvShowsRepository.fetchTvShows(page = 1)
        }
        fetchTvShowsMutableLiveData.postValue(allTvShowsList)
    }

    fun fetchActionMovies() = launch {
        if (allActionMoviesList.isEmpty()) {
            allActionMoviesList =  parseJsonMoviesGenresRepository.fetchMoviesByGenre(genre = GenresEnum.ACTION, page = 1)
        }
        fetchActionMoviesMutableLiveData.postValue(allActionMoviesList)
    }

    fun fetchComedyMovies() = launch {
        if (allComedyMoviesList.isEmpty()) {
            allComedyMoviesList =  parseJsonMoviesGenresRepository.fetchMoviesByGenre(genre = GenresEnum.COMEDY, page = 1)
        }
        fetchComedyMoviesMutableLiveData.postValue(allComedyMoviesList)
    }

    fun fetchDramaMovies() = launch {
        if (allDramaMoviesList.isEmpty()) {
            allDramaMoviesList =  parseJsonMoviesGenresRepository.fetchMoviesByGenre(genre = GenresEnum.DRAMA, page = 1)
        }
        fetchDramaMoviesMutableLiveData.postValue(allDramaMoviesList)
    }

    fun fetchFantasyMovies() = launch {
        if (allFantasyMoviesList.isEmpty()) {
            allFantasyMoviesList =  parseJsonMoviesGenresRepository.fetchMoviesByGenre(genre = GenresEnum.FANTASY, page = 1)
        }
        fetchFantasyMoviesMutableLiveData.postValue(allFantasyMoviesList)
    }

    fun fetchHorrorMovies() = launch {
        if (allHorrorMoviesList.isEmpty()) {
            allHorrorMoviesList =  parseJsonMoviesGenresRepository.fetchMoviesByGenre(genre = GenresEnum.HORROR, page = 1)
        }
        fetchFantasyHorrorMutableLiveData.postValue(allHorrorMoviesList)
    }

    fun fetchMysteryMovies() = launch {
        if (allMysteryMoviesList.isEmpty()) {
            allMysteryMoviesList =  parseJsonMoviesGenresRepository.fetchMoviesByGenre(genre = GenresEnum.MYSTERY, page = 1)
        }
        fetchFantasyMysteryMutableLiveData.postValue(allMysteryMoviesList)
    }

    fun fetchTopIMDBMovies() = launch {
        if (allTopIMDBMoviesList.isEmpty()) {
            allTopIMDBMoviesList =  parseJsonMoviesGenresRepository.fetchMoviesByGenre(genre = GenresEnum.TOP_IMDB, page = 1)
        }
        fetchTopIMDBMutableLiveData.postValue(allTopIMDBMoviesList)
    }
}