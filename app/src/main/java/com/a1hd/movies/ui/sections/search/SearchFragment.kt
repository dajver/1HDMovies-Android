package com.a1hd.movies.ui.sections.search

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.a1hd.movies.databinding.FragmentSearchBinding
import com.a1hd.movies.etc.openKeyboard
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.ui.sections.search.adapter.SearchResultRecyclerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment: BaseFragment<FragmentSearchBinding>(FragmentSearchBinding::inflate) {

    @Inject
    lateinit var searchResultRecyclerAdapter: SearchResultRecyclerAdapter

    private val searchViewModel: SearchViewModel by viewModels()

    private var hasResults = false
    private var isLoading = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchResultRecyclerAdapter.onSearchResultClickListener = {
            navigationRouter.navigateTo(Router.MovieDetails(it.link))
        }
        binding.rvSearchResult.adapter = searchResultRecyclerAdapter

        searchViewModel.fetchSearchResultLiveData.observe(viewLifecycleOwner) {
            hasResults = it.isNotEmpty()
            searchResultRecyclerAdapter.setSearchResult(it.toMutableList())
            updateEmptyState()
        }

        searchViewModel.isLoadingLiveData.observe(viewLifecycleOwner) {
            isLoading = it
            binding.pbLoading.isVisible = it
            updateEmptyState()
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.etSearch.requestFocus()
        binding.etSearch.openKeyboard()
        binding.etSearch.addTextChangedListener {
            val query = it?.toString().orEmpty()
            binding.btnClearSearch.isVisible = query.isNotEmpty()
            searchViewModel.search(query)
        }
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            binding.etSearch.requestFocus()
            binding.etSearch.openKeyboard()
        }
    }

    /** "No results found" shows only for a non-blank query that finished loading with no matches. */
    private fun updateEmptyState() {
        val query = binding.etSearch.text?.toString()?.trim().orEmpty()
        binding.tvNoResults.isVisible = query.isNotEmpty() && !isLoading && !hasResults
    }
}
