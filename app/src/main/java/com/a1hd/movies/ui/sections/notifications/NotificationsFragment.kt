package com.a1hd.movies.ui.sections.notifications

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.a1hd.movies.databinding.FragmentNotificationsBinding
import com.a1hd.movies.ui.base.BaseFragment
import com.a1hd.movies.ui.navigation.route.Router
import com.a1hd.movies.ui.sections.notifications.adapter.NotificationsRecyclerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsFragment: BaseFragment<FragmentNotificationsBinding>(FragmentNotificationsBinding::inflate) {

    @Inject
    lateinit var notificationsRecyclerAdapter: NotificationsRecyclerAdapter

    private val notificationsViewModel: NotificationsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationsRecyclerAdapter.onItemClickListener = {
            navigationRouter.navigateTo(Router.MovieDetails(it.showLink))
        }
        binding.rvNotifications.adapter = notificationsRecyclerAdapter
        binding.btnRefresh.setOnClickListener {
            notificationsViewModel.refresh()
        }

        notificationsViewModel.notificationsLiveData.observe(viewLifecycleOwner) { items ->
            notificationsRecyclerAdapter.setItems(items.toMutableList())
            binding.tvEmpty.isVisible = items.isEmpty()
        }
        notificationsViewModel.loadingLiveData.observe(viewLifecycleOwner) {
            binding.pbLoading.isVisible = it
        }

        // Shows the list (with unread dots), then clears the unread badge.
        notificationsViewModel.open()
    }
}
