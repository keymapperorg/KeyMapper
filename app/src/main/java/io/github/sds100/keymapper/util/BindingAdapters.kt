package io.github.sds100.keymapper.util

import android.view.View
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.github.sds100.keymapper.data.model.ActionModel

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
            text = it.title
            chipIcon = it.icon
            isCloseIconVisible = it.hasError
            isClickable = it.hasError

            if (it.title == null && it.hasError) {
                text = it.errorDescription
            }

            addView(this)
        }
    }
}