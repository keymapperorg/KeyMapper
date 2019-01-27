package io.github.sds100.keymapper.Utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.Interfaces.IContext

/**
 * Created by sds100 on 31/12/2018.
 */

/**
 * Get a resource string
 */
// Using varargs doesn't work since prints [LJava.lang.object@32f...etc
fun Context.str(@StringRes resId: Int, formatArgs: Any? = null): String = getString(resId, formatArgs)

fun IContext.str(@StringRes resId: Int, formatArgs: Any? = null): String = ctx.getString(resId, formatArgs)
fun View.str(@StringRes resId: Int, formatArgs: Any? = null): String = context.str(resId, formatArgs)

fun Context.bool(@BoolRes resId: Int): Boolean = resources.getBoolean(resId)

/**
 * Get a string from an attribute
 */
fun Context.str(attributeSet: AttributeSet, @StyleableRes styleableId: IntArray, @StyleableRes attrId: Int): String {
    val typedArray = theme.obtainStyledAttributes(attributeSet, styleableId, 0, 0)

    val attrValue: String?

    try {
        attrValue = typedArray.getString(attrId)
    } finally {
        typedArray.recycle()
    }

    return attrValue!!
}

fun View.str(attributeSet: AttributeSet, @StyleableRes styleableId: IntArray, @StyleableRes attrId: Int) =
        context.str(attributeSet, styleableId, attrId)

/**
 * Get a resource drawable. Can be safely used to get vector drawables on pre-lollipop.
 */
fun Context.drawable(@DrawableRes resId: Int): Drawable {
    return AppCompatResources.getDrawable(this, resId)!!
}

fun View.drawable(@DrawableRes resId: Int): Drawable = context.drawable(resId)
fun IContext.drawable(@DrawableRes resId: Int): Drawable = ctx.drawable(resId)

fun Context.color(@ColorRes resId: Int): Int = ContextCompat.getColor(this, resId)
fun View.color(@ColorRes resId: Int): Int = context.color(resId)