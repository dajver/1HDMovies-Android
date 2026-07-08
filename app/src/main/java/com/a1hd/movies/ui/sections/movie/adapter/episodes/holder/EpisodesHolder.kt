package com.a1hd.movies.ui.sections.movie.adapter.episodes.holder

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.R
import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.databinding.ItemEpisodeBinding

class EpisodesHolder(private val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(movieData: MovieEpisodesDataModel, isWatched: Boolean = false) {
        binding.ivWatched.isVisible = isWatched
        // Selection is shown by the red pill background; keep the text white, bold when selected.
        binding.tvEpisode.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
        binding.tvEpisode.setTypeface(null, if (movieData.isSelected) Typeface.BOLD else Typeface.NORMAL)
        binding.tvEpisode.text = movieData.episodeNumber
        binding.tvEpisodeName.text = movieData.episodeName
    }

    fun select(hasFocus: Boolean, movieData: MovieEpisodesDataModel) {
        binding.tvEpisode.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
    }
}