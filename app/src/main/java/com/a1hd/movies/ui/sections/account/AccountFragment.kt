package com.a1hd.movies.ui.sections.account

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.a1hd.movies.R
import com.a1hd.movies.databinding.FragmentAccountBinding
import com.a1hd.movies.services.AuthenticationService
import com.a1hd.movies.services.FirebaseSyncService
import com.a1hd.movies.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : BaseFragment<FragmentAccountBinding>(FragmentAccountBinding::inflate) {

    @Inject
    lateinit var authService: AuthenticationService

    @Inject
    lateinit var syncService: FirebaseSyncService

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                try {
                    authService.handleSignInResult(result.data)
                    updateUI()
                    syncService.syncAll()
                    updateUI()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Google Sign-In with web client ID from google-services.json
        val webClientId = getString(R.string.default_web_client_id)
        authService.initGoogleSignIn(requireActivity(), webClientId)

        binding.btnSignIn.setOnClickListener {
            val intent = authService.getSignInIntent()
            if (intent != null) {
                signInLauncher.launch(intent)
            }
        }

        binding.btnSignOut.setOnClickListener {
            authService.signOut()
            updateUI()
        }

        binding.btnSync.setOnClickListener {
            lifecycleScope.launch {
                binding.btnSync.text = getString(R.string.syncing)
                binding.btnSync.isEnabled = false
                syncService.syncAll()
                binding.btnSync.text = getString(R.string.sync_now)
                binding.btnSync.isEnabled = true
                updateUI()
            }
        }

        updateUI()
    }

    private fun updateUI() {
        val signedIn = authService.isSignedIn
        binding.llSignedIn.isVisible = signedIn
        binding.llSignIn.isVisible = !signedIn

        if (signedIn) {
            binding.tvUserName.text = getString(R.string.signed_in_as, authService.displayName ?: authService.email ?: "User")

            val lastSync = syncService.lastSyncDate
            if (lastSync > 0) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                binding.tvLastSync.text = getString(R.string.last_sync, dateFormat.format(Date(lastSync)))
            } else {
                binding.tvLastSync.text = getString(R.string.last_sync, "Never")
            }
        }
    }
}
