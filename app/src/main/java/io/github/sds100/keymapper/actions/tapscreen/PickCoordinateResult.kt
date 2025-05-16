package io.github.sds100.keymapper.actions.tapscreen

import kotlinx.serialization.Serializable


@Serializable
data class PickCoordinateResult(val x: Int, val y: Int, val description: String)
