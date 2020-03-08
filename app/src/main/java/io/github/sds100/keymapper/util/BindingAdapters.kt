package io.github.sds100.keymapper.util

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.ActionModel
import io.github.sds100.keymapper.data.model.FlagModel
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.TriggerModel
import io.github.sds100.keymapper.ui.callback.ActionErrorClickCallback
import splitties.resources.appStr
import splitties.resources.drawable


/**
 * Created by sds100 on 25/01/2020.
 */

@BindingAdapter("app:onLongClick")
fun setLongClickListener(view: View, onLongClickListener: View.OnLongClickListener) {
    view.setOnLongClickListener(onLongClickListener)
}

@BindingAdapter("app:actions", "app:actionErrorClickCallback", requireAll = true)
fun ChipGroup.bindActions(actions: List<ActionModel>, callback: ActionErrorClickCallback) {
    removeAllViews()

    actions.forEach {
        Chip(context).apply {
            text = it.description
            chipIcon = it.icon
            isCloseIconVisible = it.hasError

            if (it.description == null && it.hasError) {
                text = it.error?.errorMessage
            }

            if (it.hasError) {
                isClickable = true
                setOnClickListener { _ ->
                    callback.onActionErrorClick(it.error!!)
                }
            }

            addView(this)
        }
    }
}

@BindingAdapter("app:isKeymapEnabled", "app:noActions", "app:noTrigger", requireAll = false)
fun TextView.setKeymapExtraInfo(isKeymapEnabled: Boolean = false, noActions: Boolean = false, noTrigger: Boolean = false) {
    text = ""

    if (!isKeymapEnabled) {
        append(appStr(R.string.disabled))
    }

    if (noActions) {
        if (text.isNotEmpty()) {
            append(" • ")
        }

        append(appStr(R.string.no_actions))
    }

    if (noTrigger) {
        if (text.isNotEmpty()) {
            append(" • ")
        }
        append(appStr(R.string.no_trigger))
    }
}

@BindingAdapter("app:triggerModel")
fun ChipGroup.bindTriggerModel(triggerModel: TriggerModel) {
    val separatorDrawable = when (triggerModel.triggerMode) {
        Trigger.PARALLEL -> context.drawable(R.drawable.ic_baseline_add_24)
        Trigger.SEQUENCE -> context.drawable(R.drawable.ic_baseline_arrow_forward_24)
        else -> context.drawable(R.drawable.ic_baseline_add_24)
    }

    removeAllViews()

    triggerModel.triggerKeyDescriptions.forEachIndexed { index, description ->

        //add a chip which is either a + or -> depending on the trigger mode
        if (index != 0) {
            Chip(context).apply {

                chipIcon = separatorDrawable

                chipStrokeWidth = 0f
                textStartPadding = 0f
                textEndPadding = 0f
                setChipIconTintResource(R.color.iconTintTriggerKeySeparator)

                addView(this)
            }
        }

        Chip(context).apply {
            text = description

            addView(this)
        }
    }
}

@BindingAdapter("app:flagModels")
fun ChipGroup.bindFlagModels(flagModels: List<FlagModel>) {
    removeAllViews()

    flagModels.forEach {
        Chip(context).apply {
            text = it.text
            chipIcon = it.icon

            addView(this)
        }
    }
}