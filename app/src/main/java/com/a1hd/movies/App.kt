package com.a1hd.movies

import android.app.Application
import com.a1hd.movies.services.AuthenticationService
import com.a1hd.movies.services.FirebaseSyncService
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

    override fun onCreate() {
        super.onCreate()
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        FirebaseApp.initializeApp(this)

        setupFavoriteSyncHooks()

        // Sync on startup if signed in
        if (authService.isSignedIn) {
            CoroutineScope(Dispatchers.IO).launch {
                syncService.syncAll()
            }
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
}
