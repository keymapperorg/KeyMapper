package io.github.sds100.keymapper.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.ui.fragment.FingerprintGestureFragment
import io.github.sds100.keymapper.ui.fragment.KeymapListFragment

/**
 * Created by sds100 on 26/01/2020.
 */

class HomePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val mTabFragmentsCreators: List<() -> Fragment>
        get() = mutableListOf<() -> Fragment>(
            {
                KeymapListFragment().apply {
                    isAppBarVisible = false
                    isInPagerAdapter = true
                }
            }
        ).apply {
            if (AppPreferences.isFingerprintGestureDetectionAvailable) {
                add { FingerprintGestureFragment() }
            }
        }

    override fun getItemCount() = mTabFragmentsCreators.size

    override fun createFragment(position: Int) = mTabFragmentsCreators[position].invoke()
}