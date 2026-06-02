package com.a1hd.movies.ui.sections.movie.watch

import com.a1hd.movies.BuildConfig
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.a1hd.movies.R
import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.databinding.ActivityVideoPlayerBinding
import com.a1hd.movies.ui.base.BaseActivity
import com.a1hd.movies.ui.views.SubtitleCue
import com.a1hd.movies.ui.views.SubtitleTrack
import com.a1hd.movies.ui.views.VTTParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val MIN_BUFFER_DURATION = 2000
private const val MAX_BUFFER_DURATION = 5000
private const val MIN_PLAYBACK_START_BUFFER = 1500
private const val MIN_PLAYBACK_RESUME_BUFFER = 2000

@UnstableApi
@AndroidEntryPoint
class VideoPlayerActivity : BaseActivity<ActivityVideoPlayerBinding>(ActivityVideoPlayerBinding::inflate), Player.Listener {

    private lateinit var simpleExoplayer: ExoPlayer
    private lateinit var videoUrl: String
    private var referer: String = "${BuildConfig.BASE_URL}/"

    private var subtitleTracks: ArrayList<SubtitleTrack> = arrayListOf()
    private var episodes: ArrayList<MovieEpisodesDataModel>? = null
    private var currentEpisodeIndex: Int = 0
    private var serverNames: ArrayList<String>? = null
    private var serverUrls: ArrayList<String>? = null
    private var currentServerIndex: Int = 0

    private var currentSubtitleCues: List<SubtitleCue> = emptyList()
    private var selectedSubtitleTrack: SubtitleTrack? = null
    private var subtitlesEnabled = false

    private val subtitleHandler = Handler(Looper.getMainLooper())
    private val subtitleUpdateRunnable = object : Runnable {
        override fun run() {
            updateSubtitleDisplay()
            subtitleHandler.postDelayed(this, 250)
        }
    }

    private var movieTitle: String? = null

    // Views inside the custom controller layout (bottom bar)
    private var btnPrevEpisode: Button? = null
    private var btnNextEpisode: Button? = null
    private var btnSubtitles: Button? = null
    private var btnServer: Button? = null

    private var topBarVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = intent.extras
        videoUrl = bundle?.getString(EXTRA_LINK).toString()
        referer = bundle?.getString(EXTRA_REFERER) ?: "${BuildConfig.BASE_URL}/"
        @Suppress("DEPRECATION")
        subtitleTracks = bundle?.getSerializable(EXTRA_SUBTITLES) as? ArrayList<SubtitleTrack> ?: arrayListOf()
        @Suppress("DEPRECATION")
        episodes = bundle?.getSerializable(EXTRA_EPISODES) as? ArrayList<MovieEpisodesDataModel>
        currentEpisodeIndex = bundle?.getInt(EXTRA_EPISODE_INDEX, 0) ?: 0
        serverNames = bundle?.getStringArrayList(EXTRA_SERVER_NAMES)
        serverUrls = bundle?.getStringArrayList(EXTRA_SERVER_URLS)
        currentServerIndex = bundle?.getInt(EXTRA_SERVER_INDEX, 0) ?: 0
        movieTitle = bundle?.getString(EXTRA_TITLE)

