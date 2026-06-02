package com.a1hd.movies.ui.sections.filter

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.a1hd.movies.api.repository.FilterType
import com.a1hd.movies.databinding.FragmentFilterBinding
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.ui.sections.allmovies.adapter.AllMoviesRecyclerAdapter
import com.a1hd.movies.ui.sections.allmovies.listener.PaginationScrollListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FilterFragment : BaseFragment<FragmentFilterBinding>(FragmentFilterBinding::inflate) {

    @Inject
    lateinit var resultsAdapter: AllMoviesRecyclerAdapter

    private val viewModel: FilterViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = binding.rvResults.layoutManager as GridLayoutManager
        val paginationListener = object : PaginationScrollListener(layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                viewModel.loadMore()
            }
        }
        resultsAdapter.onMovieClickListener = {
            navigationRouter.navigateTo(Router.MovieDetails(it.link))
        }
        binding.rvResults.adapter = resultsAdapter
        binding.rvResults.addOnScrollListener(paginationListener)

        binding.filterBar.setFilterOptions(viewModel.filterOptions)
        binding.filterBar.onFiltersChanged = {
            val filters = binding.filterBar.filters
            filters.type.clear()
            filters.type.add(FilterType.MOVIE)
            filters.type.add(FilterType.TV_SERIES)
            binding.pbProgress.isVisible = true
            viewModel.applyFilters(filters)
        }

        viewModel.resultsLiveData.observe(viewLifecycleOwner) {
            binding.pbProgress.isVisible = false
            binding.tvEmpty.isVisible = it.isEmpty() && viewModel.hasSearched
            resultsAdapter.setMovies(it.toMutableList())
        }

        // Auto-fetch on first load with both types
        if (!viewModel.hasSearched) {
            viewModel.filterOptions.type.add(FilterType.MOVIE)
            viewModel.filterOptions.type.add(FilterType.TV_SERIES)
            binding.pbProgress.isVisible = true
            viewModel.applyFilters(viewModel.filterOptions)
        }
    }
}
