package com.a1hd.movies.api.repository

import com.a1hd.movies.BuildConfig
import com.a1hd.movies.api.RestHttpClient
import com.a1hd.movies.api.repository.models.FilterOptions
import com.a1hd.movies.etc.extensions.io
import org.jsoup.Jsoup
import javax.inject.Inject

class ParseJsonFilterRepository @Inject constructor(
    private val restHttpClient: RestHttpClient
) {

    suspend fun fetchFiltered(options: FilterOptions, page: Int = 1): List<MoviesDataModel> = io {
        val url = "${BuildConfig.BASE_URL}/filter?${options.queryString}&page=$page"
        val html = restHttpClient.get(url)
        val doc = Jsoup.parse(html)

        val moviesElements = doc.select("div.item-film")
        val filmVisualInformation = moviesElements.select("div.film-thumbnail").select("img.film-thumbnail-img")
        val filmTextInformation = moviesElements.select("div.film-detail")
        val filmReleaseInformation = moviesElements.select("div.film-info")
        val qualities = moviesElements.select("div.film-thumbnail").select("div.quality")
        val thumbnails = filmVisualInformation.eachAttr("src")
        val names = filmVisualInformation.eachAttr("alt")
        val links = filmTextInformation.select("h3.film-name").select("a").eachAttr("href")

        val dateInfoElements = filmReleaseInformation.select("span.item")
        val dateInfo = mutableListOf<String>()
        for (element in dateInfoElements) {
            for (textNode in element.textNodes()) {
                val text = textNode.text().trim()
                if (text.isNotEmpty()) {
                    dateInfo.add(text)
                }
            }
        }

        val typeAndYear = mutableListOf<String>()
        var i = 0
        while (i < dateInfo.size - 1) {
            typeAndYear.add("${dateInfo[i]},${dateInfo[i + 1]}")
            i += 2
        }

        val movies = mutableListOf<MoviesDataModel>()
        for (index in thumbnails.indices) {
            if (index >= names.size || index >= links.size) break
            val name = names[index]
            val thumbnail = thumbnails[index]
            val link = links[index]
            val type = if (index < typeAndYear.size && typeAndYear[index].split(",").firstOrNull() == "Movie") {
                MovieType.MOVIE
            } else {
                MovieType.TV_SHOW
            }
            val quality = if (index < qualities.size) {
                try { qualities[index].text() } catch (_: Exception) { "" }
            } else ""
            val releaseYear = if (index < typeAndYear.size) {
                typeAndYear[index].split(",").lastOrNull() ?: ""
            } else ""
            movies.add(MoviesDataModel(name, thumbnail, link, type, quality, releaseYear))
        }
        movies
    }
}
