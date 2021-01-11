package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import io.github.sds100.keymapper.databinding.FragmentsFourBinding
import io.github.sds100.keymapper.util.UrlUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 19/03/2020.
 */
abstract class FourFragments(
    private val topLeft: Triple<Int, Class<out Fragment>, Int>,
    private val topRight: Triple<Int, Class<out Fragment>, Int>,
    private val bottomLeft: Triple<Int, Class<out Fragment>, Int>,
    private val bottomRight: Triple<Int, Class<out Fragment>, Int>) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentsFourBinding.inflate(inflater, container, false).apply {

            topLeftFragmentHeader = str(topLeft.first)
            addFragment(containerTopLeft, topLeft.second)
            setOnTopLeftFragmentHelpClick {
                UrlUtils.launchCustomTab(
                    requireContext(),
                    str(topLeft.third)
                )
            }

            topRightFragmentHeader = str(topRight.first)
            addFragment(containerTopRight, topRight.second)
            setOnTopRightFragmentHelpClick {
                UrlUtils.launchCustomTab(
                    requireContext(),
                    str(topRight.third)
                )
            }

            bottomLeftFragmentHeader = str(bottomLeft.first)
            addFragment(containerBottomLeft, bottomLeft.second)
            setOnBottomLeftFragmentHelpClick {
                UrlUtils.launchCustomTab(
                    requireContext(),
                    str(bottomLeft.third)
                )
            }

            bottomRightFragmentHeader = str(bottomRight.first)
            addFragment(containerBottomRight, bottomRight.second)
            setOnBottomRightFragmentHelpClick {
                UrlUtils.launchCustomTab(
                    requireContext(),
                    str(bottomRight.third)
                )
            }

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
