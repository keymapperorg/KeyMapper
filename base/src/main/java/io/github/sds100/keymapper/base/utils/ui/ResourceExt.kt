package io.github.sds100.keymapper.base.utils.ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.AttrRes
import androidx.annotation.BoolRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.annotation.StyleableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors

// Using varargs doesn't work since prints [LJava.lang.object@32f...etc
fun Context.str(@StringRes resId: Int, formatArg: Any? = null): String = getString(resId, formatArg)
fun Context.str(@StringRes resId: Int, formatArgArray: Array<Any>): String =
    getString(resId, *formatArgArray)

fun View.str(@StringRes resId: Int, formatArgs: Any? = null): String =
    context.str(resId, formatArgs)

fun Fragment.str(@StringRes resId: Int, formatArgs: Any? = null): String =
    requireContext().str(resId, formatArgs)

fun Context.strArray(@ArrayRes resId: Int): Array<String> = resources.getStringArray(resId)
fun Fragment.strArray(@ArrayRes resId: Int): Array<String> = requireContext().strArray(resId)

fun Context.bool(@BoolRes resId: Int): Boolean = resources.getBoolean(resId)

fun View.bool(
    attributeSet: AttributeSet,
    @StyleableRes styleableId: IntArray,
    @StyleableRes attrId: Int,
    defaultValue: Boolean = false,
) = context.bool(attributeSet, styleableId, attrId, defaultValue)

fun Context.resourceId(
    attributeSet: AttributeSet,
    @StyleableRes styleableId: IntArray,
    @StyleableRes attrId: Int,
): Int? {
    val typedArray = theme.obtainStyledAttributes(attributeSet, styleableId, 0, 0)
    var attrValue: Int?

    try {
        attrValue = typedArray.getResourceIdOrThrow(attrId)
    } catch (e: IllegalArgumentException) {
        // return null if it can't find it
        attrValue = null
    }

    typedArray.recycle()

    return attrValue
}

/**
 * Get a boolean from an attribute
 */
fun Context.bool(
    attributeSet: AttributeSet,
    @StyleableRes styleableId: IntArray,
    @StyleableRes attrId: Int,
    defaultValue: Boolean = false,
): Boolean {
    val typedArray = theme.obtainStyledAttributes(attributeSet, styleableId, 0, 0)

    var attrValue: Boolean

    try {
        attrValue = typedArray.getBoolean(attrId, defaultValue)
    } catch (e: Exception) {
        attrValue = defaultValue
    } finally {
        typedArray.recycle()
    }

    return attrValue
}

/**
 * Get a string from an attribute
 */
fun Context.str(
    attributeSet: AttributeSet,
    @StyleableRes styleableId: IntArray,
    @StyleableRes attrId: Int,
): String? {
    val typedArray = theme.obtainStyledAttributes(attributeSet, styleableId, 0, 0)

    val attrValue: String?

    try {
        attrValue = typedArray.getString(attrId)
    } finally {
        typedArray.recycle()
    }

    return attrValue
}

fun View.str(
    attributeSet: AttributeSet,
    @StyleableRes styleableId: IntArray,
    @StyleableRes attrId: Int,
) = context.str(attributeSet, styleableId, attrId)

/**
 * Get a resource drawable. Can be safely used to get vector drawables on pre-lollipop.
 */
fun Context.drawable(@DrawableRes resId: Int): Drawable =
    AppCompatResources.getDrawable(this, resId)!!

fun View.drawable(@DrawableRes resId: Int): Drawable = context.drawable(resId)
fun Fragment.drawable(@DrawableRes resId: Int): Drawable = requireContext().drawable(resId)

fun Context.color(@ColorRes resId: Int, harmonize: Boolean = false): Int {
    val color = ContextCompat.getColor(this, resId)

    if (harmonize) {
        return MaterialColors.harmonizeWithPrimary(this, color)
    } else {
        return color
    }
}

fun View.color(@ColorRes resId: Int, harmonize: Boolean = false): Int =
    context.color(resId, harmonize)

fun Fragment.color(@ColorRes resId: Int, harmonize: Boolean = false): Int =
    requireContext().color(resId, harmonize)

@ColorInt
fun Context.styledColor(@AttrRes attr: Int) = withStyledAttributes(attr) { getColor(it, 0) }

@ColorInt
fun Fragment.styledColor(@AttrRes attr: Int) = requireContext().styledColor(attr)

@ColorInt
fun View.styledColor(@AttrRes attr: Int) = context.styledColor(attr)

fun View.styledFloat(@AttrRes attr: Int): Float = context.styledFloat(attr)
fun Context.styledFloat(@AttrRes attr: Int): Float = withStyledAttributes(attr) { getFloat(it, 1f) }

fun Context.int(@IntegerRes resId: Int) = resources.getInteger(resId)
fun Fragment.int(@IntegerRes resId: Int) = requireContext().int(resId)

fun Context.intArray(@ArrayRes resId: Int): IntArray = resources.getIntArray(resId)
fun Fragment.intArray(@ArrayRes resId: Int): IntArray = resources.getIntArray(resId)

fun Context.styledColorSL(@AttrRes attr: Int): ColorStateList? = withStyledAttributes(attr) {
    getColorStateList(it)
}

fun Fragment.styledColorSL(@AttrRes attr: Int) = context!!.styledColorSL(attr)
fun View.styledColorSL(@AttrRes attr: Int) = context.styledColorSL(attr)

fun Context.colorSl(@ColorRes color: Int): ColorStateList? =
    ContextCompat.getColorStateList(this, color)

fun View.colorSl(@ColorRes color: Int) = context.colorSl(color)

private val uiThreadConfinedCachedAttrArray = IntArray(1)
private val cachedAttrArray = IntArray(1)

inline fun <T> Context.withStyledAttributes(
    @AttrRes attrRes: Int,
    func: TypedArray.(firstIndex: Int) -> T,
): T = obtainStyledAttr(attrRes).let { styledAttrs ->
    styledAttrs.func(styledAttrs.getIndex(0)).also { styledAttrs.recycle() }
}

fun Context.obtainStyledAttr(@AttrRes attrRes: Int): TypedArray =
    if (Looper.getMainLooper().isCurrentThread) {
        uiThreadConfinedCachedAttrArray[0] = attrRes
        obtainStyledAttributes(uiThreadConfinedCachedAttrArray)
    } else {
        synchronized(cachedAttrArray) {
            cachedAttrArray[0] = attrRes
            obtainStyledAttributes(cachedAttrArray)
        }
    }
