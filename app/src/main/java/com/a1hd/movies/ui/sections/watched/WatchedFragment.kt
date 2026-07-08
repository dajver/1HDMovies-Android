package com.a1hd.movies.ui.sections.watched

import com.a1hd.movies.BuildConfig
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.a1hd.movies.databinding.FragmentWatchedBinding
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.ui.sections.favorite.adapter.FavoritesRecyclerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WatchedFragment: BaseFragment<FragmentWatchedBinding>(FragmentWatchedBinding::inflate) {

    @Inject
    lateinit var watchedRecyclerAdapter: FavoritesRecyclerAdapter

    private val watchedViewModel: WatchedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        watchedRecyclerAdapter.onFavoriteClickListener = {
            val movieUrl = it.linkToDetails.replace(BuildConfig.BASE_URL, "")
            navigationRouter.navigateTo(Router.MovieDetails(movieUrl))
        }
        binding.rvWatched.adapter = watchedRecyclerAdapter

        watchedViewModel.fetchAllWatched()
        watchedViewModel.watchedLiveData.observe(viewLifecycleOwner) {
            watchedRecyclerAdapter.setFavorites(it)
            binding.tvEmpty.isVisible = it.isEmpty()
        }
    }
}
