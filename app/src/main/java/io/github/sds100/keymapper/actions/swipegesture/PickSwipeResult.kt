package io.github.sds100.keymapper.actions.swipegesture

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 25/03/2021.
 */
@Serializable

data class PickSwipeResult(val path: SerializablePath, val description: String)