package io.github.sds100.keymapper.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.ui.fragment.fingerprint.FingerprintMapListFragment
import io.github.sds100.keymapper.ui.fragment.keymap.KeymapListFragment

/**
 * Created by sds100 on 26/01/2020.
 */

class HomePagerAdapter(
    fragment: Fragment,
    fingerprintGesturesAvailable: Boolean
) : FragmentStateAdapter(fragment) {

    companion object {
        const val INDEX_FINGERPRINT_TAB = 1
    }

    private var tabFragmentsCreators = emptyList<() -> Fragment>()

    init {
        invalidateFragments(fingerprintGesturesAvailable)
    }

    override fun getItemCount() = tabFragmentsCreators.size

    override fun createFragment(position: Int) = tabFragmentsCreators[position].invoke()

    fun invalidateFragments(fingerprintGesturesAvailable: Boolean) {
        tabFragmentsCreators = mutableListOf<() -> Fragment>(
            {
                KeymapListFragment().apply {
                    isAppBarVisible = false
                }
            }
        ).apply {
            if (fingerprintGesturesAvailable) {
                add(INDEX_FINGERPRINT_TAB) {
                    FingerprintMapListFragment().apply {
                        isAppBarVisible = false
                    }
                }
            }
        }.toList()

        notifyDataSetChanged()
    }
}