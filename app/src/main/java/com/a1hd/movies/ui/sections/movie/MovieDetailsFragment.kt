package com.a1hd.movies.ui.sections.movie

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.a1hd.movies.R
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.api.repository.TagRef
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
            val details = movieDetailsViewModel.getDetails()
            val selectedSeason = movieDetailsViewModel.getSelectedSeason()
            val episodes = selectedSeason?.episodes?.let { ArrayList(it) }
            val index = episodes?.indexOfFirst { it.link == episode.link }?.coerceAtLeast(0) ?: 0
            navigationRouter.navigateTo(
                Router.WatchMovie(
                    movieUrl = episode.link,
                    title = details?.name,
                    episodes = episodes,
                    episodeIndex = index,
                    showLink = details?.linkToDetails,
                    thumbnail = details?.thumbnail,
                    contentType = details?.type?.name,
                    seasonNumber = selectedSeason?.seasonNumber
                )
            )
        }
        episodesRecyclerAdapter.onEpisodeLongClickListener = { episode ->
            showEpisodeWatchedMenu(episode)
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
            binding.tvDuration.text = getString(R.string.min, movie.duration)
            binding.tvIMDB.text = movie.imdb
            Glide.with(requireContext()).load(movie.thumbnail).into(binding.ivPoster)

            populateChips(binding.llGenre, movie.genreTags, movie.genre)
            populateChips(binding.llCasts, movie.castTags, movie.cast)
            populateChips(binding.llCountry, movie.countryTags, movie.country)
            populateChips(binding.llRelease, movie.yearTags, movie.release)
            populateChips(binding.llProduction, movie.productionTags, movie.production)

            binding.ivPoster.setOnClickListener { showFullPoster(movie.thumbnail) }
            binding.btnTrailer.setOnClickListener { openTrailer(movie.name, movie.release) }

            binding.btnWatchMovie.isVisible = movie.seasonsList.isNullOrEmpty()
            binding.rvSeasons.isVisible = !movie.seasonsList.isNullOrEmpty()
            binding.rvEpisode.isVisible = !movie.seasonsList?.lastOrNull()?.episodes.isNullOrEmpty()

            val seasons = movie.seasonsList
            if (!seasons.isNullOrEmpty()) {
                seasonsRecyclerAdapter.setSeasons(seasons)
                val selectedIndex = seasons.indexOfFirst { it.isSelected }.coerceAtLeast(0)
                binding.rvSeasons.scrollToPosition(selectedIndex)

                val episodes = seasons.firstOrNull { it.isSelected }?.episodes ?: seasons.lastOrNull()?.episodes
                if (!episodes.isNullOrEmpty()) {
                    episodesRecyclerAdapter.setEpisodes(episodes)
                }
            }

            binding.btnWatchMovie.setOnClickListener {
                navigationRouter.navigateTo(
                    Router.WatchMovie(
                        movieUrl = movie.watchMovieLinkWithEpisodeId,
                        title = movie.name,
                        showLink = movie.linkToDetails,
                        thumbnail = movie.thumbnail,
                        contentType = movie.type.name
                    )
                )
            }

            addFavoriteButtonState(movie)
            binding.btnFavorites.setOnClickListener {
                movieDetailsViewModel.favorite(movie)
                addFavoriteButtonState(movie)
            }
            binding.btnWatched.setOnClickListener {
                movieDetailsViewModel.toggleMovieWatched()
            }
        }

        movieDetailsViewModel.watchedEpisodesLiveData.observe(viewLifecycleOwner) { links ->
            episodesRecyclerAdapter.setWatchedLinks(links)
        }
        movieDetailsViewModel.watchedMovieLiveData.observe(viewLifecycleOwner) { isWatched ->
            binding.btnWatched.text = getString(
                if (isWatched) R.string.mark_unwatched else R.string.mark_watched
            )
        }

        movieDetailsViewModel.fetchYouMayAlsoLike(movieUrl!!)
        movieDetailsViewModel.fetchYouMayAlsoLikeLiveData.observe(viewLifecycleOwner) {
            youMayAlsoLikeRecyclerAdapter.setMovies(it.toMutableList())
        }
    }

    /** Renders a metadata list as clickable chips that open the matching listing. */
    private fun populateChips(container: LinearLayout, tags: List<TagRef>, fallback: String) {
        container.removeAllViews()
        if (tags.isEmpty()) {
            if (fallback.isNotBlank()) container.addView(createChip(fallback, null))
            return
        }
        tags.forEach { tag -> container.addView(createChip(tag.name, tag)) }
    }

    private fun createChip(text: String, tag: TagRef?): View {
        val density = resources.displayMetrics.density
        val padH = (12 * density).toInt()
        val padV = (6 * density).toInt()
        val chip = TextView(requireContext()).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 16f
            background = ContextCompat.getDrawable(requireContext(), R.drawable.chip_background)
            setPadding(padH, padV, padH, padV)
        }
        chip.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = (8 * density).toInt() }
        if (tag != null) {
            chip.isFocusable = true
            chip.isClickable = true
            chip.setOnClickListener { navigationRouter.navigateTo(Router.Tag(tag.name, tag.url)) }
        }
        return chip
    }

    /** Opens a YouTube search for the trailer (the site has no trailer data). */
    private fun openTrailer(name: String, year: String) {
        val query = Uri.encode("$name $year trailer".trim())
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showFullPoster(url: String) {
        if (url.isEmpty()) return
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val image = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            isFocusable = true
        }
        dialog.setContentView(
            image,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        Glide.with(this).load(url).into(image)
        image.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showEpisodeWatchedMenu(episode: com.a1hd.movies.api.repository.MovieEpisodesDataModel) {
        val seasonNumber = movieDetailsViewModel.getSelectedSeason()?.seasonNumber ?: ""
        val isWatched = movieDetailsViewModel.watchedEpisodesLiveData.value?.contains(episode.link) == true
        val action = getString(
            if (isWatched) R.string.mark_episode_unwatched else R.string.mark_episode_watched
        )
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("${episode.episodeNumber} ${episode.episodeName}".trim())
            .setItems(arrayOf(action)) { _, _ ->
                movieDetailsViewModel.toggleEpisodeWatched(episode, seasonNumber)
            }
            .show()
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