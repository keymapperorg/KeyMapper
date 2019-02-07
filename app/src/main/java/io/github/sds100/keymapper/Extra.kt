package io.github.sds100.keymapper

import java.io.Serializable

/**
 * Created by sds100 on 26/01/2019.
 */

data class Extra(@ExtraId val id: String, val data: String) : Serializable