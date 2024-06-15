package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 28/02/2021.
 */

/*
Can't serialize with kotlinx.serialization because a custom serializer breaks a build
 if kapt is enabled. A custom serializer is required because it has a generic type.
See https://youtrack.jetbrains.com/issue/KT-42652 and https://youtrack.jetbrains.com/issue/KT-30346.
Hopefully this will be fixed in Kotlin 1.5.

I will create new Defaultable classes for each type for now.
 */
sealed class Defaultable<out T> {

    companion object {
        fun <T> create(value: T?): Defaultable<T> = if (value == null) {
            Default
        } else {
            Custom(value)
        }
    }

    fun nullIfDefault(): T? = if (this is Custom) {
        this.data
    } else {
        null
    }

    data class Custom<out T>(val data: T) : Defaultable<T>()

    object Default : Defaultable<Nothing>()
}

// The custom serializer to serialize Defaultable.Custom<T>)
// class CustomSerializer<T>(private val dataSerializer: KSerializer<T>) :
//    KSerializer<Defaultable.Custom<T>> {
//    override val descriptor: SerialDescriptor = dataSerializer.descriptor
//    override fun serialize(encoder: Encoder, value: Defaultable.Custom<T>) =
//        dataSerializer.serialize(encoder, value.data)
//
//    override fun deserialize(decoder: Decoder) =
//        Defaultable.Custom(dataSerializer.deserialize(decoder))
// }
