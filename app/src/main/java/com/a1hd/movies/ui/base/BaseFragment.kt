package com.a1hd.movies.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.a1hd.movies.R
import com.a1hd.movies.ui.navigation.NavigationRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import javax.inject.Inject

typealias FragmentInflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

abstract class BaseFragment<VB: ViewBinding>(
    private val inflate: FragmentInflate<VB>
) : Fragment(), CoroutineScope by MainScope() {

    @Inject
    lateinit var navigationRouter: NavigationRouter

    private var _binding: VB? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Any screen that includes a @+id/btnBack gets a working back button for free.
        view.findViewById<View?>(R.id.btnBack)?.setOnClickListener {
            navigationRouter.navigateBack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        _binding = null
    }
}