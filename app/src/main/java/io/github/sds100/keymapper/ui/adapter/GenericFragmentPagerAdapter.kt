package io.github.sds100.keymapper.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Created by sds100 on 26/01/2020.
 */

class GenericFragmentPagerAdapter(fragment: Fragment,
                                  fragmentCreatorsWithId: List<Pair<Int, () -> Fragment>>
) : FragmentStateAdapter(fragment) {

    private val itemIds = fragmentCreatorsWithId.map { it.first.toLong() }
    private val fragmentCreatorsList = fragmentCreatorsWithId.map { it.second }

    override fun getItemCount() = fragmentCreatorsList.size

    override fun createFragment(position: Int): Fragment {
        return fragmentCreatorsList[position].invoke()
    }

    override fun getItemId(position: Int): Long {
        return itemIds[position]
    }

    override fun containsItem(iteid: Long): Boolean {
        return itemIds.contains(iteid)
    }
}