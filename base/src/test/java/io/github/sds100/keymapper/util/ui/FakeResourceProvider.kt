package io.github.sds100.keymapper.base.utils.ui

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeResourceProvider : ResourceProvider {
    val stringResourceMap: MutableMap<Int, String> = mutableMapOf()
    override val onThemeChange: Flow<Unit> = MutableSharedFlow()

    override fun getString(resId: Int, args: Array<Any>): String {
        return stringResourceMap[resId] ?: ""
    }

    override fun getString(resId: Int, arg: Any): String {
        return stringResourceMap[resId] ?: ""
    }

    override fun getString(resId: Int): String {
        return stringResourceMap[resId] ?: ""
    }

    override fun getText(resId: Int): CharSequence {
        return stringResourceMap[resId] ?: ""
    }

    override fun getDrawable(resId: Int): Drawable {
        throw Exception()
    }

    override fun getColor(color: Int): Int {
        throw Exception()
    }
}
