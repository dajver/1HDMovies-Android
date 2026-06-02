package com.a1hd.movies.ui.sections.movie

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.a1hd.movies.R
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.databinding.FragmentMovieDetailsBinding
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.ui.sections.movie.adapter.episodes.EpisodesRecyclerAdapter
import com.a1hd.movies.ui.sections.movie.adapter.season.SeasonsRecyclerAdapter
import com.a1hd.movies.ui.sections.movie.adapter.youlike.YouMayAlsoLikeRecyclerAdapter
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MovieDetailsFragment: BaseFragment<FragmentMovieDetailsBinding>(FragmentMovieDetailsBinding::inflate) {

    @Inject
    lateinit var seasonsRecyclerAdapter: SeasonsRecyclerAdapter

    @Inject
    lateinit var episodesRecyclerAdapter: EpisodesRecyclerAdapter

    @Inject
    lateinit var youMayAlsoLikeRecyclerAdapter: YouMayAlsoLikeRecyclerAdapter

    private val movieDetailsViewModel: MovieDetailsViewModel by viewModels()

    var movieUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            movieUrl = it.getString(ARG_MOVIE_URL, "")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (movieUrl.isNullOrEmpty()) {
            throw RuntimeException("movieUrl mustn't be null or empty")
        }

        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_color)!!)
        seasonsRecyclerAdapter.onSeasonClickListener = {
            episodesRecyclerAdapter.setEpisodes(it.episodes)
        }
        episodesRecyclerAdapter.onEpisodeClickListener = { episode ->
            val selectedSeason = movieDetailsViewModel.getSelectedSeason()
            val episodes = selectedSeason?.episodes?.let { ArrayList(it) }
            val index = episodes?.indexOfFirst { it.link == episode.link }?.coerceAtLeast(0) ?: 0
            val title = movieDetailsViewModel.getMovieName()
            navigationRouter.navigateTo(Router.WatchMovie(episode.link, title, episodes, index))
        }
        youMayAlsoLikeRecyclerAdapter.onMovieClickListener = {
            navigationRouter.navigateTo(Router.MovieDetails(it.link))
        }
        binding.rvSeasons.adapter = seasonsRecyclerAdapter
        binding.rvSeasons.addItemDecoration(divider)
        binding.rvEpisode.adapter = episodesRecyclerAdapter
        binding.rvEpisode.addItemDecoration(divider)
        binding.rvYouMayAlsoLike.adapter = youMayAlsoLikeRecyclerAdapter

        movieDetailsViewModel.fetchDetails(movieUrl!!)
        movieDetailsViewModel.fetchDetailsMoviesLiveData.observe(viewLifecycleOwner) { movie ->
            binding.pbProgress.isVisible = false
            binding.llMovieContainer.isVisible = true

            binding.tvName.text = movie.name
            binding.tvDescription.text = movie.description
            binding.tvCasts.text = movie.cast
            binding.tvGenre.text = movie.genre
            binding.tvDuration.text = getString(R.string.min, movie.duration)
            binding.tvCountry.text = movie.country
            binding.tvIMDB.text = movie.imdb
            binding.tvRelease.text = movie.release
            binding.tvProduction.text = movie.production
            Glide.with(requireContext()).load(movie.thumbnail).into(binding.ivPoster)

            binding.btnWatchMovie.isVisible = movie.seasonsList.isNullOrEmpty()
            binding.rvSeasons.isVisible = !movie.seasonsList.isNullOrEmpty()
            binding.rvEpisode.isVisible = !movie.seasonsList?.lastOrNull()?.episodes.isNullOrEmpty()

            val seasons = movie.seasonsList
            if (!seasons.isNullOrEmpty()) {
                seasonsRecyclerAdapter.setSeasons(seasons)
                binding.rvSeasons.scrollToPosition(seasonsRecyclerAdapter.itemCount - 1)

                val episodes = seasons.firstOrNull { it.isSelected }?.episodes ?: seasons.lastOrNull()?.episodes
                if (!episodes.isNullOrEmpty()) {
                    episodesRecyclerAdapter.setEpisodes(episodes)
                }
            }

            binding.btnWatchMovie.setOnClickListener {
                navigationRouter.navigateTo(Router.WatchMovie(movie.watchMovieLinkWithEpisodeId, title = movie.name))
            }

            addFavoriteButtonState(movie)
            binding.btnFavorites.setOnClickListener {
                movieDetailsViewModel.favorite(movie)
                addFavoriteButtonState(movie)
            }
        }

        movieDetailsViewModel.fetchYouMayAlsoLike(movieUrl!!)
        movieDetailsViewModel.fetchYouMayAlsoLikeLiveData.observe(viewLifecycleOwner) {
            youMayAlsoLikeRecyclerAdapter.setMovies(it.toMutableList())
        }
    }

    private fun addFavoriteButtonState(movie: MoviesDetailsDataModel) {
        binding.btnFavorites.text = if (movieDetailsViewModel.hasMovie(movie)) {
            getString(R.string.remove_favorites)
        } else {
            getString(R.string.add_favorites)
        }
    }

    companion object {

        private const val ARG_MOVIE_URL = "ARG_MOVIE_URL"

        @JvmStatic
        fun newInstance(movieUrl: String?): MovieDetailsFragment {
            val fragment = MovieDetailsFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_MOVIE_URL, movieUrl)
            }
            return fragment
        }
    }
}