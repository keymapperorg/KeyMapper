package io.github.sds100.keymapper.util

import android.content.Context
import android.view.KeyEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.Trigger.Companion.DOUBLE_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.LONG_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.PARALLEL
import io.github.sds100.keymapper.data.model.Trigger.Companion.SEQUENCE
import io.github.sds100.keymapper.data.model.TriggerChipModel
import io.github.sds100.keymapper.data.model.TriggerKeyModel
import splitties.resources.appStr

/**
 * Created by sds100 on 02/03/2020.
 */

fun Trigger.buildDescription(): String = buildString {
    val separator = when (mode) {
        PARALLEL -> appStr(R.string.plus)
        SEQUENCE -> appStr(R.string.arrow)
        else -> appStr(R.string.plus)
    }

    val longPress = appStr(R.string.clicktype_long_press)
    val doublePress = appStr(R.string.clicktype_double_press)

    keys.forEachIndexed { index, key ->
        if (index > 0) {
            append("  $separator ")
        }

        when (key.clickType) {
            LONG_PRESS -> append(longPress)
            DOUBLE_PRESS -> append(doublePress)
        }

        append(" ${KeycodeUtils.keycodeToString(key.keyCode)}")

        append(" (${key.device.name})")
    }
}

fun Trigger.Key.buildModel(): TriggerKeyModel {

    return TriggerKeyModel(
        name = KeycodeUtils.keycodeToString(keyCode),
        clickType = clickType,
        deviceName = device.name
    )
}