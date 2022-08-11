package io.github.sds100.keymapper.actions.sound

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 22/06/2021.
 */
@Serializable
@Parcelize
data class ChooseSoundResult(val soundUid: String, val name: String) : Parcelable 
