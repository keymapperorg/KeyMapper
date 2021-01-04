package io.github.sds100.keymapper.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.ui.fragment.IntentActionTypeFragment
import io.github.sds100.keymapper.ui.fragment.fingerprint.FingerprintMapListFragment

/**
 * Created by sds100 on 26/01/2020.
 */

class HomePagerAdapter(
    fragment: Fragment,
    fingerprintGesturesAvailable: Boolean
) : FragmentStateAdapter(fragment) {

    private var mTabFragmentsCreators = emptyList<() -> Fragment>()

    init {
        invalidateFragments(fingerprintGesturesAvailable)
    }

    override fun getItemCount() = mTabFragmentsCreators.size

    override fun createFragment(position: Int) = mTabFragmentsCreators[position].invoke()

    fun invalidateFragments(fingerprintGesturesAvailable: Boolean) {
        mTabFragmentsCreators = mutableListOf<() -> Fragment>(
            {
//                KeymapListFragment().apply {
//                    isAppBarVisible = false
//                    isInPagerAdapter = true
//                }

                IntentActionTypeFragment()
            }
        ).apply {
            if (fingerprintGesturesAvailable) {
                add {
                    FingerprintMapListFragment().apply {
                        isAppBarVisible = false
                        isInPagerAdapter = true
                    }
                }
            }
        }.toList()

        notifyDataSetChanged()
    }
}