package io.github.sds100.keymapper.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.ui.fragment.*

/**
 * Created by sds100 on 26/01/2020.
 */

class ChooseActionPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val mTabFragmentsCreators: Array<() -> Fragment> = arrayOf(
        {
            AppListFragment().apply {
                isAppBarVisible = false
                isInPagerAdapter = true
            }
        },
        {
            AppShortcutListFragment().apply {
                isAppBarVisible = false
                isInPagerAdapter = true
            }
        },
        {
            KeyActionTypeFragment()
        },
        {
            KeycodeListFragment().apply {
                isAppBarVisible = false
                isInPagerAdapter = true
            }
        },
        {
            TextBlockActionTypeFragment()
        },
        {
            UrlActionTypeFragment()
        }
    )

    fun getSearchStateKey(position: Int): String? = when (position) {
        0 -> AppListFragment.SEARCH_STATE_KEY
        1 -> AppShortcutListFragment.SEARCH_STATE_KEY
        3 -> KeycodeListFragment.SEARCH_STATE_KEY

        else -> null
    }

    override fun getItemCount() = mTabFragmentsCreators.size

    override fun createFragment(position: Int) = mTabFragmentsCreators[position].invoke()
}