package io.github.sds100.keymapper.ui.adapter

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.ui.fragment.*

/**
 * Created by sds100 on 26/01/2020.
 */

class ChooseActionPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val mTabFragmentsCreators: List<() -> Fragment> = mutableListOf(
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
            KeycodeListFragment().apply {
                isAppBarVisible = false
                isInPagerAdapter = true
            }
        },
        {
            KeyActionTypeFragment()
        },

        {
            KeyEventActionTypeFragment()
        },
        {
            TextBlockActionTypeFragment()
        },
        {
            UrlActionTypeFragment()
        },
        {
            SystemActionListFragment().apply {
                isAppBarVisible = false
                isInPagerAdapter = true
            }
        },
        {
            UnsupportedActionListFragment().apply {
                isAppBarVisible = false
                isInPagerAdapter = true
            }
        }
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            add(5) { TapCoordinateActionTypeFragment() }
        }
    }

    fun getSearchStateKey(position: Int): String? = when (position) {
        0 -> AppListFragment.SEARCH_STATE_KEY
        1 -> AppShortcutListFragment.SEARCH_STATE_KEY
        2 -> KeycodeListFragment.SEARCH_STATE_KEY
        6 -> SystemActionListFragment.SEARCH_STATE_KEY

        else -> null
    }

    override fun getItemCount() = mTabFragmentsCreators.size

    override fun createFragment(position: Int) = mTabFragmentsCreators[position].invoke()
}