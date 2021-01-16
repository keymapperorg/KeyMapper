package io.github.sds100.keymapper.ui.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding

/**
 * Created by sds100 on 22/02/2020.
 */
abstract class DefaultRecyclerViewFragment<T>
    : RecyclerViewFragment<T, FragmentRecyclerviewBinding>() {

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentRecyclerviewBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        modelState.viewState.observe(viewLifecycleOwner, {
            binding.viewState = it
        })
    }

    override fun getBottomAppBar(binding: FragmentRecyclerviewBinding) = binding.appBar
}