        fullScreen()
        findControllerViews()
        setupTopBar()
        setupEpisodeControls()
        setupSubtitleControls()
        setupServerControls()
        syncTopBarWithController()
    }

    private fun findControllerViews() {
        val playerView = binding.playerViewFullscreen
        btnPrevEpisode = playerView.findViewById(R.id.btnPrevEpisode)
        btnNextEpisode = playerView.findViewById(R.id.btnNextEpisode)
        btnSubtitles = playerView.findViewById(R.id.btnSubtitles)
        btnServer = playerView.findViewById(R.id.btnServer)
    }

    private fun setupTopBar() {
        movieTitle?.let {
            binding.tvMovieTitle.text = it
        }
        val eps = episodes
        if (eps != null && eps.size > 1 && currentEpisodeIndex in eps.indices) {
            val episode = eps[currentEpisodeIndex]
            binding.tvEpisodeLabel.text = "${episode.episodeNumber} - ${episode.episodeName}"
            binding.tvEpisodeLabel.isVisible = true
        }
    }

    private fun syncTopBarWithController() {
        // Poll the controller visibility and sync the top bar
        val checkRunnable = object : Runnable {
            override fun run() {
                val controllerVisible = binding.playerViewFullscreen.isControllerFullyVisible
                if (controllerVisible != topBarVisible) {
                    topBarVisible = controllerVisible
                    binding.llTopBar.isVisible = controllerVisible
                }
                subtitleHandler.postDelayed(this, 200)
            }
        }
        subtitleHandler.post(checkRunnable)
    }

    private fun setupEpisodeControls() {
        val eps = episodes
        if (eps != null && eps.size > 1) {
            btnPrevEpisode?.isVisible = true
            btnNextEpisode?.isVisible = true
            updateEpisodeButtonStates()

            btnPrevEpisode?.setOnClickListener {
                if (currentEpisodeIndex > 0) {
                    navigateToEpisode(currentEpisodeIndex - 1)
                }
            }
            btnNextEpisode?.setOnClickListener {
                if (currentEpisodeIndex < eps.size - 1) {
                    navigateToEpisode(currentEpisodeIndex + 1)
                }
            }
        }
    }

    private fun updateEpisodeButtonStates() {
        val eps = episodes ?: return
        btnPrevEpisode?.isEnabled = currentEpisodeIndex > 0
        btnPrevEpisode?.alpha = if (currentEpisodeIndex > 0) 1.0f else 0.4f
        btnNextEpisode?.isEnabled = currentEpisodeIndex < eps.size - 1
        btnNextEpisode?.alpha = if (currentEpisodeIndex < eps.size - 1) 1.0f else 0.4f
    }

    private fun navigateToEpisode(index: Int) {
        val eps = episodes ?: return
        if (index !in eps.indices) return
        val resultIntent = Intent()
        resultIntent.putExtra(RESULT_EPISODE_INDEX, index)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun setupSubtitleControls() {
        if (subtitleTracks.isNotEmpty()) {
            btnSubtitles?.isVisible = true
            val englishTrack = subtitleTracks.firstOrNull {
                it.language.lowercase().startsWith("en") || it.label.lowercase().contains("english")
            }
            if (englishTrack != null) {
                selectSubtitleTrack(englishTrack)
            }
            btnSubtitles?.setOnClickListener {
                showSubtitlePicker()
            }
        }
    }

    private fun setupServerControls() {
        val names = serverNames
        val urls = serverUrls
        if (names != null && urls != null && names.size > 1) {
            btnServer?.isVisible = true
            btnServer?.text = names.getOrNull(currentServerIndex) ?: getString(R.string.server)
            btnServer?.setOnClickListener {
                showServerPicker()
            }
        }
    }

    private fun showServerPicker() {
        val names = serverNames ?: return
        val builder = androidx.appcompat.app.AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
        builder.setTitle(getString(R.string.server))
        builder.setItems(names.toTypedArray()) { _, which ->
            if (which != currentServerIndex) {
                currentServerIndex = which
                btnServer?.text = names[which]
                // Restart with the new server - go back to WatchMovieFragment to re-detect stream
                finish()
            }
        }
        builder.show()
    }

    private fun showSubtitlePicker() {
        val items = mutableListOf("Off")
        items.addAll(subtitleTracks.map { it.label.ifEmpty { it.language.ifEmpty { "Unknown" } } })

        val builder = androidx.appcompat.app.AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
        builder.setTitle(getString(R.string.subtitles))
        builder.setItems(items.toTypedArray()) { _, which ->
            if (which == 0) {
                subtitlesEnabled = false
                selectedSubtitleTrack = null
                currentSubtitleCues = emptyList()
                binding.tvSubtitle.isVisible = false
                btnSubtitles?.text = getString(R.string.subtitles)
            } else {
                val track = subtitleTracks[which - 1]
                selectSubtitleTrack(track)
            }
        }
        builder.show()
    }

    private fun selectSubtitleTrack(track: SubtitleTrack) {
        selectedSubtitleTrack = track
        subtitlesEnabled = true
        btnSubtitles?.text = track.label.ifEmpty { track.language.ifEmpty { getString(R.string.subtitles) } }
        fetchSubtitleContent(track.url)
    }

    private fun fetchSubtitleContent(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val content = response.body?.string() ?: return@launch
                val cues = VTTParser.parse(content)
                withContext(Dispatchers.Main) {
                    currentSubtitleCues = cues
                    binding.tvSubtitle.isVisible = true
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateSubtitleDisplay() {
        if (!subtitlesEnabled || currentSubtitleCues.isEmpty()) {
            binding.tvSubtitle.isVisible = false
            return
        }
        if (!::simpleExoplayer.isInitialized) return

        val positionMs = simpleExoplayer.currentPosition
        val positionSec = positionMs / 1000.0

        val activeCue = currentSubtitleCues.firstOrNull {
            positionSec >= it.startTime && positionSec <= it.endTime
        }

        if (activeCue != null) {
            binding.tvSubtitle.text = activeCue.text
            binding.tvSubtitle.isVisible = true
        } else {
            binding.tvSubtitle.isVisible = false
        }
    }

    private fun initializePlayer() {
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16))
            .setBufferDurationsMs(MIN_BUFFER_DURATION, MAX_BUFFER_DURATION, MIN_PLAYBACK_START_BUFFER, MIN_PLAYBACK_RESUME_BUFFER)
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true).build()
        val trackSelector = DefaultTrackSelector(this).apply {
            parameters = parameters.buildUpon()
                .setMaxVideoSize(1280, 720)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .build()
        }

        simpleExoplayer = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
        preparePlayer(videoUrl)
    }

    private fun preparePlayer(videoUrl: String) {
        val uri = Uri.parse(videoUrl)
        val mediaSource = buildMediaSource(uri)
        simpleExoplayer.setMediaSource(mediaSource, false)
        simpleExoplayer.playWhenReady = true
        simpleExoplayer.addListener(this)
        binding.playerViewFullscreen.player = simpleExoplayer
        simpleExoplayer.prepare()
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf(
                "Referer" to referer,
                "Origin" to referer.trimEnd('/')
            ))
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36")
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            )
    }

    private fun releasePlayer() {
        if (::simpleExoplayer.isInitialized) {
            simpleExoplayer.pause()
            simpleExoplayer.stop()
            simpleExoplayer.release()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_BUFFERING)
            binding.progressBar.visibility = View.VISIBLE
        else if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED)
            binding.progressBar.visibility = View.INVISIBLE
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        error.printStackTrace()
    }

    private fun fullScreen() {
        @Suppress("DEPRECATION")
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()
        @Suppress("DEPRECATION")
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        subtitleHandler.post(subtitleUpdateRunnable)
    }

    override fun onStop() {
        super.onStop()
        subtitleHandler.removeCallbacks(subtitleUpdateRunnable)
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        subtitleHandler.removeCallbacks(subtitleUpdateRunnable)
        releasePlayer()
    }

    companion object {

        const val RESULT_EPISODE_INDEX = "RESULT_EPISODE_INDEX"

        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_LINK = "EXTRA_LINK"
        private const val EXTRA_REFERER = "EXTRA_REFERER"
        private const val EXTRA_SUBTITLES = "EXTRA_SUBTITLES"
        private const val EXTRA_EPISODES = "EXTRA_EPISODES"
        private const val EXTRA_EPISODE_INDEX = "EXTRA_EPISODE_INDEX"
        private const val EXTRA_SERVER_NAMES = "EXTRA_SERVER_NAMES"
        private const val EXTRA_SERVER_URLS = "EXTRA_SERVER_URLS"
        private const val EXTRA_SERVER_INDEX = "EXTRA_SERVER_INDEX"

        fun createIntent(
            context: Context,
            url: String,
            referer: String = "${BuildConfig.BASE_URL}/",
            title: String? = null,
            subtitles: ArrayList<SubtitleTrack> = arrayListOf(),
            episodes: ArrayList<MovieEpisodesDataModel>? = null,
            currentEpisodeIndex: Int = 0,
            serverNames: ArrayList<String>? = null,
            serverUrls: ArrayList<String>? = null,
            serverIndex: Int = 0
        ): Intent {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(EXTRA_LINK, url)
            title?.let { intent.putExtra(EXTRA_TITLE, it) }
            intent.putExtra(EXTRA_REFERER, referer)
            intent.putExtra(EXTRA_SUBTITLES, subtitles)
            episodes?.let { intent.putExtra(EXTRA_EPISODES, it) }
            intent.putExtra(EXTRA_EPISODE_INDEX, currentEpisodeIndex)
            serverNames?.let { intent.putStringArrayListExtra(EXTRA_SERVER_NAMES, it) }
            serverUrls?.let { intent.putStringArrayListExtra(EXTRA_SERVER_URLS, it) }
            intent.putExtra(EXTRA_SERVER_INDEX, serverIndex)
            return intent
        }

        @Deprecated("Use createIntent instead", ReplaceWith("createIntent(context, url, referer)"))
        fun setUrl(context: Context, url: String, referer: String = "${BuildConfig.BASE_URL}/"): Intent {
            return createIntent(context, url, referer)
        }
    }
}
