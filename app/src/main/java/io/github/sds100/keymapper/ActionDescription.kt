package io.github.sds100.keymapper

import android.graphics.drawable.Drawable

/**
 * Created by sds100 on 07/10/2018.
 */

/**
 * Contains all necessary information to display information about an action to the user
 */
data class ActionDescription(
        val iconDrawable: Drawable? = null,
        val title: String? = null,
        val errorDescription: String? = null,
        val errorResult: ErrorResult? = null) {

    val errorCode: Int?
        get() = errorResult?.errorCode
}