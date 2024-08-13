package io.github.sds100.keymapper.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.assistant.AssistantTriggerFragment
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapListFragment
import io.github.sds100.keymapper.mappings.keymaps.KeyMapListFragment

/**
 * Created by sds100 on 26/01/2020.
 */

class HomePagerAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment) {

    companion object {
        val TAB_NAMES: Map<HomeTab, Int> = mapOf(
            HomeTab.KEY_EVENTS to R.string.tab_keyevents,
            HomeTab.FINGERPRINT_MAPS to R.string.tab_fingerprint,
            HomeTab.ASSISTANT_TRIGGER to R.string.tab_assistant_trigger
        )
    }

    private var tabs: List<HomeTab> = emptyList()
    private val tabFragmentsCreators: List<() -> Fragment>
        get() = tabs.map { tab ->
            when (tab) {
                HomeTab.KEY_EVENTS -> {
                    {
                        KeyMapListFragment().apply {
                            isAppBarVisible = false
                        }
                    }
                }

                HomeTab.FINGERPRINT_MAPS -> {
                    {
                        FingerprintMapListFragment().apply {
                            isAppBarVisible = false
                        }
                    }
                }

                HomeTab.ASSISTANT_TRIGGER -> {
                    {
                        AssistantTriggerFragment()
                    }
                }
            }
        }

    override fun getItemCount() = tabFragmentsCreators.size

    override fun createFragment(position: Int) = tabFragmentsCreators[position].invoke()

    fun invalidateFragments(tabs: List<HomeTab>) {
        if (this.tabs == tabs) return

        this.tabs = tabs
        notifyDataSetChanged()
    }
}
