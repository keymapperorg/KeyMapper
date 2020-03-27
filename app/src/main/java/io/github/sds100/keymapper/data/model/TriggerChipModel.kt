package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 02/03/2020.
 */

data class TriggerChipModel(val triggerKeyDescriptions: List<String>, @Trigger.Mode val triggerMode: Int)