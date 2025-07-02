/*
 * Copyright 2019-2021 Louis Cognault Ayeva Derman. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NOTHING_TO_INLINE")

package io.github.sds100.keymapper.common.utils

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

inline fun Long.hasFlag(flag: Long): Boolean = flag and this == flag
inline fun Long.withFlag(flag: Long): Long = this or flag
inline fun Long.minusFlag(flag: Long): Long = this and flag.inv()

inline fun Int.hasFlag(flag: Int): Boolean = flag and this == flag
inline fun Int.withFlag(flag: Int): Int = this or flag
inline fun Int.minusFlag(flag: Int): Int = this and flag.inv()

inline fun Short.hasFlag(flag: Short): Boolean = flag and this == flag
inline fun Short.withFlag(flag: Short): Short = this or flag
inline fun Short.minusFlag(flag: Short): Short = this and flag.inv()

inline fun Byte.hasFlag(flag: Byte): Boolean = flag and this == flag
inline fun Byte.withFlag(flag: Byte): Byte = this or flag
inline fun Byte.minusFlag(flag: Byte): Byte = this and flag.inv()

inline fun ULong.hasFlag(flag: ULong): Boolean = flag and this == flag
inline fun ULong.withFlag(flag: ULong): ULong = this or flag
inline fun ULong.minusFlag(flag: ULong): ULong = this and flag.inv()

inline fun UInt.hasFlag(flag: UInt): Boolean = flag and this == flag
inline fun UInt.withFlag(flag: UInt): UInt = this or flag
inline fun UInt.minusFlag(flag: UInt): UInt = this and flag.inv()

inline fun UShort.hasFlag(flag: UShort): Boolean = flag and this == flag
inline fun UShort.withFlag(flag: UShort): UShort = this or flag
inline fun UShort.minusFlag(flag: UShort): UShort = this and flag.inv()

inline fun UByte.hasFlag(flag: UByte): Boolean = flag and this == flag
inline fun UByte.withFlag(flag: UByte): UByte = this or flag
inline fun UByte.minusFlag(flag: UByte): UByte = this and flag.inv()
