package com.a1hd.movies.ui.sections.allmovies

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.a1hd.movies.api.repository.models.FilterType
import com.a1hd.movies.databinding.FragmentAllMoviesBinding
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.ui.sections.allmovies.adapter.AllMoviesRecyclerAdapter
import com.a1hd.movies.ui.sections.allmovies.listener.PaginationScrollListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AllMoviesFragment: BaseFragment<FragmentAllMoviesBinding>(FragmentAllMoviesBinding::inflate) {

    @Inject
    lateinit var allMoviesRecyclerAdapter: AllMoviesRecyclerAdapter

    private val allMoviesViewModel: AllMoviesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = binding.rvMovies.layoutManager as GridLayoutManager
        val paginationListener = object : PaginationScrollListener(layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                allMoviesViewModel.currentPage += 1
                allMoviesViewModel.fetchPaginationMovies()
            }
        }
        allMoviesRecyclerAdapter.onMovieClickListener = {
            navigationRouter.navigateTo(Router.MovieDetails(it.link))
        }
        binding.rvMovies.adapter = allMoviesRecyclerAdapter
        binding.rvMovies.addOnScrollListener(paginationListener)

        binding.filterBar.setFilterOptions(allMoviesViewModel.filterOptions)
        binding.filterBar.onFiltersChanged = {
            val filters = binding.filterBar.filters
            filters.type.clear()
            filters.type.add(FilterType.MOVIE)
            if (binding.filterBar.hasActiveFilters) {
                binding.pbProgress.isVisible = true
                allMoviesViewModel.applyFilters(filters)
            } else {
                allMoviesViewModel.resetFilters()
            }
        }

        allMoviesViewModel.fetchMovies()
        allMoviesViewModel.fetchMoviesLiveData.observe(viewLifecycleOwner) {
            binding.pbProgress.isVisible = false
            binding.llMoviesContainer.isVisible = true

            val moviesList = it.toMutableList()
            allMoviesRecyclerAdapter.setMovies(moviesList)
        }
    }
}
