package io.github.sds100.keymapper.actions.sound

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 22/06/2021.
 */
@Serializable
data class ChooseSoundFileResult(val soundUid: String, val description: String)
