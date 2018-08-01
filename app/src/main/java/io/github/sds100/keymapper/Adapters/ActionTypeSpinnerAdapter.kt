package io.github.sds100.keymapper.Adapters

import android.content.Context
import android.widget.ArrayAdapter
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 16/07/2018.
 */
class ActionTypeSpinnerAdapter(
        context: Context
) : ArrayAdapter<String>(
        context,
        android.R.layout.simple_spinner_item,
        context.resources.getStringArray(R.array.spinner_action_type_array)
) {

    companion object {
        /**
         * Must be the same order as [R.array.spinner_action_type_array]
         */
        private val ORDERED_ACTION_TYPE_LIST = listOf(
                Action.TYPE_APP,
                Action.TYPE_APP_SHORTCUT,
                Action.TYPE_KEY,
                Action.TYPE_KEYCODE,
                Action.TYPE_SYSTEM_ACTION,
                Action.TYPE_TEXT_BLOCK
                )

        fun getActionTypeFromPosition(position: Int): Int {
            return ORDERED_ACTION_TYPE_LIST[position]
        }
    }

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
}