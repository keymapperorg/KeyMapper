package io.github.sds100.keymapper.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import splitties.init.appCtx

/**
 * Created by sds100 on 08/03/2020.
 */

/**
 * Get a resource drawable. Can be safely used to get vector drawables on pre-lollipop.
 */
fun Context.safeVectorDrawable(@DrawableRes resId: Int?): Drawable? {
    resId ?: return null
    return AppCompatResources.getDrawable(this, resId)
}