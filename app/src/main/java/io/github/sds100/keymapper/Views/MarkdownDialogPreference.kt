package io.github.sds100.keymapper.Views

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import io.github.sds100.keymapper.Delegates.ShowTextFromUrlDelegate
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Utils.AttrUtils
import io.github.sds100.keymapper.Utils.MarkdownUtils

/**
 * Created by sds100 on 15/12/2018.
 */

class MarkdownDialogPreference(context: Context?, attrs: AttributeSet?) : Preference(context, attrs),
        ShowTextFromUrlDelegate {

    private val mFileUrl = AttrUtils.getCustomStringAttrValue(
            context!!,
            attrs!!,
            R.styleable.MarkdownDialogPreference,
            R.styleable.MarkdownDialogPreference_fileUrl
    )!!

    override fun onClick() {
        super.onClick()

        showTextFromUrl(context!!, mFileUrl) { text ->
            MarkdownUtils.showDialog(context!!, text)
        }
    }
}