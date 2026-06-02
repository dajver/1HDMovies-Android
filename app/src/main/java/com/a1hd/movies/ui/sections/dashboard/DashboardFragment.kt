package com.a1hd.movies.ui.sections.dashboard

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.a1hd.movies.R
import com.a1hd.movies.api.repository.GenresEnum
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.databinding.FragmentDashboardBinding
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.api.repository.MovieType
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.ui.sections.dashboard.adapter.DashboardRecyclerAdapter
import com.a1hd.movies.ui.sections.dashboard.viewpager.ViewPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val NONE_LINK_TO_DETAILS = "NONE"

@AndroidEntryPoint
class DashboardFragment: BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    @Inject
    lateinit var topMoviesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var topTvShowsRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var moviesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var tvShowsRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var actionMoviesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var comedyMoviesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var dramaMoviesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var fantasyMoviesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var horrorMoviesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var mysteryMoviesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var topImdbMoviesRecyclerAdapter: DashboardRecyclerAdapter
    
    private val dashboardViewModel: DashboardViewModel by viewModels()

    private val viewPagerAdapter: ViewPagerAdapter by lazy {
        ViewPagerAdapter(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        topTvShowsRecyclerAdapter.onMovieClickListener = onMovieClickListener
        moviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        tvShowsRecyclerAdapter.onMovieClickListener = onMovieClickListener
        actionMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        comedyMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        dramaMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        fantasyMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        horrorMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        mysteryMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        topImdbMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        viewPagerAdapter.onMostPopularMovieClickListener = {
            navigationRouter.navigateTo(Router.WatchMovie(it.link))
        }
        binding.rvTopMovies.adapter = topMoviesRecyclerAdapter
        binding.rvTopTvShows.adapter = topTvShowsRecyclerAdapter
        binding.rvMovies.adapter = moviesRecyclerAdapter
        binding.rvTvShows.adapter = tvShowsRecyclerAdapter
        binding.rvAction.adapter = actionMoviesRecyclerAdapter
        binding.rvComedy.adapter = comedyMoviesRecyclerAdapter
        binding.rvDrama.adapter = dramaMoviesRecyclerAdapter
        binding.rvFantasy.adapter = fantasyMoviesRecyclerAdapter
        binding.rvHorror.adapter = horrorMoviesRecyclerAdapter
        binding.rvMystery.adapter = mysteryMoviesRecyclerAdapter
        binding.rvTopIMDB.adapter = topImdbMoviesRecyclerAdapter
        binding.viewPagerMain.adapter = viewPagerAdapter

        dashboardViewModel.fetchMostPopular()
        dashboardViewModel.fetchMostPopularLiveData.observe(viewLifecycleOwner) {
            binding.pbProgressMostPopular.isVisible = false
            viewPagerAdapter.setImages(it.toMutableList())
        }

        dashboardViewModel.fetchDashboard()
        dashboardViewModel.fetchDashboardMoviesLiveData.observe(viewLifecycleOwner) {
            binding.pbProgress.isVisible = false
            binding.llMenu.isVisible = true
            binding.nsMoviesList.isVisible = true

            val topMovies = it.filter { it.type == MovieType.MOVIE }.toMutableList()
            topMoviesRecyclerAdapter.setMovies(topMovies)
            val topTvShows = it.filter { it.type == MovieType.TV_SHOW }.toMutableList()
            topTvShowsRecyclerAdapter.setMovies(topTvShows)

            binding.tvTopMovies.isVisible = topMovies.isNotEmpty()
            binding.rvTopMovies.isVisible = topMovies.isNotEmpty()
            binding.tvTopTvShows.isVisible = topTvShows.isNotEmpty()
            binding.rvTopTvShows.isVisible = topTvShows.isNotEmpty()
        }

        dashboardViewModel.fetchMovies()
        dashboardViewModel.fetchMoviesLiveData.observe(viewLifecycleOwner) {
            val moviesList = it.toMutableList()
            moviesList.add(moviesList.size, allMoviesPlaceHolder())
            moviesRecyclerAdapter.setMovies(moviesList)
        }

        dashboardViewModel.fetchTvShows()
        dashboardViewModel.fetchTvShowsLiveData.observe(viewLifecycleOwner) {
            val tvShowsList = it.toMutableList()
            tvShowsList.add(tvShowsList.size, allTvShowsPlaceHolder())
            tvShowsRecyclerAdapter.setMovies(tvShowsList)
        }

        dashboardViewModel.fetchActionMovies()
        dashboardViewModel.fetchActionMoviesLiveData.observe(viewLifecycleOwner) {
            val actionMovies = it.toMutableList()
            actionMovies.add(actionMovies.size, genreMoviesPlaceHolder(getString(R.string.action), GenresEnum.ACTION))
            actionMoviesRecyclerAdapter.setMovies(actionMovies)
        }

        dashboardViewModel.fetchComedyMovies()
        dashboardViewModel.fetchComedyMoviesLiveData.observe(viewLifecycleOwner) {
            val comedyMovies = it.toMutableList()
            comedyMovies.add(comedyMovies.size, genreMoviesPlaceHolder(getString(R.string.comedy), GenresEnum.COMEDY))
            comedyMoviesRecyclerAdapter.setMovies(comedyMovies)
        }

        dashboardViewModel.fetchDramaMovies()
        dashboardViewModel.fetchDramaMoviesLiveData.observe(viewLifecycleOwner) {
            val dramaMovies = it.toMutableList()
            dramaMovies.add(dramaMovies.size, genreMoviesPlaceHolder(getString(R.string.drama), GenresEnum.DRAMA))
            dramaMoviesRecyclerAdapter.setMovies(dramaMovies)
        }

        dashboardViewModel.fetchFantasyMovies()
        dashboardViewModel.fetchFantasyMoviesLiveData.observe(viewLifecycleOwner) {
            val fantasyMovies = it.toMutableList()
            fantasyMovies.add(fantasyMovies.size, genreMoviesPlaceHolder(getString(R.string.fantasy), GenresEnum.FANTASY))
            fantasyMoviesRecyclerAdapter.setMovies(fantasyMovies)
        }

        dashboardViewModel.fetchHorrorMovies()
        dashboardViewModel.fetchHorrorMoviesLiveData.observe(viewLifecycleOwner) {
            val horrorMovies = it.toMutableList()
            horrorMovies.add(horrorMovies.size, genreMoviesPlaceHolder(getString(R.string.horror), GenresEnum.HORROR))
            horrorMoviesRecyclerAdapter.setMovies(horrorMovies)
        }

        dashboardViewModel.fetchMysteryMovies()
        dashboardViewModel.fetchMysteryMoviesLiveData.observe(viewLifecycleOwner) {
            val mysteryMovies = it.toMutableList()
            mysteryMovies.add(mysteryMovies.size, genreMoviesPlaceHolder(getString(R.string.mystery), GenresEnum.MYSTERY))
            mysteryMoviesRecyclerAdapter.setMovies(mysteryMovies)
        }

        dashboardViewModel.fetchTopIMDBMovies()
        dashboardViewModel.fetchTopIMDBMoviesLiveData.observe(viewLifecycleOwner) {
            val topIMDBMovies = it.toMutableList()
            topIMDBMovies.add(topIMDBMovies.size, genreMoviesPlaceHolder(getString(R.string.top_imdb), GenresEnum.TOP_IMDB))
            topImdbMoviesRecyclerAdapter.setMovies(topIMDBMovies)
        }

        setupListeners()
    }

    private val onMovieClickListener: (MoviesDataModel) -> Unit = {
        if (it.link == NONE_LINK_TO_DETAILS) {
            if (it.type == MovieType.MOVIE) {
                if (it.genre != null) {
                    navigationRouter.navigateTo(Router.MovieByGenre(it.genre))
                } else {
                    navigationRouter.navigateTo(Router.AllMovies)
                }
            } else if (it.type == MovieType.TV_SHOW) {
                navigationRouter.navigateTo(Router.AllTvShows)
            }
        } else {
            navigationRouter.navigateTo(Router.MovieDetails(it.link))
        }
    }

    private fun setupListeners() {
        binding.btnHome.requestFocus()
        binding.btnSearch.setOnClickListener {
            navigationRouter.navigateTo(Router.Search)
        }

        binding.btnFilter.setOnClickListener {
            navigationRouter.navigateTo(Router.Filter)
        }

        binding.btnFavorites.setOnClickListener {
            navigationRouter.navigateTo(Router.Favorites)
        }

        binding.btnAccount.setOnClickListener {
            navigationRouter.navigateTo(Router.Account)
        }
    }

    private fun allMoviesPlaceHolder(): MoviesDataModel {
        val quality = ""
        val thumbnail = ""
        val episodes = ""
        return MoviesDataModel(
            getString(R.string.all_movies),
            thumbnail,
            NONE_LINK_TO_DETAILS,
            MovieType.MOVIE,
            quality,
            episodes
        )
    }

    private fun allTvShowsPlaceHolder(): MoviesDataModel {
        val quality = ""
        val thumbnail = ""
        val episodes = ""
        return MoviesDataModel(
            getString(R.string.all_tv_shows),
            thumbnail,
            NONE_LINK_TO_DETAILS,
            MovieType.TV_SHOW,
            quality,
            episodes
        )
    }

    private fun genreMoviesPlaceHolder(genreName: String, genre: GenresEnum): MoviesDataModel {
        val quality = ""
        val thumbnail = ""
        val episodes = ""
        return MoviesDataModel(
            genreName,
            thumbnail,
            NONE_LINK_TO_DETAILS,
            MovieType.MOVIE,
            quality,
            episodes
        ).apply {
            this.genre = genre
        }
    }
}