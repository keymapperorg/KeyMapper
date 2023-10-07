package io.github.sds100.keymapper.util.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import io.github.sds100.keymapper.databinding.FragmentsTwoBinding
import io.github.sds100.keymapper.util.FragmentInfo

/**
 * Created by sds100 on 19/03/2020.
 */
abstract class TwoFragments(
    private val top: FragmentInfo,
    private val bottom: FragmentInfo
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentsTwoBinding.inflate(inflater, container, false).apply {

        topFragmentInfo = top
        addFragment(containerTop, top.instantiate)

        addFragment(containerBottom, bottom.instantiate)
        bottomFragmentInfo = bottom

    }.root

    private fun addFragment(
        container: FragmentContainerView,
        instantiateFragment: () -> Fragment
    ) {

        if (childFragmentManager.findFragmentById(container.id) == null) {
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(container.id, instantiateFragment.invoke())
                .commit()
        }
    }
}