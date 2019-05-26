package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.delegate.ShowTextFromUrlDelegate
import io.github.sds100.keymapper.util.MarkdownUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 15/12/2018.
 */

class MarkdownDialogPreference(context: Context?, attrs: AttributeSet?) : Preference(context, attrs),
        ShowTextFromUrlDelegate {

    private val mFileUrl = context?.str(
            attrs!!,
            R.styleable.MarkdownDialogPreference,
            R.styleable.MarkdownDialogPreference_fileUrl
    )

    override fun onClick() {
        super.onClick()

        mFileUrl?.let { fileUrl ->
            showTextFromUrl(context!!, fileUrl) { text ->
                MarkdownUtils.showDialog(context!!, text)
            }
        }
    }
}