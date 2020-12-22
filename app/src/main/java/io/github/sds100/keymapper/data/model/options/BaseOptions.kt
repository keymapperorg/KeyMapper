package io.github.sds100.keymapper.data.model.options

import android.os.Parcelable

/**
 * Created by sds100 on 21/11/20.
 */

/**
 * Make sure to add Parcelize annotation
 */
interface BaseOptions<T> : Parcelable {
    val id: String

    val intOptions: List<IntOption>
    val boolOptions: List<BoolOption>

    fun setValue(id: String, value: Int): BaseOptions<T>
    fun setValue(id: String, value: Boolean): BaseOptions<T>
    fun apply(old: T): T
}