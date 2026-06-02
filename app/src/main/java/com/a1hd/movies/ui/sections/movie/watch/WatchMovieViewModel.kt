package com.a1hd.movies.ui.sections.movie.watch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a1hd.movies.api.RestHttpClient
import com.a1hd.movies.etc.extensions.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import org.jsoup.Jsoup
import javax.inject.Inject

data class ServerOption(
    val name: String,
    val embedUrl: String
)

@HiltViewModel
class WatchMovieViewModel @Inject constructor(
    private val restHttpClient: RestHttpClient
) : ViewModel() {

    private val embedUrlMutableLiveData = MutableLiveData<EmbedUrlResult>()
    val embedUrlLiveData: LiveData<EmbedUrlResult> = embedUrlMutableLiveData

    private val serversMutableLiveData = MutableLiveData<List<ServerOption>>()
    val serversLiveData: LiveData<List<ServerOption>> = serversMutableLiveData

    var embedUrl: String? = null
        private set

    var selectedServer: ServerOption? = null
        private set

    var servers: List<ServerOption> = emptyList()
        private set

    fun fetchEmbedUrl(watchUrl: String) = launch {
        try {
            val html = restHttpClient.get(watchUrl)
            val plUrlPattern = "const pl_url = '([^']+)'".toRegex()
            val plUrlMatch = plUrlPattern.find(html)
            val plUrl = plUrlMatch?.groups?.get(1)?.value

            if (plUrl != null) {
                val serverHtml = restHttpClient.get(plUrl)
                val doc = Jsoup.parse(serverHtml)
                val serverElements = doc.select("a.sv-item")

                val options = mutableListOf<ServerOption>()
                for (element in serverElements) {
                    val name = element.text().trim()
                    val url = element.attr("data-id")
                    if (url.isNotEmpty()) {
                        options.add(ServerOption(name, url))
                    }
                }

                servers = options
                serversMutableLiveData.postValue(options)

                val first = options.firstOrNull()
                if (first != null) {
                    selectedServer = first
                    embedUrl = first.embedUrl
                    embedUrlMutableLiveData.postValue(EmbedUrlResult(first.embedUrl))
                } else {
                    embedUrlMutableLiveData.postValue(EmbedUrlResult(null))
                }
            } else {
                embedUrlMutableLiveData.postValue(EmbedUrlResult(null))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            embedUrlMutableLiveData.postValue(EmbedUrlResult(null))
        }
    }

    fun selectServer(server: ServerOption) {
        selectedServer = server
        embedUrl = server.embedUrl
        embedUrlMutableLiveData.postValue(EmbedUrlResult(server.embedUrl))
    }

    data class EmbedUrlResult(val url: String?)
}
