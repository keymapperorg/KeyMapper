package io.github.sds100.keymapper.common.utils

import android.os.Build
import android.view.InputDevice
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object InputDeviceUtils {
    fun appendDeviceDescriptorToName(descriptor: String, name: String): String =
        "$name ${descriptor.substring(0..4)}"

    fun createInputDeviceInfo(inputDevice: InputDevice): InputDeviceInfo = InputDeviceInfo(
        inputDevice.descriptor,
        inputDevice.name,
        inputDevice.id,
        inputDevice.isExternalCompat,
        isGameController = inputDevice.controllerNumber != 0,
        sources = inputDevice.sources,
    )
}

val InputDevice.isExternalCompat: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isExternal
    } else {
        try {
            val m: Method = InputDevice::class.java.getMethod("isExternal")
            (m.invoke(this) as Boolean)
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            false
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            false
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            false
        }
    }