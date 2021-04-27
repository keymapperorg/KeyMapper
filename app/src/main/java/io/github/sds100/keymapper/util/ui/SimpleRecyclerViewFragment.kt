package io.github.sds100.keymapper.util.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding

/**
 * Created by sds100 on 22/02/2020.
 */
abstract class SimpleRecyclerViewFragment<T>
    : RecyclerViewFragment<T, FragmentSimpleRecyclerviewBinding>() {

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSimpleRecyclerviewBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {}

    override fun getProgressBar(binding: FragmentSimpleRecyclerviewBinding) = binding.progressBar
    override fun getRecyclerView(binding: FragmentSimpleRecyclerviewBinding) =
        binding.epoxyRecyclerView

    override fun getEmptyListPlaceHolder(binding: FragmentSimpleRecyclerviewBinding) =
        binding.emptyListPlaceHolder

    override fun getBottomAppBar(binding: FragmentSimpleRecyclerviewBinding): BottomAppBar? {
        if (isAppBarVisible) {
            //only inflate a menu if the app bar is visible because this takes a significant amount of time
            binding.appBar.replaceMenu(R.menu.menu_recyclerview_fragment)
            binding.appBar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        }

        return binding.appBar
    }
}