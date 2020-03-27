package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.setLiveData
import java.io.Serializable

/**
 * Created by sds100 on 22/02/2020.
 */
abstract class RecyclerViewFragment : Fragment() {

    open val savedStateKey: String? = null

    open val progressCallback: ProgressCallback? = null
    var isAppBarVisible = true
    var isInPagerAdapter = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FragmentRecyclerviewBinding.inflate(inflater, container, false).apply {

            progressCallback = this@RecyclerViewFragment.progressCallback
            lifecycleOwner = viewLifecycleOwner

            appBar.isVisible = isAppBarVisible
            appBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            requireActivity().onBackPressedDispatcher.addCallback {
                findNavController().navigateUp()
            }

            subscribeList(this)

            return this.root
        }
    }

    fun <T : Serializable> selectModel(model: T) {
        findNavController().apply {
            if (savedStateKey != null) {
                // this livedata could be observed from a fragment on the backstack or in the same position on the
                // backstack as this fragment
                if (isInPagerAdapter) {
                    currentBackStackEntry?.setLiveData(savedStateKey!!, model)
                } else {
                    previousBackStackEntry?.setLiveData(savedStateKey!!, model)
                    navigateUp()
                }
            }
        }
    }

    abstract fun subscribeList(binding: FragmentRecyclerviewBinding)
}