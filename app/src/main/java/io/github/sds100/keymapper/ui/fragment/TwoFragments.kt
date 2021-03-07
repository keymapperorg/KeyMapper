package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import io.github.sds100.keymapper.databinding.FragmentsTwoBinding
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 19/03/2020.
 */
abstract class TwoFragments(
    private val top: Pair<Int, Class<out Fragment>>,
    private val bottom: Pair<Int, Class<out Fragment>>) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentsTwoBinding.inflate(inflater, container, false).apply {

            topFragmentHeader = str(top.first)
            addFragment(containerTop, top.second)

            bottomFragmentHeader = str(bottom.first)
            addFragment(containerBottom, bottom.second)

        }.root
    }

    private fun addFragment(
        container: FragmentContainerView,
        fragmentClass: Class<out Fragment>) {

        if (childFragmentManager.findFragmentById(container.id) == null) {
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(container.id, fragmentClass, bundleOf())
                .commit()
        }
    }
}
