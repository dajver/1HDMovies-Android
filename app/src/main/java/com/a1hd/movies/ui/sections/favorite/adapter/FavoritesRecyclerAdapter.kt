package com.a1hd.movies.ui.sections.favorite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.api.repository.MoviesDetailsDataModel
import com.a1hd.movies.databinding.ItemDashboardBinding
import com.a1hd.movies.ui.isTvDevice
import com.a1hd.movies.ui.sections.favorite.adapter.holder.FavoritesHolder
import javax.inject.Inject

class FavoritesRecyclerAdapter @Inject constructor(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var favoritesList: MutableList<MoviesDetailsDataModel> = mutableListOf()

    var onFavoriteClickListener: (MoviesDetailsDataModel) -> Unit = { }

    fun setFavorites(groups: MutableList<MoviesDetailsDataModel>) {
        this.favoritesList = groups
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ItemDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoritesHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = (holder as FavoritesHolder)
        viewHolder.bind(favoritesList[position], onFavoriteClickListener)

        if (holder.itemView.context.isTvDevice()) {
            viewHolder.itemView.setOnFocusChangeListener { v, hasFocus ->
                v.isSelected = hasFocus
            }
        }
    }

    override fun getItemCount(): Int = favoritesList.size
}