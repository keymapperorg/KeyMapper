package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference

/**
 * Created by sds100 on 03/10/2018.
 */

/**
 * To cancel the multi-select list dialog, return false inside the [OnPreferenceClickListener].
 */
class CancellableMultiSelectListPreference(
        context: Context?,
        attrs: AttributeSet?
) : MultiSelectListPreference(context, attrs) {

    override fun onClick() {
        if (onPreferenceClickListener.onPreferenceClick(this)) {
            super.onClick()
        }
    }
}