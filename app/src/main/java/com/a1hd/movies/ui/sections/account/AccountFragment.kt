package com.a1hd.movies.ui.sections.account

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.a1hd.movies.R
import com.google.android.gms.common.api.ApiException
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
        // Process the returned data regardless of result code; surface any failure.
        lifecycleScope.launch {
            try {
                val user = authService.handleSignInResult(result.data)
                if (user != null) {
                    updateUI()
                    syncService.syncAll()
                    updateUI()
                } else {
                    toast(getString(R.string.sign_in_failed))
                }
            } catch (e: ApiException) {
                toast(getString(R.string.sign_in_error_code, e.statusCode))
            } catch (e: Exception) {
                toast(getString(R.string.sign_in_error, e.message ?: "unknown"))
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
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
                syncService.lastSyncError?.let { toast(getString(R.string.sign_in_error, it)) }
            }
        }

        updateUI()
    }

    private fun updateUI() {
        val signedIn = authService.isSignedIn
        binding.llSignedIn.isVisible = signedIn
        binding.llSignIn.isVisible = !signedIn

        if (signedIn) {
            binding.tvUserName.text = authService.displayName ?: authService.email ?: "User"
            val email = authService.email
            binding.tvUserEmail.text = email ?: ""
            binding.tvUserEmail.isVisible = !email.isNullOrEmpty()

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
