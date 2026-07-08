package com.a1hd.movies.ui.sections.movie.adapter.episodes

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.api.repository.MovieEpisodesDataModel
import com.a1hd.movies.databinding.ItemEpisodeBinding
import com.a1hd.movies.ui.sections.movie.adapter.episodes.holder.EpisodesHolder
import javax.inject.Inject

class EpisodesRecyclerAdapter @Inject constructor(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var episodesList: MutableList<MovieEpisodesDataModel> = mutableListOf()
    private var watchedLinks: Set<String> = emptySet()
    var onEpisodeClickListener: (MovieEpisodesDataModel) -> Unit = { }
    var onEpisodeLongClickListener: (MovieEpisodesDataModel) -> Unit = { }

    fun setEpisodes(groups: MutableList<MovieEpisodesDataModel>) {
        this.episodesList = groups
        notifyDataSetChanged()
    }

    fun setWatchedLinks(links: Set<String>) {
        this.watchedLinks = links
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodesHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = (holder as EpisodesHolder)
        val model = episodesList[position]
        viewHolder.bind(model, watchedLinks.contains(model.link))

        holder.itemView.isSelected = model.isSelected
        viewHolder.select(model.isSelected, model)

        viewHolder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.isSelected = hasFocus
            viewHolder.select(hasFocus, model)
        }

        holder.itemView.setOnClickListener {
            episodesList.onEach { it.isSelected = false }
            model.isSelected = true
            episodesList.forEachIndexed { index, _ ->
                notifyItemChanged(index)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                onEpisodeClickListener.invoke(model)
            }, 500)
        }

        holder.itemView.setOnLongClickListener {
            onEpisodeLongClickListener.invoke(model)
            true
        }
    }

    override fun getItemCount(): Int = episodesList.size
}