package io.github.sds100.keymapper.util.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.sds100.keymapper.util.color
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 21/02/2021.
 */

class ResourceProviderImpl(context: Context): ResourceProvider {
    private val ctx = context.applicationContext

    override fun getString(resId: Int, args: Array<Any>): String {
        return ctx.str(resId, formatArgArray = args)
    }

    override fun getString(resId: Int, arg: Any): String {
        return ctx.str(resId, arg)
    }

    override fun getString(resId: Int): String {
        return ctx.str(resId)
    }

    override fun getDrawable(resId: Int): Drawable {
        return ctx.drawable(resId)
    }

    override fun getColor(color: Int): Int {
        return ctx.color(color)
    }
}

interface ResourceProvider {
    fun getString(@StringRes resId: Int, args: Array<Any>): String
    fun getString(@StringRes resId: Int, arg: Any): String
    fun getString(@StringRes resId: Int): String
    fun getDrawable(@DrawableRes resId: Int): Drawable
    fun getColor(@ColorRes color: Int): Int
}