package com.a1hd.movies.ui.sections.notifications.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.a1hd.movies.R
import com.a1hd.movies.databinding.ItemNotificationBinding
import com.a1hd.movies.db.entity.ShowNotificationEntity
import com.bumptech.glide.Glide
import javax.inject.Inject

class NotificationsRecyclerAdapter @Inject constructor() : RecyclerView.Adapter<NotificationsRecyclerAdapter.Holder>() {

    private var items: MutableList<ShowNotificationEntity> = mutableListOf()
    var onItemClickListener: (ShowNotificationEntity) -> Unit = { }

    fun setItems(items: MutableList<ShowNotificationEntity>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnFocusChangeListener { v, hasFocus -> v.isSelected = hasFocus }
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShowNotificationEntity) {
            val context = itemView.context
            binding.tvShowName.text = item.showName
            binding.tvNewCount.text = context.getString(R.string.new_episodes_count, item.newCount)
            binding.tvLatest.text = context.getString(R.string.latest_episode, item.latestEpisodeLabel)
            binding.tvTime.text = DateUtils.getRelativeTimeSpanString(item.detectedAt)
            if (item.thumbnail.isNotEmpty()) {
                Glide.with(context).load(item.thumbnail).into(binding.ivThumbnail)
            }
            itemView.setOnClickListener { onItemClickListener.invoke(item) }
        }
    }
}
