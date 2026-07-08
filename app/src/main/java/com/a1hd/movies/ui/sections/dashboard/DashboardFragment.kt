package com.a1hd.movies.ui.sections.dashboard

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.a1hd.movies.R
import com.a1hd.movies.api.repository.GenresEnum
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.databinding.FragmentDashboardBinding
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.api.repository.MovieType
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.db.repository.ContinueWatchingItem
import com.a1hd.movies.ui.sections.dashboard.adapter.ContinueWatchingRecyclerAdapter
import com.a1hd.movies.ui.sections.dashboard.adapter.DashboardRecyclerAdapter
import com.a1hd.movies.ui.sections.dashboard.viewpager.ViewPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val NONE_LINK_TO_DETAILS = "NONE"

@AndroidEntryPoint
class DashboardFragment: BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    @Inject
    lateinit var continueWatchingRecyclerAdapter: ContinueWatchingRecyclerAdapter

    @Inject
    lateinit var newReleasesRecyclerAdapter: DashboardRecyclerAdapter

    @Inject
    lateinit var animationMoviesRecyclerAdapter: DashboardRecyclerAdapter

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
        continueWatchingRecyclerAdapter.onItemClickListener = { item -> openContinueWatching(item) }
        continueWatchingRecyclerAdapter.onItemLongClickListener = { item -> showContinueWatchingMenu(item) }
        newReleasesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        animationMoviesRecyclerAdapter.onMovieClickListener = onMovieClickListener
        binding.rvContinueWatching.adapter = continueWatchingRecyclerAdapter
        binding.rvNewReleases.adapter = newReleasesRecyclerAdapter
        binding.rvAnimation.adapter = animationMoviesRecyclerAdapter
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

        dashboardViewModel.fetchContinueWatching()
        dashboardViewModel.fetchContinueWatchingLiveData.observe(viewLifecycleOwner) { items ->
            continueWatchingRecyclerAdapter.setItems(items.toMutableList())
            binding.tvContinueWatching.isVisible = items.isNotEmpty()
            binding.rvContinueWatching.isVisible = items.isNotEmpty()
        }

        dashboardViewModel.fetchMostPopular()
        dashboardViewModel.fetchMostPopularLiveData.observe(viewLifecycleOwner) {
            binding.pbProgressMostPopular.isVisible = false
            viewPagerAdapter.setImages(it.toMutableList())
            setupSlideDots(it.size)
            // New Releases row: same content as the slider, but cards open details.
            newReleasesRecyclerAdapter.setMovies(it.map { mp -> mp.toCard() }.toMutableList())
        }

        binding.btnNotifications.setOnClickListener {
            navigationRouter.navigateTo(Router.Notifications)
        }
        dashboardViewModel.unreadCountLiveData.observe(viewLifecycleOwner) { count ->
            binding.tvNotificationBadge.isVisible = count > 0
            binding.tvNotificationBadge.text = if (count > 99) "99+" else count.toString()
        }
        dashboardViewModel.fetchUnreadNotifications()

        dashboardViewModel.fetchAnimationMovies()
        dashboardViewModel.fetchAnimationMoviesLiveData.observe(viewLifecycleOwner) {
            val animationMovies = it.toMutableList()
            animationMovies.add(animationMovies.size, genreMoviesPlaceHolder(getString(R.string.animation), GenresEnum.ANIMATION))
            animationMoviesRecyclerAdapter.setMovies(animationMovies)
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

    override fun onResume() {
        super.onResume()
        // Refresh Continue Watching + unread badge when returning from other screens.
        if (view != null) {
            dashboardViewModel.fetchContinueWatching()
            dashboardViewModel.fetchUnreadNotifications()
        }
    }

    private fun com.a1hd.movies.api.repository.MostPopularMoviesDataModel.toCard(): MoviesDataModel {
        val parts = quality.split(",").map { it.trim() }
        val typeStr = parts.getOrElse(1) { "" }
        val type = if (typeStr.equals("Movie", ignoreCase = true)) MovieType.MOVIE else MovieType.TV_SHOW
        return MoviesDataModel(
            name,
            thumbnail,
            link,
            type,
            parts.getOrElse(0) { "" },
            parts.getOrElse(2) { "" }
        )
    }

    private fun openContinueWatching(item: ContinueWatchingItem) {
        navigationRouter.navigateTo(
            Router.WatchMovie(
                movieUrl = item.watchUrl,
                title = item.title,
                episodes = item.episodes,
                episodeIndex = item.episodeIndex,
                showLink = item.showLink,
                thumbnail = item.thumbnail,
                contentType = item.contentType,
                seasonNumber = item.seasonNumber
            )
        )
    }

    private fun showContinueWatchingMenu(item: ContinueWatchingItem) {
        val open = getString(R.string.open_details)
        val remove = getString(R.string.remove_from_continue_watching)
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(item.title)
            .setItems(arrayOf(open, remove)) { _, which ->
                when (which) {
                    0 -> navigationRouter.navigateTo(Router.MovieDetails(item.id))
                    1 -> dashboardViewModel.removeFromContinueWatching(item)
                }
            }
            .show()
    }

    /** iOS-style pagination dots under the hero slider. */
    private fun setupSlideDots(count: Int) {
        binding.llSlideDots.removeAllViews()
        if (count <= 1) return
        val density = resources.displayMetrics.density
        val size = (8 * density).toInt()
        val margin = (4 * density).toInt()
        val dots = ArrayList<ImageView>()
        for (i in 0 until count) {
            val dot = ImageView(requireContext())
            dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = margin
                marginEnd = margin
            }
            dot.setImageResource(
                if (i == binding.viewPagerMain.currentItem) R.drawable.slide_dot_active
                else R.drawable.slide_dot_inactive
            )
            binding.llSlideDots.addView(dot)
            dots.add(dot)
        }
        binding.viewPagerMain.clearOnPageChangeListeners()
        binding.viewPagerMain.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { index, dot ->
                    dot.setImageResource(
                        if (index == position) R.drawable.slide_dot_active
                        else R.drawable.slide_dot_inactive
                    )
                }
            }
        })
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