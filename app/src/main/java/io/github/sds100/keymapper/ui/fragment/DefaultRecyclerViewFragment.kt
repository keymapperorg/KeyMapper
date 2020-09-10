package io.github.sds100.keymapper.ui.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding

/**
 * Created by sds100 on 22/02/2020.
 */
abstract class DefaultRecyclerViewFragment : RecyclerViewFragment<FragmentRecyclerviewBinding>() {

    override val appBar: BottomAppBar
        get() = binding.appBar

    open val noItemsText: String? = null
    open val itemCount: LiveData<Int> = MutableLiveData()

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentRecyclerviewBinding.inflate(inflater, container, false).apply {
            progressCallback = this.progressCallback
            lifecycleOwner = viewLifecycleOwner

            noItemsText = this@DefaultRecyclerViewFragment.noItemsText

            this@DefaultRecyclerViewFragment.itemCount.observe(viewLifecycleOwner, {
                itemCount = it
            })
        }
}