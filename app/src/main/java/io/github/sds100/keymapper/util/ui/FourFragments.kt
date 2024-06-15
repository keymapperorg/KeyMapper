package io.github.sds100.keymapper.util.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import io.github.sds100.keymapper.databinding.FragmentsFourBinding
import io.github.sds100.keymapper.util.FragmentInfo

/**
 * Created by sds100 on 19/03/2020.
 */
abstract class FourFragments(
    private val topLeft: FragmentInfo,
    private val topRight: FragmentInfo,
    private val bottomLeft: FragmentInfo,
    private val bottomRight: FragmentInfo,
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = FragmentsFourBinding.inflate(inflater, container, false).apply {
        addFragment(containerTopLeft, topLeft.instantiate)
        topLeftFragmentInfo = topLeft

        addFragment(containerTopRight, topRight.instantiate)
        topRightFragmentInfo = topRight

        addFragment(containerBottomLeft, bottomLeft.instantiate)
        bottomLeftFragmentInfo = bottomLeft

        addFragment(containerBottomRight, bottomRight.instantiate)
        bottomRightFragmentInfo = bottomRight
    }.root

    private fun addFragment(
        container: FragmentContainerView,
        instantiate: () -> Fragment,
    ) {
        if (childFragmentManager.findFragmentById(container.id) == null) {
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(container.id, instantiate.invoke())
                .commit()
        }
    }
}
