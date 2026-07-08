package com.a1hd.movies.ui.sections.favorite

import com.a1hd.movies.BuildConfig
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.a1hd.movies.databinding.FragmentFavoriteBinding
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.ui.sections.favorite.adapter.FavoritesRecyclerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FavoriteFragment: BaseFragment<FragmentFavoriteBinding>(FragmentFavoriteBinding::inflate) {

    @Inject
    lateinit var favoritesRecyclerAdapter: FavoritesRecyclerAdapter

    private val favoriteViewModel: FavoriteViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoritesRecyclerAdapter.onFavoriteClickListener = {
            val movieUrl = it.linkToDetails.replace(BuildConfig.BASE_URL, "")
            navigationRouter.navigateTo(Router.MovieDetails(movieUrl))
        }
        binding.rvFavorites.adapter = favoritesRecyclerAdapter
        binding.btnWatchedScreen.setOnClickListener {
            navigationRouter.navigateTo(Router.Watched)
        }

        favoriteViewModel.fetchAllFavorites()
        favoriteViewModel.fetchFavoritesLiveData.observe(viewLifecycleOwner) {
            favoritesRecyclerAdapter.setFavorites(it)
        }
    }
}