package com.a1hd.movies.ui.views

import android.app.ActionBar
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.a1hd.movies.client.VideoChromeClient
import com.a1hd.movies.client.WEB_VIEW_USER_AGENT
import com.a1hd.movies.databinding.ViewVideoWebviewBinding
import com.a1hd.movies.etc.LastOpenedScreenRepository
import com.a1hd.movies.ui.sections.select.SelectSourceSheetFragment
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class VideoWebView(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {

    @Inject
    lateinit var lastOpenedScreenRepository: LastOpenedScreenRepository

    private var viewBinding: ViewVideoWebviewBinding
    private var chromeClient: VideoChromeClient
    private val handler = Handler(Looper.getMainLooper())
    private val delayMillis: Long = 3000
    private val sourceList = mutableListOf<String>()
    private val subtitleMap = java.util.concurrent.ConcurrentHashMap<String, SubtitleTrack>()
    private var isRequesting = false
    private var hasReportedSources = false

    private val sourcesListMutableLiveData = MutableLiveData<List<String>>()
    val sourcesListLiveData: LiveData<List<String>> = sourcesListMutableLiveData

    private val sourcesListFetchingMutableLiveData = MutableLiveData<Boolean>()
    val sourcesLisFetchingLiveData: LiveData<Boolean> = sourcesListFetchingMutableLiveData

    private val sourcesLoadingStatusMutableLiveData = MutableLiveData<String>()
    val sourcesLoadingStatusLiveData: LiveData<String> = sourcesLoadingStatusMutableLiveData

    init {
        viewBinding = ViewVideoWebviewBinding.inflate(LayoutInflater.from(context), this, true)
        chromeClient = VideoChromeClient(viewBinding.videoViewFrame, viewBinding.videoViewWebview)
    }

    fun init() {
        viewBinding.videoViewWebview.webChromeClient = chromeClient
        viewBinding.videoViewWebview.addJavascriptInterface(SubtitleJsBridge(), "subtitleDetector")
        viewBinding.videoViewWebview.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                if (url.contains(".m3u8")) {
                    if (!isRequesting) {
                        sourceList.clear()
                        isRequesting = true
                        sourcesListFetchingMutableLiveData.postValue(true)
                    }

                    sourceList.add(url)
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({
                        if (sourceList.isNotEmpty() && !hasReportedSources) {
                            hasReportedSources = true
                            sourcesListMutableLiveData.postValue(ArrayList(sourceList))
                        }
                        isRequesting = false
                        sourcesListFetchingMutableLiveData.postValue(false)
                    }, delayMillis)
                }

                // Detect subtitle URLs from network requests
                if (url.contains(".vtt") || url.contains(".srt")) {
                    if (!subtitleMap.containsKey(url)) {
                        subtitleMap[url] = SubtitleTrack(label = "Unknown", url = url, language = "")
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject subtitle detection + auto-play script
                val script = SUBTITLE_DETECT_JS + "\n" + AUTO_PLAY_JS
                handler.postDelayed({
                    view?.evaluateJavascript(script, null)
                }, 2000)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
                lastOpenedScreenRepository.lastOpenedPage = url
                return false
            }
        }
        viewBinding.videoViewWebview.settings.userAgentString = WEB_VIEW_USER_AGENT
        viewBinding.videoViewWebview.settings.mediaPlaybackRequiresUserGesture = false
        viewBinding.videoViewWebview.setBackgroundColor(Color.TRANSPARENT)
        viewBinding.videoViewWebview.settings.apply {
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        chromeClient.onConsoleErrorMessage = {
            sourcesLoadingStatusMutableLiveData.postValue(it)
        }
    }

    fun resetState() {
        sourceList.clear()
        subtitleMap.clear()
        isRequesting = false
        hasReportedSources = false
        handler.removeCallbacksAndMessages(null)
        viewBinding.videoViewWebview.stopLoading()
        viewBinding.videoViewWebview.loadUrl("about:blank")
    }

    fun getDetectedSubtitles(): List<SubtitleTrack> {
        return ArrayList(subtitleMap.values)
    }

    fun showSourceDialog(fragmentManager: FragmentManager) {
        val sourceDialog = SelectSourceSheetFragment()
        sourceDialog.setSourceList(sourceList)
        sourceDialog.show(fragmentManager, "SelectSourceSheetFragment")
    }

    fun setFullScreenView(actionBar: ActionBar?, frameLayout: FrameLayout) {
        chromeClient.setFullScreenView(actionBar, frameLayout)
    }

    fun loadUrl(url: String) {
        viewBinding.videoViewWebview.loadUrl(url)
        lastOpenedScreenRepository.lastOpenedPage = url
    }

    fun canGoBack(): Boolean = viewBinding.videoViewWebview.canGoBack()

    fun goBack() {
        viewBinding.videoViewWebview.goBack()
    }

    val ivSourceAvailable
        get() = viewBinding.ivSourceAvailable

    private inner class SubtitleJsBridge {
        @JavascriptInterface
        fun onSubtitleDetected(jsonString: String) {
            try {
                val json = JSONObject(jsonString)
                val url = json.optString("url", "")
                val label = json.optString("label", "")
                val lang = json.optString("lang", "")
                if (url.isNotEmpty() && !subtitleMap.containsKey(url)) {
                    subtitleMap[url] = SubtitleTrack(
                        label = label.ifEmpty { lang.ifEmpty { "Unknown" } },
                        url = url,
                        language = lang
                    )
                }
            } catch (_: Exception) {}
        }
    }

    companion object {
        private val SUBTITLE_DETECT_JS = """
            (function() {
                var reportedSubs = {};
                function reportSub(url, label, lang) {
                    if (url && !reportedSubs[url]) {
                        reportedSubs[url] = true;
                        subtitleDetector.onSubtitleDetected(JSON.stringify({url: url, label: label || '', lang: lang || ''}));
                    }
                }
                function scanObject(obj) {
                    if (!obj || typeof obj !== 'object') return;
                    if (Array.isArray(obj)) { obj.forEach(function(item) { scanObject(item); }); return; }
                    var subUrl = obj.file || obj.url || obj.src || '';
                    var kind = (obj.kind || '').toLowerCase();
                    var label = obj.label || obj.language || obj.lang || '';
                    if (subUrl && (kind === 'captions' || kind === 'subtitles' || subUrl.indexOf('.vtt') !== -1 || subUrl.indexOf('.srt') !== -1)) {
                        reportSub(subUrl, label, obj.language || obj.lang || '');
                    }
                    ['tracks', 'subtitles', 'captions', 'subs', 'textTracks', 'sources'].forEach(function(key) {
                        if (obj[key]) scanObject(obj[key]);
                    });
                }
                function scanForSubs(text) {
                    try { var obj = (typeof text === 'string') ? JSON.parse(text) : text; scanObject(obj); } catch(e) {}
                }
                var origOpen = XMLHttpRequest.prototype.open;
                var origSend = XMLHttpRequest.prototype.send;
                XMLHttpRequest.prototype.open = function(method, url) {
                    this._interceptUrl = url;
                    if (url && (url.indexOf('.vtt') !== -1 || url.indexOf('.srt') !== -1)) { reportSub(url, '', ''); }
                    return origOpen.apply(this, arguments);
                };
                XMLHttpRequest.prototype.send = function() {
                    var xhr = this;
                    xhr.addEventListener('load', function() {
                        try {
                            var ct = xhr.getResponseHeader('content-type') || '';
                            if (ct.indexOf('json') !== -1 || (xhr._interceptUrl && xhr._interceptUrl.indexOf('json') !== -1)) { scanForSubs(xhr.responseText); }
                            if (xhr.responseText) {
                                var vttMatches = xhr.responseText.match(/https?:[^"'\s]+\.vtt/g);
                                if (vttMatches) { vttMatches.forEach(function(u) { reportSub(u, '', ''); }); }
                            }
                        } catch(e) {}
                    });
                    return origSend.apply(this, arguments);
                };
                var origFetch = window.fetch;
                window.fetch = function(input) {
                    var url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                    if (url && (url.indexOf('.vtt') !== -1 || url.indexOf('.srt') !== -1)) { reportSub(url, '', ''); }
                    return origFetch.apply(this, arguments).then(function(response) {
                        var clone = response.clone();
                        clone.text().then(function(text) {
                            try { scanForSubs(text); var m = text.match(/https?:[^"'\s]+\.vtt/g); if (m) m.forEach(function(u) { reportSub(u, '', ''); }); } catch(e) {}
                        }).catch(function(){});
                        return response;
                    });
                };
                function fullScan() {
                    document.querySelectorAll('track').forEach(function(t) { if (t.src) reportSub(t.src, t.label || '', t.srclang || ''); });
                    if (typeof jwplayer !== 'undefined') {
                        try {
                            var cfg = jwplayer().getConfig();
                            if (cfg && cfg.tracks) scanObject({tracks: cfg.tracks});
                            var playlist = jwplayer().getPlaylist();
                            if (playlist) playlist.forEach(function(item) { if (item.tracks) scanObject({tracks: item.tracks}); });
                        } catch(e) {}
                    }
                    var vjsEl = document.querySelector('.video-js');
                    if (vjsEl && vjsEl.player) {
                        try {
                            var tt = vjsEl.player.textTracks();
                            for (var i = 0; i < tt.length; i++) { if (tt[i].src) reportSub(tt[i].src, tt[i].label || '', tt[i].language || ''); }
                        } catch(e) {}
                    }
                    document.querySelectorAll('script').forEach(function(s) {
                        if (s.textContent) { var matches = s.textContent.match(/https?:[^"'\s]+\.vtt/g); if (matches) matches.forEach(function(u) { reportSub(u.replace(/\\\\/g, ''), '', ''); }); }
                    });
                }
                var observer = new MutationObserver(function() { fullScan(); });
                observer.observe(document.documentElement, { childList: true, subtree: true });
                setInterval(fullScan, 2000);
            })();
        """.trimIndent()

        private val AUTO_PLAY_JS = """
            (function() {
                function tryPlay() {
                    if (typeof jwplayer !== 'undefined') { try { jwplayer().play(); return true; } catch(e) {} }
                    var vjsPlayer = document.querySelector('.video-js');
                    if (vjsPlayer && vjsPlayer.player) { try { vjsPlayer.player.play(); return true; } catch(e) {} }
                    var video = document.querySelector('video');
                    if (video) { video.play(); return true; }
                    var selectors = ['.jw-icon-playback', '.jw-display-icon-container', '.vjs-big-play-button', '[class*="play-btn"]', '[class*="playBtn"]', 'button[aria-label*="Play"]', '.play-button', '#play-btn'];
                    for (var i = 0; i < selectors.length; i++) { var btn = document.querySelector(selectors[i]); if (btn) { btn.click(); return true; } }
                    return false;
                }
                setTimeout(tryPlay, 1000);
                setTimeout(tryPlay, 3000);
                setTimeout(tryPlay, 5000);
            })();
        """.trimIndent()
    }
}