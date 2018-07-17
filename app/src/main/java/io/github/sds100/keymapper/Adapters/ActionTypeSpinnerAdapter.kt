package io.github.sds100.keymapper.Adapters

import android.content.Context
import android.widget.ArrayAdapter
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
        private val ORDERED_ITEM_TYPE_LIST = listOf(
                Item.APP,
                Item.APP_SHORTCUT,
                Item.KEY,
                Item.ACTION,
                Item.SETTING,
                Item.TEXT_BLOCK,
                Item.KEYCODE
        )

        fun getItemTypeFromPosition(position: Int): Item {
            return ORDERED_ITEM_TYPE_LIST[position]
        }
    }

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    /**
     *
     */
    enum class Item {
        APP, APP_SHORTCUT, KEY, ACTION, SETTING, TEXT_BLOCK, KEYCODE
    }
}