package com.a1hd.movies.ui.sections.filter.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.R
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.a1hd.movies.api.repository.models.FilterData
import com.a1hd.movies.api.repository.models.FilterOptions
import com.a1hd.movies.api.repository.models.FilterSort
import com.a1hd.movies.databinding.ViewFilterBarBinding

class FilterBarView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding: ViewFilterBarBinding
    var filters = FilterOptions()
        private set
    var onFiltersChanged: (() -> Unit)? = null

    private val activeTint = ColorStateList.valueOf(Color.parseColor("#CCE53935"))
    private val inactiveTint = ColorStateList.valueOf(Color.parseColor("#44FFFFFF"))

    init {
        binding = ViewFilterBarBinding.inflate(LayoutInflater.from(context), this, true)
        setupGenreButton()
        setupCountryButton()
        setupYearButton()
        setupSortButton()
        setupResetButton()
    }

    private fun showPickerDialog(title: String, items: Array<String>, onPick: (Int) -> Unit) {
        AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(title)
            .setItems(items) { _, which -> onPick(which) }
            .show()
    }

    private fun setupGenreButton() {
        binding.btnGenre.setOnClickListener {
            val items = arrayOf(context.getString(com.a1hd.movies.R.string.all)) + FilterData.genres.filter { it != "All" }
            showPickerDialog(context.getString(com.a1hd.movies.R.string.genre), items) { which ->
                filters.genre = if (which == 0) "" else items[which]
                updateChipStates()
                onFiltersChanged?.invoke()
            }
        }
    }

    private fun setupCountryButton() {
        binding.btnCountry.setOnClickListener {
            val items = arrayOf(context.getString(com.a1hd.movies.R.string.all)) + FilterData.countries
            showPickerDialog(context.getString(com.a1hd.movies.R.string.country), items) { which ->
                filters.country = if (which == 0) "" else items[which]
                updateChipStates()
                onFiltersChanged?.invoke()
            }
        }
    }

    private fun setupYearButton() {
        binding.btnYear.setOnClickListener {
            val items = arrayOf(context.getString(com.a1hd.movies.R.string.all)) + FilterData.years.filter { it.isNotEmpty() }
            showPickerDialog(context.getString(com.a1hd.movies.R.string.year), items) { which ->
                filters.year = if (which == 0) "" else items[which]
                updateChipStates()
                onFiltersChanged?.invoke()
            }
        }
    }

    private fun setupSortButton() {
        binding.btnSort.setOnClickListener {
            val items = FilterSort.entries.map { it.displayName }.toTypedArray()
            showPickerDialog(context.getString(com.a1hd.movies.R.string.sort), items) { which ->
                filters.sort = FilterSort.entries[which]
                updateChipStates()
                onFiltersChanged?.invoke()
            }
        }
    }

    private fun setupResetButton() {
        binding.btnReset.setOnClickListener {
            filters.genre = ""
            filters.country = ""
            filters.year = ""
            filters.sort = FilterSort.DEFAULT
            updateChipStates()
            onFiltersChanged?.invoke()
        }
    }

    fun setFilterOptions(options: FilterOptions) {
        filters = options
        updateChipStates()
    }

    val hasActiveFilters: Boolean
        get() = filters.genre.isNotEmpty() || filters.country.isNotEmpty() ||
                filters.year.isNotEmpty() || filters.sort != FilterSort.DEFAULT

    private fun updateChipStates() {
        updateChip(binding.btnGenre, context.getString(com.a1hd.movies.R.string.genre), filters.genre)
        updateChip(binding.btnCountry, context.getString(com.a1hd.movies.R.string.country), filters.country)
        updateChip(binding.btnYear, context.getString(com.a1hd.movies.R.string.year), filters.year)
        val sortValue = if (filters.sort == FilterSort.DEFAULT) "" else filters.sort.displayName
        updateChip(binding.btnSort, context.getString(com.a1hd.movies.R.string.sort), sortValue)
        binding.btnReset.isVisible = hasActiveFilters
    }

    private fun updateChip(button: Button, label: String, value: String) {
        if (value.isNotEmpty()) {
            button.text = value
            button.backgroundTintList = activeTint
            button.setTextColor(Color.WHITE)
        } else {
            button.text = label
            button.backgroundTintList = inactiveTint
            button.setTextColor(Color.parseColor("#AAAAAA"))
        }
    }
}