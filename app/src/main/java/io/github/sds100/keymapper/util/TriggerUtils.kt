package io.github.sds100.keymapper.util

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

        append(" ${KeycodeUtils.keycodeToString(key.keycode)}")

        if (key.deviceId != null) {
            //TODO get device name
            val deviceName = ""

            append(" ($deviceName)")
        }
    }
}

fun Trigger.Key.buildModel(): TriggerKeyModel {
    //TODO get device name
    return TriggerKeyModel(
        name = KeycodeUtils.keycodeToString(keycode),
        clickType = clickType,
        deviceName = null
    )
}

fun Trigger.buildTriggerChipModel(): TriggerChipModel {
    val keyDescriptions = sequence {
        keys.forEach { key ->
            val description = buildString {
                //only let the user know when it isn't a short press

                val interpunct = appStr(R.string.interpunct)

                when (key.clickType) {
                    LONG_PRESS -> append("${appStr(R.string.clicktype_long_press)} $interpunct ")
                    DOUBLE_PRESS -> append("${appStr(R.string.clicktype_double_press)} $interpunct ")
                }

                val keycodeString = KeycodeUtils.keycodeToString(key.keycode)
                append(keycodeString)

                //TODO need to get device name from its ID
                val deviceName: String? = null

                if (deviceName != null) {
                    append(" $interpunct $deviceName")
                }

            }

            yield(description)
        }
    }.toList()

    return TriggerChipModel(keyDescriptions, mode)
}