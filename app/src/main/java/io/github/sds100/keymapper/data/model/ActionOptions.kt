package io.github.sds100.keymapper.data.model

import java.io.Serializable

/**
 * Created by sds100 on 27/06/20.
 */
class ActionOptions(val actionId: String, val flags: Int, val extras: List<Extra>) : Serializable