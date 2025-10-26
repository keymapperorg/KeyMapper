package io.github.sds100.keymapper.base.utils.ui

import android.graphics.drawable.Drawable

class FakeResourceProvider : ResourceProvider {
    val stringResourceMap: MutableMap<Int, String> = mutableMapOf()

    override fun getString(
        resId: Int,
        args: Array<Any>,
    ): String = stringResourceMap[resId] ?: ""

    override fun getString(
        resId: Int,
        arg: Any,
    ): String = stringResourceMap[resId] ?: ""

    override fun getString(resId: Int): String = stringResourceMap[resId] ?: ""

    override fun getText(resId: Int): CharSequence = stringResourceMap[resId] ?: ""

    override fun getDrawable(resId: Int): Drawable = throw Exception()

    override fun getColor(color: Int): Int = throw Exception()
}
