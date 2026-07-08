package com.a1hd.movies.ui.sections.movie.adapter.season.holder

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.R
import com.a1hd.movies.api.repository.MovieSeasonDataModel
import com.a1hd.movies.databinding.ItemSeasonBinding

class SeasonsHolder(private val binding: ItemSeasonBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(movieData: MovieSeasonDataModel) {
        // Selection is shown by the red pill background; keep the text white, bold when selected.
        binding.tvSeason.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
        binding.tvSeason.setTypeface(null, if (movieData.isSelected) Typeface.BOLD else Typeface.NORMAL)
        binding.tvSeason.text = movieData.seasonNumber
    }

    fun select(hasFocus: Boolean, movieData: MovieSeasonDataModel) {
        binding.tvSeason.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
    }
}