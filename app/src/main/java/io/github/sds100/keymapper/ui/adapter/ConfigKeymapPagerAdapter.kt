package io.github.sds100.keymapper.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.ui.fragment.AppListFragment
import io.github.sds100.keymapper.ui.fragment.ConstraintsAndMoreFragment
import io.github.sds100.keymapper.ui.fragment.TriggerAndActionsFragment

/**
 * Created by sds100 on 26/01/2020.
 */

class ConfigKeymapPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    /**
     * Mapping of the ViewPager page indexes to their respective Fragments
     */
    private val tabFragmentsCreators: Map<Int, () -> Fragment> = mapOf(
        0 to { TriggerAndActionsFragment() },
        1 to { ConstraintsAndMoreFragment() }
    )

    override fun getItemCount() = tabFragmentsCreators.size

    override fun createFragment(position: Int): Fragment {
        return tabFragmentsCreators[position]?.invoke() ?: throw IndexOutOfBoundsException()
    }
}