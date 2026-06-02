package com.a1hd.movies.ui.sections.alltvshows.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.databinding.ItemDashboardBinding
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.ui.isTabletOrientation
import com.a1hd.movies.ui.sections.alltvshows.adapter.holder.AllTvShowsHolder
import javax.inject.Inject

class AllTvShowsRecyclerAdapter @Inject constructor() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var tvSHowsList: MutableList<MoviesDataModel> = mutableListOf()
    var onTvShowsClickListener: (MoviesDataModel) -> Unit = { }

    fun setTvSHows(groups: MutableList<MoviesDataModel>) {
        this.tvSHowsList.clear()
        this.tvSHowsList.addAll(groups)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ItemDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllTvShowsHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = (holder as AllTvShowsHolder)
        val model = tvSHowsList[position]
        viewHolder.bind(model, onTvShowsClickListener)

        if (holder.itemView.context.isTabletOrientation()) {
            if (model.isSelected) {
                viewHolder.itemView.isSelected = model.isSelected
                viewHolder.itemView.requestFocus()
            }
            viewHolder.itemView.setOnFocusChangeListener { v, hasFocus ->
                v.isSelected = hasFocus
            }
        }
    }

    override fun getItemCount(): Int = tvSHowsList.size
}