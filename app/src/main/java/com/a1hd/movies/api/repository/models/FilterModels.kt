package com.a1hd.movies.api.repository.models

import java.net.URLEncoder

data class FilterOptions(
    var type: MutableSet<FilterType> = mutableSetOf(),
    var genre: String = "",
    var country: String = "",
    var year: String = "",
    var sort: FilterSort = FilterSort.DEFAULT
) {
    val queryString: String
        get() {
            val params = mutableListOf<String>()
            if (type.isNotEmpty()) {
                val typeValues = type.map { it.value }.sorted().joinToString(",")
                params.add("type=$typeValues")
            }
            if (genre.isNotEmpty()) {
                params.add("genre=${URLEncoder.encode(genre, "UTF-8")}")
            }
            if (country.isNotEmpty()) {
                params.add("country=${URLEncoder.encode(country, "UTF-8")}")
            }
            if (year.isNotEmpty()) {
                params.add("year=$year")
            }
            params.add("sort=${sort.value}")
            return params.joinToString("&")
        }

    val isEmpty: Boolean
        get() = type.isEmpty() && genre.isEmpty() && country.isEmpty() && year.isEmpty() && sort == FilterSort.DEFAULT

    fun reset() {
        type.clear()
        genre = ""
        country = ""
        year = ""
        sort = FilterSort.DEFAULT
    }
}

enum class FilterType(val value: String, val displayName: String) {
    MOVIE("2", "Movie"),
    TV_SERIES("1", "TV Series")
}

enum class FilterSort(val value: String, val displayName: String) {
    DEFAULT("default", "Default"),
    LAST_UPDATED("last_updated", "Last Updated")
}

object FilterData {
    val genres = listOf(
        "All", "Drama", "Comedy", "Thriller", "Action", "Romance", "Horror", "Crime",
        "Documentary", "Adventure", "Mystery", "Fantasy", "Family", "Science Fiction",
        "TV Movie", "Animation", "History", "Music", "War", "Western"
    )

    val countries = listOf(
        "United States of America", "United Kingdom", "France", "Canada", "Germany",
        "Japan", "Italy", "India", "Spain", "Australia", "Hong Kong", "South Korea",
        "China", "Belgium", "Sweden", "Mexico", "Denmark", "Ireland", "Poland", "Russia",
        "Netherlands", "Brazil", "Norway", "Argentina", "South Africa", "Finland",
        "Switzerland", "New Zealand", "Austria", "Thailand", "Turkey", "Hungary",
        "Czech Republic", "Taiwan", "Israel", "Romania", "Philippines", "Portugal",
        "Greece", "Chile", "Indonesia", "Iceland", "Colombia", "Ukraine", "Singapore",
        "Serbia", "Nigeria", "Malaysia", "Iran"
    )

    val years: List<String> by lazy {
        val result = mutableListOf("")
        for (year in 2025 downTo 1970) {
            result.add(year.toString())
        }
        result
    }
}
