package io.github.sds100.keymapper.Views

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Utils.AttrUtils

/**
 * Created by sds100 on 13/12/2018.
 */

/**
 * A simple Preference view which opens a link defined as an attribute
 */
class UrlLinkPreference(
        context: Context?,
        attrs: AttributeSet,
        defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private val mUrlLink: String = AttrUtils.getCustomStringAttrValue(context!!, attrs, R.attr.linkUrl)!!

    override fun onClick() {
        super.onClick()

        //open the link
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mUrlLink))
        context.startActivity(intent)
    }
}