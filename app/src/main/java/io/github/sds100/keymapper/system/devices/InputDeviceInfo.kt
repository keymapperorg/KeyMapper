package io.github.sds100.keymapper.system.devices

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 07/03/2021.
 */

@Serializable
data class InputDeviceInfo(val descriptor: String, val name: String, val id: Int, val isExternal : Boolean)