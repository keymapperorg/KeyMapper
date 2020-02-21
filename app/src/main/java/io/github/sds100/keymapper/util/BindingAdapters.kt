package io.github.sds100.keymapper.util

import android.view.View
import androidx.databinding.BindingAdapter

/**
 * Created by sds100 on 25/01/2020.
 */

@BindingAdapter("app:onLongClick")
fun setLongClickListener(view: View, onLongClickListener: View.OnLongClickListener) {
    view.setOnLongClickListener(onLongClickListener)
}