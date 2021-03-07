package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import io.github.sds100.keymapper.databinding.FragmentsFourBinding
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 19/03/2020.
 */
abstract class FourFragments(
    private val topLeft: Pair<Int, Class<out Fragment>>,
    private val topRight: Pair<Int, Class<out Fragment>>,
    private val bottomLeft: Pair<Int, Class<out Fragment>>,
    private val bottomRight: Pair<Int, Class<out Fragment>>) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentsFourBinding.inflate(inflater, container, false).apply {

            topLeftFragmentHeader = str(topLeft.first)
            addFragment(containerTopLeft, topLeft.second)

            topRightFragmentHeader = str(topRight.first)
            addFragment(containerTopRight, topRight.second)

            bottomLeftFragmentHeader = str(bottomLeft.first)
            addFragment(containerBottomLeft, bottomLeft.second)

            bottomRightFragmentHeader = str(bottomRight.first)
            addFragment(containerBottomRight, bottomRight.second)

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
