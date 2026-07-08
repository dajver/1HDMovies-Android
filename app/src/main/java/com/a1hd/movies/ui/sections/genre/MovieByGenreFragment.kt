package com.a1hd.movies.ui.sections.genre

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.a1hd.movies.R
import com.a1hd.movies.api.repository.GenresEnum
import com.a1hd.movies.databinding.FragmentMovieByGenreBinding
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.ui.sections.allmovies.listener.PaginationScrollListener
import com.a1hd.movies.ui.sections.genre.adapter.MovieGenresRecyclerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MovieByGenreFragment: BaseFragment<FragmentMovieByGenreBinding>(FragmentMovieByGenreBinding::inflate) {

    @Inject
    lateinit var movieGenresRecyclerAdapter: MovieGenresRecyclerAdapter

    private val movieByGenreViewModel: MovieByGenreViewModel by viewModels()

    var movieGenre: GenresEnum? = null
    var tagTitle: String? = null
    var tagUrl: String? = null

    /** Effective listing URL — the genre URL or a clickable tag's URL. */
    private val listingUrl: String? get() = movieGenre?.url ?: tagUrl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            movieGenre = it.getSerializable(ARG_MOVIE_GENRE) as? GenresEnum
            tagTitle = it.getString(ARG_TAG_TITLE)
            tagUrl = it.getString(ARG_TAG_URL)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = listingUrl ?: throw RuntimeException("MovieByGenre requires a genre or a tag URL")

        val layoutManager = binding.rvMovies.layoutManager as GridLayoutManager
        val paginationListener = object : PaginationScrollListener(layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                movieByGenreViewModel.currentPage += 1
                movieByGenreViewModel.fetchPaginationMoviesByUrl(url)
            }
        }
        movieGenresRecyclerAdapter.onMovieGenreClickListener = {
            navigationRouter.navigateTo(Router.MovieDetails(it.link))
        }
        binding.rvMovies.adapter = movieGenresRecyclerAdapter
        binding.rvMovies.addOnScrollListener(paginationListener)
        binding.tvName.text = movieGenre?.toTitleString() ?: tagTitle

        movieByGenreViewModel.fetchMoviesByUrl(url)
        movieByGenreViewModel.fetchMoviesGenreLiveData.observe(viewLifecycleOwner) {
            binding.pbProgress.isVisible = false
            binding.llMoviesContainer.isVisible = true

            val moviesList = it.toMutableList()
            movieGenresRecyclerAdapter.setMovies(moviesList)
        }
    }

    private fun GenresEnum?.toTitleString(): String {
        return when (this) {
            GenresEnum.ACTION -> getString(R.string.action)
            GenresEnum.COMEDY -> getString(R.string.comedy)
            GenresEnum.DRAMA -> getString(R.string.drama)
            GenresEnum.FANTASY -> getString(R.string.fantasy)
            GenresEnum.HORROR -> getString(R.string.horror)
            GenresEnum.MYSTERY -> getString(R.string.mystery)
            GenresEnum.ANIMATION -> getString(R.string.animation)
            GenresEnum.TOP_IMDB -> getString(R.string.top_imdb)
            else -> throw RuntimeException("No such genre type as $this")
        }
    }

    companion object {

        private const val ARG_MOVIE_GENRE = "ARG_MOVIE_GENRE"
        private const val ARG_TAG_TITLE = "ARG_TAG_TITLE"
        private const val ARG_TAG_URL = "ARG_TAG_URL"

        @JvmStatic
        fun newInstance(movieGenre: GenresEnum?): MovieByGenreFragment {
            val fragment = MovieByGenreFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(ARG_MOVIE_GENRE, movieGenre)
            }
            return fragment
        }

        @JvmStatic
        fun newInstanceForTag(title: String, url: String): MovieByGenreFragment {
            val fragment = MovieByGenreFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TAG_TITLE, title)
                putString(ARG_TAG_URL, url)
            }
            return fragment
        }
    }
}