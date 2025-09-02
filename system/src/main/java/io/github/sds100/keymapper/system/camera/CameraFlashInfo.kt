package io.github.sds100.keymapper.system.camera

data class CameraFlashInfo(
    val supportsVariableStrength: Boolean,
    val defaultStrength: Int,
    val maxStrength: Int,
)
