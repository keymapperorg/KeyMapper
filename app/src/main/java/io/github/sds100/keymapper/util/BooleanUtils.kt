package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 22/05/2019.
 */

fun Boolean?.isNotNullAndTrue(): Boolean {
    return this != null && this == true
}