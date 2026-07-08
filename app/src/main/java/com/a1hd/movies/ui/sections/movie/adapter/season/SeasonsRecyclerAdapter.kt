package com.a1hd.movies.ui.sections.movie.adapter.season

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.api.repository.MovieSeasonDataModel
import com.a1hd.movies.databinding.ItemSeasonBinding
import com.a1hd.movies.ui.sections.movie.adapter.season.holder.SeasonsHolder
import javax.inject.Inject

class SeasonsRecyclerAdapter @Inject constructor(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var seasonsList: MutableList<MovieSeasonDataModel> = mutableListOf()
    var onSeasonClickListener: (MovieSeasonDataModel) -> Unit = { }

    fun setSeasons(groups: MutableList<MovieSeasonDataModel>) {
        this.seasonsList = groups
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ItemSeasonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeasonsHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = (holder as SeasonsHolder)
        val model = seasonsList[position]
        viewHolder.bind(model)

        // Red pill for the selected season (touch); on TV, focus overrides this so the
        // focused item is red and the playing season stays bold.
        holder.itemView.isSelected = model.isSelected

        viewHolder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.isSelected = hasFocus
            viewHolder.select(hasFocus, model)
        }

        holder.itemView.setOnClickListener {
            seasonsList.onEach { it.isSelected = false }
            model.isSelected = true
            seasonsList.forEachIndexed { index, _ ->
                notifyItemChanged(index)
            }
            onSeasonClickListener.invoke(model)
        }
    }

    override fun getItemCount(): Int = seasonsList.size
}