package io.github.sds100.keymapper.base.utils.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceProviderImpl @Inject constructor(@ApplicationContext context: Context) :
    ResourceProvider {
    private val ctx = context.applicationContext

    override fun getString(resId: Int, args: Array<Any>): String =
        ctx.str(resId, formatArgArray = args)

    override fun getText(resId: Int): CharSequence = ctx.getText(resId)

    override fun getString(resId: Int, arg: Any): String = ctx.str(resId, arg)

    override fun getString(resId: Int): String = ctx.str(resId)

    override fun getDrawable(resId: Int): Drawable = ctx.drawable(resId)

    override fun getColor(color: Int): Int = ctx.color(color)
}

interface ResourceProvider {
    fun getString(@StringRes resId: Int, args: Array<Any>): String
    fun getString(@StringRes resId: Int, arg: Any): String
    fun getString(@StringRes resId: Int): String
    fun getText(@StringRes resId: Int): CharSequence
    fun getDrawable(@DrawableRes resId: Int): Drawable
    fun getColor(@ColorRes color: Int): Int
}
