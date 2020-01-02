package io.github.sds100.keymapper.fragment

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.SimpleRecyclerViewItem
import io.github.sds100.keymapper.adapter.SimpleItemAdapter
import io.github.sds100.keymapper.util.ErrorCodeUtils
import io.github.sds100.keymapper.util.SystemActionUtils
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 20/07/2019.
 */

class UnsupportedActionsFragment : RecyclerViewFragment() {
    override val adapter by lazy {
        val items = sequence {
            SystemActionUtils.getUnsupportedSystemActionsWithReasons(context!!).forEach {
                val systemActionDef = it.first
                val reason = it.second

                val text = systemActionDef.getDescription(context!!)
                val secondaryText = ErrorCodeUtils.getErrorCodeDescription(context!!, reason!!)

                val icon = systemActionDef.getIcon(context!!)

                yield(SimpleRecyclerViewItem(text, secondaryText, icon))
            }
        }.toList()

        SimpleItemAdapter().apply {
            setItems(items)
        }
    }
}