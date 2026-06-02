package com.a1hd.movies.ui.sections.movie.watch

import com.a1hd.movies.BuildConfig
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.databinding.FragmentWatchMovieBinding
import com.a1hd.movies.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WatchMovieFragment: BaseFragment<FragmentWatchMovieBinding>(FragmentWatchMovieBinding::inflate) {

    private val viewModel: WatchMovieViewModel by viewModels()

    var movieUrl: String? = null
    var movieTitle: String? = null
    var episodes: ArrayList<MovieEpisodesDataModel>? = null
    var currentEpisodeIndex: Int = 0

    private val playerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val newIndex = data?.getIntExtra(VideoPlayerActivity.RESULT_EPISODE_INDEX, -1) ?: -1
            if (newIndex >= 0 && newIndex != currentEpisodeIndex) {
                // Load next/prev episode
                val eps = episodes ?: return@registerForActivityResult
                if (newIndex in eps.indices) {
                    currentEpisodeIndex = newIndex
                    movieUrl = eps[newIndex].link
                    loadCurrentEpisode()
                }
            } else {
                // Normal close — go back
                navigationRouter.navigateBack()
            }
        } else {
            // User pressed back — go back
            navigationRouter.navigateBack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            movieUrl = it.getString(ARG_MOVIE_URL, "")
            movieTitle = it.getString(ARG_TITLE)
            @Suppress("DEPRECATION")
            episodes = it.getSerializable(ARG_EPISODES) as? ArrayList<MovieEpisodesDataModel>
            currentEpisodeIndex = it.getInt(ARG_EPISODE_INDEX, 0)
        }
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (movieUrl.isNullOrEmpty()) {
            throw RuntimeException("movieUrl mustn't be null or empty")
        }

        binding.webView.init()
        binding.webView.setFullScreenView(requireActivity().actionBar, binding.fullscreenView)

        binding.webView.sourcesListLiveData.observe(viewLifecycleOwner) {
            binding.webView.ivSourceAvailable.isVisible = it.isNotEmpty()
            val referer = viewModel.embedUrl ?: movieUrl ?: "${BuildConfig.BASE_URL}/"
            val subtitles = binding.webView.getDetectedSubtitles()
            val servers = viewModel.servers
            val serverNames = if (servers.size > 1) ArrayList(servers.map { s -> s.name }) else null
            val serverUrls = if (servers.size > 1) ArrayList(servers.map { s -> s.embedUrl }) else null
            val serverIndex = servers.indexOfFirst { s -> s == viewModel.selectedServer }.coerceAtLeast(0)
            playerLauncher.launch(VideoPlayerActivity.createIntent(
                context = requireContext(),
                url = it.first(),
                referer = referer,
                title = movieTitle,
                subtitles = ArrayList(subtitles),
                episodes = episodes,
                currentEpisodeIndex = currentEpisodeIndex,
                serverNames = serverNames,
                serverUrls = serverUrls,
                serverIndex = serverIndex
            ))
        }

        binding.webView.sourcesLisFetchingLiveData.observe(viewLifecycleOwner) {
            binding.pbLoadingSources.isVisible = it
        }

        binding.webView.sourcesLoadingStatusLiveData.observe(viewLifecycleOwner) {
            binding.tvLoadingStatus.text = it
        }

        viewModel.serversLiveData.observe(viewLifecycleOwner) { servers ->
            if (servers.size > 1) {
                binding.spinnerServers.isVisible = true
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, servers.map { it.name })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerServers.adapter = adapter
                binding.spinnerServers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val server = servers[position]
                        if (server != viewModel.selectedServer) {
                            binding.pbLoadingSources.isVisible = true
                            binding.webView.resetState()
                            viewModel.selectServer(server)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            } else {
                binding.spinnerServers.isVisible = false
            }
        }

        viewModel.embedUrlLiveData.observe(viewLifecycleOwner) { result ->
            binding.pbLoadingSources.isVisible = false
            binding.webView.loadUrl(result.url ?: movieUrl!!)
        }

        loadCurrentEpisode()
    }

    private fun loadCurrentEpisode() {
        binding.pbLoadingSources.isVisible = true
        binding.webView.resetState()
        viewModel.fetchEmbedUrl(movieUrl!!)
    }

    companion object {

        private const val ARG_MOVIE_URL = "ARG_MOVIE_URL"
        private const val ARG_TITLE = "ARG_TITLE"
        private const val ARG_EPISODES = "ARG_EPISODES"
        private const val ARG_EPISODE_INDEX = "ARG_EPISODE_INDEX"

        @JvmStatic
        fun newInstance(
            movieUrl: String?,
            title: String? = null,
            episodes: ArrayList<MovieEpisodesDataModel>? = null,
            episodeIndex: Int = 0
        ): WatchMovieFragment {
            val fragment = WatchMovieFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_MOVIE_URL, movieUrl)
                title?.let { putString(ARG_TITLE, it) }
                episodes?.let { putSerializable(ARG_EPISODES, it) }
                putInt(ARG_EPISODE_INDEX, episodeIndex)
            }
            return fragment
        }
    }
}
