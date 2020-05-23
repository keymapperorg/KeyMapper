package io.github.sds100.keymapper.data.model

import androidx.annotation.StringDef
import java.io.Serializable

/**
 * Created by sds100 on 26/01/2019.
 */

@StringDef(value = [
    Extra.EXTRA_PACKAGE_NAME,
    Extra.EXTRA_SHORTCUT_TITLE,
    Extra.EXTRA_STREAM_TYPE,
    Extra.EXTRA_LENS,
    Extra.EXTRA_RINGER_MODE,
    Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT
])
annotation class ExtraId

data class Extra(@ExtraId val id: String, val data: String) : Serializable {
    companion object {
        //DON'T CHANGE THESE IDs!!!!
        const val EXTRA_SHORTCUT_TITLE = "extra_title"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_STREAM_TYPE = "extra_stream_type"
        const val EXTRA_LENS = "extra_flash"
        const val EXTRA_RINGER_MODE = "extra_ringer_mode"

        const val EXTRA_SEQUENCE_TRIGGER_TIMEOUT = "extra_sequence_trigger_timeout"
    }
}