package com.a1hd.movies.ui.sections.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.db.entity.ShowNotificationEntity
import com.a1hd.movies.db.repository.NotificationRepository
import com.a1hd.movies.etc.extensions.launch
import com.a1hd.movies.services.FirebaseSyncService
import com.a1hd.movies.services.NewEpisodeService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val newEpisodeService: NewEpisodeService,
    private val syncService: FirebaseSyncService
): ViewModel() {

    private val notificationsMutableLiveData = MutableLiveData<List<ShowNotificationEntity>>()
    val notificationsLiveData: LiveData<List<ShowNotificationEntity>> = notificationsMutableLiveData

    private val loadingMutableLiveData = MutableLiveData<Boolean>()
    val loadingLiveData: LiveData<Boolean> = loadingMutableLiveData

    fun load() = launch {
        notificationsMutableLiveData.postValue(notificationRepository.getAll())
    }

    /**
     * Opens the screen: briefly show the unread dots, then mark everything read and re-post so the
     * rows reflect the read state (dots gone) — mirrors iOS, where markAllRead re-renders the list.
     */
    fun open() = launch {
        notificationsMutableLiveData.postValue(notificationRepository.getAll())
        notificationRepository.markAllRead()
        syncService.markAllNotificationsRead()
        notificationsMutableLiveData.postValue(notificationRepository.getAll())
    }

    fun refresh() = launch {
        loadingMutableLiveData.postValue(true)
        newEpisodeService.checkForNewEpisodes(force = true)
        notificationsMutableLiveData.postValue(notificationRepository.getAll())
        loadingMutableLiveData.postValue(false)
    }
}
