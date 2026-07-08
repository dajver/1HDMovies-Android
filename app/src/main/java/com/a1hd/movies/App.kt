package com.a1hd.movies

import android.app.Application
import com.a1hd.movies.db.repository.NotificationRepository
import com.a1hd.movies.db.repository.PlaybackProgressRepository
import com.a1hd.movies.db.repository.WatchedEpisodeRepository
import com.a1hd.movies.db.repository.WatchedRepository
import com.a1hd.movies.services.AuthenticationService
import com.a1hd.movies.services.FirebaseSyncService
import com.a1hd.movies.services.NewEpisodeService
import com.a1hd.movies.ui.sections.favorite.repository.FavoriteRepository
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.conscrypt.Conscrypt
import java.security.Security
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var authService: AuthenticationService

    @Inject
    lateinit var syncService: FirebaseSyncService

    @Inject
    lateinit var favoriteRepository: FavoriteRepository

    @Inject
    lateinit var newEpisodeService: NewEpisodeService

    @Inject
    lateinit var playbackProgressRepository: PlaybackProgressRepository

    @Inject
    lateinit var watchedRepository: WatchedRepository

    @Inject
    lateinit var watchedEpisodeRepository: WatchedEpisodeRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        FirebaseApp.initializeApp(this)

        setupFavoriteSyncHooks()
        setupPlaybackAndWatchedSyncHooks()

        CoroutineScope(Dispatchers.IO).launch {
            // Sync on startup if signed in, then check favorited shows for new episodes (6h throttle).
            if (authService.isSignedIn) {
                syncService.syncAll()
            }
            newEpisodeService.checkForNewEpisodes()
        }
    }

    private fun setupFavoriteSyncHooks() {
        favoriteRepository.onFavoriteAdded = { movie ->
            if (authService.isSignedIn) {
                CoroutineScope(Dispatchers.IO).launch {
                    syncService.uploadFavorite(movie)
                }
            }
        }
        favoriteRepository.onFavoriteRemoved = { movie ->
            if (authService.isSignedIn) {
                CoroutineScope(Dispatchers.IO).launch {
                    syncService.deleteFavorite(movie)
                }
            }
        }
    }

    /** Immediate cloud upload/delete on mutation, mirroring the favorites hooks. */
    private fun setupPlaybackAndWatchedSyncHooks() {
        fun whenSignedIn(block: suspend () -> Unit) {
            if (authService.isSignedIn) {
                CoroutineScope(Dispatchers.IO).launch { block() }
            }
        }
        playbackProgressRepository.onProgressSaved = { e -> whenSignedIn { syncService.uploadPlaybackProgress(e) } }
        playbackProgressRepository.onProgressCleared = { link -> whenSignedIn { syncService.deletePlaybackProgress(link) } }
        watchedRepository.onWatchedChanged = { e -> whenSignedIn { syncService.uploadWatchedMovie(e) } }
        watchedRepository.onWatchedRemoved = { link -> whenSignedIn { syncService.deleteWatchedMovie(link) } }
        watchedEpisodeRepository.onWatchedChanged = { e -> whenSignedIn { syncService.uploadWatchedEpisode(e) } }
        watchedEpisodeRepository.onWatchedRemoved = { link -> whenSignedIn { syncService.deleteWatchedEpisode(link) } }
        notificationRepository.onNotificationChanged = { e -> whenSignedIn { syncService.uploadNotification(e) } }
    }
}
