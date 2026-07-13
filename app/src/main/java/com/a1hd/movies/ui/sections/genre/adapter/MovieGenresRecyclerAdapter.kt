package com.a1hd.movies.ui.sections.genre.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.api.repository.MoviesDataModel
import com.a1hd.movies.databinding.ItemDashboardBinding
import com.a1hd.movies.ui.isTvDevice
import com.a1hd.movies.ui.sections.genre.adapter.holder.MovieGenreHolder
import javax.inject.Inject

class MovieGenresRecyclerAdapter @Inject constructor(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var moviesGenreList: MutableList<MoviesDataModel> = mutableListOf()

    var onMovieGenreClickListener: (MoviesDataModel) -> Unit = { }

    fun setMovies(groups: MutableList<MoviesDataModel>) {
        // The ViewModel posts the full accumulated (deduped) list each page. On a pure append
        // (pagination), insert only the new tail so existing rows — and the current D-pad focus — stay put.
        val oldSize = moviesGenreList.size
        if (isAppendOf(groups, oldSize)) {
            val inserted = groups.size - oldSize
            moviesGenreList.addAll(groups.subList(oldSize, groups.size))
            notifyItemRangeInserted(oldSize, inserted)
        } else {
            moviesGenreList.clear()
            moviesGenreList.addAll(groups)
            notifyDataSetChanged()
        }
    }

    private fun isAppendOf(groups: List<MoviesDataModel>, oldSize: Int): Boolean {
        if (oldSize == 0 || groups.size <= oldSize) return false
        for (i in 0 until oldSize) {
            if (moviesGenreList[i].link != groups[i].link) return false
        }
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ItemDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieGenreHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = (holder as MovieGenreHolder)
        val model = moviesGenreList[position]
        viewHolder.bind(model, onMovieGenreClickListener)

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

    override fun getItemCount(): Int = moviesGenreList.size
}