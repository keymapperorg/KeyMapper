package io.github.sds100.keymapper.Utils

import android.content.Context
import android.util.AttributeSet
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 13/12/2018.
 */

object AttrUtils {
    fun getCustomStringAttrValue(ctx: Context, attrs: AttributeSet, attrId: Int): String? {
        val typedArray = ctx.theme.obtainStyledAttributes(attrs, R.styleable.ServiceStatusLayout, 0, 0)

        val attrValue: String?

        try {
            attrValue = typedArray.getString(attrId)
        } finally {
            typedArray.recycle()
        }

        return attrValue
    }
}