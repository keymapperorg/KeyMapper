package io.github.sds100.keymapper.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 13/12/2018.
 */

/**
 * A simple Preference view which opens a link defined as an attribute
 */
class UrlLinkPreference(
        context: Context?,
        attrs: AttributeSet
) : Preference(context, attrs) {

    private val mUrlLink = context?.str(
            attrs,
            R.styleable.UrlLinkPreference,
            R.styleable.UrlLinkPreference_linkUrl
    )

    override fun onClick() {
        super.onClick()

        //open the link
        mUrlLink?.let { urlLink ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlLink))
            context.startActivity(intent)
        }
    }
}