package io.github.sds100.keymapper.ui.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ui.fragment.ActionsFragment
import io.github.sds100.keymapper.ui.fragment.ConstraintsAndMoreFragment
import io.github.sds100.keymapper.ui.fragment.TriggerAndActionsFragment
import io.github.sds100.keymapper.ui.fragment.TriggerFragment
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.intArray

/**
 * Created by sds100 on 26/01/2020.
 */

class ConfigKeymapPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    /**
     * Mapping of the ViewPager page indexes to their respective Fragments
     */
    private val tabFragmentsCreators = mutableMapOf<Int, () -> Fragment>()
    private val itemIds = mutableMapOf<Int, Long>()

    init {
        createFragmentMap(fragment.requireContext())
    }

    override fun getItemCount() = tabFragmentsCreators.size

    override fun createFragment(position: Int): Fragment {
        return tabFragmentsCreators[position]?.invoke() ?: throw IndexOutOfBoundsException()
    }

    override fun getItemId(position: Int): Long {
        return itemIds[position]!!
    }

    override fun containsItem(itemId: Long): Boolean {
        return itemIds.containsValue(itemId)
    }

    private fun createFragmentMap(ctx: Context) {
        val fragmentIds = ctx.intArray(R.array.config_keymap_fragments)

        fragmentIds.forEachIndexed { index, id ->

            val entry = when (id) {
                ctx.int(R.integer.fragment_id_trigger_and_actions) -> ::TriggerAndActionsFragment

                ctx.int(R.integer.fragment_id_constraints_and_more) -> ::ConstraintsAndMoreFragment

                ctx.int(R.integer.fragment_id_actions) -> ::ActionsFragment

                ctx.int(R.integer.fragment_id_trigger) -> ::TriggerFragment

                else -> throw Exception("Don't know how to instantiate a fragment for this id $id")
            }

            itemIds[index] = id.toLong()
            tabFragmentsCreators[index] = entry
        }
    }
}