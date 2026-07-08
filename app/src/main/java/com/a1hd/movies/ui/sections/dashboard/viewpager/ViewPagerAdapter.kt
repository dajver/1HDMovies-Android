package com.a1hd.movies.ui.sections.dashboard.viewpager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewpager.widget.PagerAdapter
import com.a1hd.movies.databinding.ItemMostPopularBinding
import com.a1hd.movies.api.repository.MostPopularMoviesDataModel
import com.bumptech.glide.Glide

internal class ViewPagerAdapter(private val context: Context) : PagerAdapter() {

    private var mostPopularMovieList: MutableList<MostPopularMoviesDataModel> = mutableListOf()
    var onMostPopularMovieClickListener: (MostPopularMoviesDataModel) -> Unit = {}

    fun setImages(images: MutableList<MostPopularMoviesDataModel>) {
        this.mostPopularMovieList = images
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mostPopularMovieList.size
    }

    override fun isViewFromObject(view: View, something: Any): Boolean {
        return view === something as FrameLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val binding = ItemMostPopularBinding.inflate(LayoutInflater.from(container.context), container, false)
        val model = mostPopularMovieList[position]
        Glide.with(context).load(model.thumbnail).into(binding.ivPoster)
        binding.tvTitle.text = model.name
        binding.tvDescription.text = model.description
        binding.tvQuality.text = model.quality.replace(", ", " · ")
        binding.root.setOnClickListener {
            onMostPopularMovieClickListener.invoke(model)
        }
        container.addView(binding.root)
        return binding.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, something: Any) {
        container.removeView(something as FrameLayout)
    }
}