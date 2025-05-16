package io.github.sds100.keymapper.util.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.util.str


abstract class SimpleRecyclerViewFragment<T> : RecyclerViewFragment<T, FragmentSimpleRecyclerviewBinding>() {

    @MenuRes
    open val appBarMenu: Int = R.menu.menu_recyclerview_fragment

    @StringRes
    open val emptyListPlaceholder: Int = R.string.recyclerview_placeholder

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSimpleRecyclerviewBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        binding.emptyListPlaceholder = str(emptyListPlaceholder)

        if (isAppBarVisible) {
            // only inflate a menu if the app bar is visible because this takes a significant amount of time
            binding.appBar.replaceMenu(appBarMenu)
            binding.appBar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        }
    }

    override fun getProgressBar(binding: FragmentSimpleRecyclerviewBinding) = binding.progressBar
    override fun getRecyclerView(binding: FragmentSimpleRecyclerviewBinding) =
        binding.epoxyRecyclerView

    override fun getEmptyListPlaceHolderTextView(binding: FragmentSimpleRecyclerviewBinding) =
        binding.textViewEmptyListPlaceholder

    override fun getBottomAppBar(binding: FragmentSimpleRecyclerviewBinding): BottomAppBar? =
        binding.appBar
}
