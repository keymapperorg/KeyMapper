package io.github.sds100.keymapper.util

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Created by sds100 on 26/01/2020.
 */

class GenericFragmentPagerAdapter(
    fragment: Fragment,
    private val fragmentCreatorsList: List<Pair<Long, () -> Fragment>>
) : FragmentStateAdapter(fragment) {

    private val itemIds = fragmentCreatorsList.map { it.first }

    override fun getItemCount() = fragmentCreatorsList.size

    override fun createFragment(position: Int): Fragment {
        return fragmentCreatorsList[position].second.invoke()
    }

    override fun getItemId(position: Int): Long {
        return itemIds[position]
    }

    override fun containsItem(itemId: Long): Boolean {
        return itemIds.contains(itemId)
    }
}