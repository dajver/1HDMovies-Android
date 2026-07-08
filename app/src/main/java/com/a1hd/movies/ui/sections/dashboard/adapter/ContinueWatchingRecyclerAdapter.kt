package com.a1hd.movies.ui.sections.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.databinding.ItemContinueWatchingBinding
import com.a1hd.movies.db.repository.ContinueWatchingItem
import com.bumptech.glide.Glide
import javax.inject.Inject

class ContinueWatchingRecyclerAdapter @Inject constructor() : RecyclerView.Adapter<ContinueWatchingRecyclerAdapter.Holder>() {

    private var items: MutableList<ContinueWatchingItem> = mutableListOf()
    var onItemClickListener: (ContinueWatchingItem) -> Unit = { }
    var onItemLongClickListener: (ContinueWatchingItem) -> Unit = { }

    fun setItems(items: MutableList<ContinueWatchingItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = ItemContinueWatchingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnFocusChangeListener { v, hasFocus -> v.isSelected = hasFocus }
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(private val binding: ItemContinueWatchingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContinueWatchingItem) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = item.subtitle
            binding.tvSubtitle.isVisible = item.subtitle.isNotEmpty()
            if (item.thumbnail.isNotEmpty()) {
                Glide.with(itemView.context).load(item.thumbnail).into(binding.ivPoster)
            }
            itemView.setOnClickListener { onItemClickListener.invoke(item) }
            itemView.setOnLongClickListener {
                onItemLongClickListener.invoke(item)
                true
            }
        }
    }
}
