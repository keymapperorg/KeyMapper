package io.github.sds100.keymapper.util

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.DeviceInfo
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.Trigger.Companion.DOUBLE_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.LONG_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.PARALLEL
import io.github.sds100.keymapper.data.model.Trigger.Companion.SEQUENCE
import io.github.sds100.keymapper.data.model.TriggerKeyModel

/**
 * Created by sds100 on 02/03/2020.
 */

fun Trigger.buildDescription(ctx: Context, deviceInfoList: List<DeviceInfo>): String = buildString {
    val separator = when (mode) {
        PARALLEL -> ctx.str(R.string.plus)
        SEQUENCE -> ctx.str(R.string.arrow)
        else -> ctx.str(R.string.plus)
    }

    val longPress = ctx.str(R.string.clicktype_long_press)
    val doublePress = ctx.str(R.string.clicktype_double_press)

    keys.forEachIndexed { index, key ->
        if (index > 0) {
            append("  $separator ")
        }

        when (key.clickType) {
            LONG_PRESS -> append(longPress)
            DOUBLE_PRESS -> append(doublePress)
        }

        append(" ${KeycodeUtils.keycodeToString(key.keyCode)}")

        val deviceName = key.getDeviceName(ctx, deviceInfoList)
        append(" ($deviceName)")
    }
}

fun Trigger.Key.buildModel(ctx: Context, deviceInfoList: List<DeviceInfo>): TriggerKeyModel {

    return TriggerKeyModel(
        id = uniqueId,
        keyCode = keyCode,
        name = KeycodeUtils.keycodeToString(keyCode),
        clickType = clickType,
        deviceName = getDeviceName(ctx, deviceInfoList)
    )
}

fun Trigger.Key.getDeviceName(ctx: Context, deviceInfoList: List<DeviceInfo>) =
    when (deviceId) {
        Trigger.Key.DEVICE_ID_THIS_DEVICE -> ctx.str(R.string.this_device)
        Trigger.Key.DEVICE_ID_ANY_DEVICE -> ctx.str(R.string.any_device)
        else -> deviceInfoList.single { it.descriptor == deviceId }.name
    }