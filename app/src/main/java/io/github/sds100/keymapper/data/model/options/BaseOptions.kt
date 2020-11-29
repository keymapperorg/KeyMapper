package io.github.sds100.keymapper.data.model.options

import java.io.Serializable

/**
 * Created by sds100 on 21/11/20.
 */
interface BaseOptions<T> : Serializable {
    val intOptions: List<BehaviorOption<Int>>
    val boolOptions: List<BehaviorOption<Boolean>>

    fun setValue(id: String, value: Int): BaseOptions<T>
    fun setValue(id: String, value: Boolean): BaseOptions<T>
    fun apply(old: T): T
}