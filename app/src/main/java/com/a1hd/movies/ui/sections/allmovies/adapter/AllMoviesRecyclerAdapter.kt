package com.a1hd.movies.ui.sections.allmovies.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.databinding.ItemDashboardBinding
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.ui.isTvDevice
import com.a1hd.movies.ui.sections.allmovies.adapter.holder.AllMoviesHolder
import javax.inject.Inject

class AllMoviesRecyclerAdapter @Inject constructor(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var moviesList: MutableList<MoviesDataModel> = mutableListOf()

    var onMovieClickListener: (MoviesDataModel) -> Unit = { }

    fun setMovies(groups: MutableList<MoviesDataModel>) {
        // The ViewModel posts the full accumulated list. When it's a pure append (pagination),
        // insert only the new tail so existing rows — and the current D-pad focus — stay put.
        val oldSize = moviesList.size
        if (isAppendOf(groups, oldSize)) {
            val inserted = groups.size - oldSize
            moviesList.addAll(groups.subList(oldSize, groups.size))
            notifyItemRangeInserted(oldSize, inserted)
        } else {
            moviesList.clear()
            moviesList.addAll(groups)
            notifyDataSetChanged()
        }
    }

    private fun isAppendOf(groups: List<MoviesDataModel>, oldSize: Int): Boolean {
        if (oldSize == 0 || groups.size <= oldSize) return false
        for (i in 0 until oldSize) {
            if (moviesList[i].link != groups[i].link) return false
        }
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ItemDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllMoviesHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = (holder as AllMoviesHolder)
        val model = moviesList[position]
        viewHolder.bind(model, onMovieClickListener)

        if (holder.itemView.context.isTvDevice()) {
            if (model.isSelected) {
                viewHolder.itemView.isSelected = model.isSelected
                viewHolder.itemView.requestFocus()
            }
            viewHolder.itemView.setOnFocusChangeListener { v, hasFocus ->
                v.isSelected = hasFocus
            }
        }
    }

    override fun getItemCount(): Int = moviesList.size
}