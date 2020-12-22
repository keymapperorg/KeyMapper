package io.github.sds100.keymapper.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Created by sds100 on 26/01/2020.
 */

class GenericFragmentPagerAdapter(fragment: Fragment,
                                  fragmentCreatorsWithId: List<Pair<Int, () -> Fragment>>
) : FragmentStateAdapter(fragment) {

    private val mItemIds = fragmentCreatorsWithId.map { it.first.toLong() }
    private val mFragmentCreatorsList = fragmentCreatorsWithId.map { it.second }

    override fun getItemCount() = mFragmentCreatorsList.size

    override fun createFragment(position: Int): Fragment {
        return mFragmentCreatorsList[position].invoke()
    }

    override fun getItemId(position: Int): Long {
        return mItemIds[position]
    }

    override fun containsItem(itemId: Long): Boolean {
        return mItemIds.contains(itemId)
    }
}