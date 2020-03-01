package io.github.sds100.keymapper.util

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.ActionModel
import splitties.resources.appStr

/**
 * Created by sds100 on 25/01/2020.
 */

@BindingAdapter("app:onLongClick")
fun setLongClickListener(view: View, onLongClickListener: View.OnLongClickListener) {
    view.setOnLongClickListener(onLongClickListener)
}

@BindingAdapter("app:actions")
fun ChipGroup.bindActions(actions: List<ActionModel>) {
    removeAllViews()

    actions.forEach {
        Chip(context).apply {
            text = it.description
            chipIcon = it.icon
            isCloseIconVisible = it.hasError
            isClickable = it.hasError

            if (it.description == null && it.hasError) {
                text = it.errorDescription
            }

            addView(this)
        }
    }
}

@BindingAdapter("app:isKeymapEnabled", "app:noActions", "app:noTriggers", requireAll = false)
fun TextView.setKeymapExtraInfo(isKeymapEnabled: Boolean = false, noActions: Boolean = false, noTriggers: Boolean = false) {
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

    if (noTriggers) {
        if (text.isNotEmpty()) {
            append(" • ")
        }
        append(appStr(R.string.no_triggers))
    }
}