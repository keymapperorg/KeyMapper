package io.github.sds100.keymapper.system.apps

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 04/04/2021.
 */
@Serializable
@Parcelize
data class ChooseAppShortcutResult(
    val packageName: String?,
    val shortcutName: String,
    val uri: String
) : Parcelable
