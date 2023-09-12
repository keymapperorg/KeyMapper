package io.github.sds100.keymapper.util.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.sds100.keymapper.util.color
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 21/02/2021.
 */

class ResourceProviderImpl(
    context: Context,
    private val coroutineScope: CoroutineScope
) : ResourceProvider {
    private val ctx = context.applicationContext

    override val onThemeChange = MutableSharedFlow<Unit>()

    override fun getString(resId: Int, args: Array<Any>): String {
        return ctx.str(resId, formatArgArray = args)
    }

    override fun getText(resId: Int): CharSequence {
        return ctx.getText(resId)
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

    override fun getContext(): Context {
        return ctx;
    }

    fun onThemeChange() {
        coroutineScope.launch {
            onThemeChange.emit(Unit)
        }
    }
}

interface ResourceProvider {
    val onThemeChange: Flow<Unit>

    fun getString(@StringRes resId: Int, args: Array<Any>): String
    fun getString(@StringRes resId: Int, arg: Any): String
    fun getString(@StringRes resId: Int): String
    fun getText(@StringRes resId: Int): CharSequence
    fun getDrawable(@DrawableRes resId: Int): Drawable
    fun getColor(@ColorRes color: Int): Int
    fun getContext(): Context;
}