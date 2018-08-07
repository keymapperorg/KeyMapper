package io.github.sds100.keymapper.Adapters

import android.content.Context
import android.widget.ArrayAdapter
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 16/07/2018.
 */
class ActionTypeSpinnerAdapter(
        ctx: Context
) : ArrayAdapter<String>(
        ctx,
        android.R.layout.simple_spinner_item,
        getActionTypeToLabelMap(ctx).values.toTypedArray() //get all the values, which are the labels for each of the action types
) {
    companion object {

        private var ACTION_TYPE_TO_LABEL_MAP: Map<ActionType, String>? = null

        /**
         * Determine the [ActionType] for any position in the spinner
         */
        fun getActionTypeFromPosition(ctx: Context, position: Int): ActionType {
            return getActionTypeToLabelMap(ctx).keys.toTypedArray()[position]
        }

        private fun getActionTypeToLabelMap(ctx: Context): Map<ActionType, String> {
            if (ACTION_TYPE_TO_LABEL_MAP == null) {

                ACTION_TYPE_TO_LABEL_MAP = mapOf(
                        ActionType.APP to ctx.getString(R.string.action_type_application),
                        ActionType.APP_SHORTCUT to ctx.getString(R.string.action_type_application_shortcut),
                        ActionType.KEY to ctx.getString(R.string.action_type_key),
                        ActionType.KEYCODE to ctx.getString(R.string.action_type_keycode),
                        ActionType.SYSTEM_ACTION to ctx.getString(R.string.action_type_action),
                        ActionType.TEXT_BLOCK to ctx.getString(R.string.action_type_text_block)
                )
            }

            return ACTION_TYPE_TO_LABEL_MAP!!
        }
    }

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
}