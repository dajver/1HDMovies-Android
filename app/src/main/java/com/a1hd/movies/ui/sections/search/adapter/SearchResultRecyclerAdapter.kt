package com.a1hd.movies.ui.sections.search.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.databinding.ItemDashboardBinding
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.ui.isTvDevice
import com.a1hd.movies.ui.sections.search.adapter.holder.SearchResultHolder
import javax.inject.Inject

class SearchResultRecyclerAdapter @Inject constructor() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var searchResultList: MutableList<MoviesDataModel> = mutableListOf()
    var onSearchResultClickListener: (MoviesDataModel) -> Unit = { }

    fun setSearchResult(groups: MutableList<MoviesDataModel>) {
        this.searchResultList = groups
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ItemDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchResultHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = (holder as SearchResultHolder)
        val model = searchResultList[position]
        viewHolder.bind(model, onSearchResultClickListener)

        if (holder.itemView.context.isTvDevice()) {
            if (model.isSelected) {
                viewHolder.itemView.isSelected = model.isSelected
//                viewHolder.itemView.requestFocus()
            }
            viewHolder.itemView.setOnFocusChangeListener { v, hasFocus ->
                v.isSelected = hasFocus
            }
        }
    }

    override fun getItemCount(): Int = searchResultList.size
}