package io.github.sds100.keymapper.Utils

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import io.github.sds100.keymapper.R

object AttrUtils {
    fun getSelectableItemBackground(ctx: Context): Drawable {
        val typedArray = ctx.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
        val drawable = typedArray.getDrawable(0)

        typedArray.recycle()

        return drawable
    }

    fun getAttrData(ctx: Context, attrId: Int): Int {
        val typedValue = TypedValue()
        (ctx as Activity).theme.resolveAttribute(attrId, typedValue, true)

        return typedValue.data
    }

    fun getAttr(ctx: Context, attrId: Int): TypedValue {
        val typedValue = TypedValue()
        (ctx as Activity).theme.resolveAttribute(attrId, typedValue, true)
        return typedValue
    }
}