package io.github.sds100.keymapper.data.model.behavior

import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.behavior.BehaviorOption.Companion.applyBehaviorOption
import splitties.bitflags.hasFlag
import java.io.Serializable

class TriggerKeyBehavior(
    key: Trigger.Key,
    @Trigger.Mode mode: Int
) : Serializable {

    companion object {
        const val ID_DO_NOT_CONSUME_KEY_EVENT = "do_not_consume_key_event"
        const val ID_CLICK_TYPE = "click_type"
    }

    val uniqueId = key.uid

    val clickType = BehaviorOption(
        id = ID_CLICK_TYPE,
        value = key.clickType,
        isAllowed = mode == Trigger.SEQUENCE || mode == Trigger.UNDEFINED
    )

    val doNotConsumeKeyEvents = BehaviorOption(
        id = ID_DO_NOT_CONSUME_KEY_EVENT,
        value = key.flags.hasFlag(Trigger.Key.FLAG_DO_NOT_CONSUME_KEY_EVENT),
        isAllowed = true
    )

    /*
     It is very important that any new options are only allowed with a valid combination of other options. Make sure
     the "isAllowed" property considers all the other options.
     */

    fun setValue(id: String, value: Boolean): TriggerKeyBehavior {
        when (id) {
            ID_DO_NOT_CONSUME_KEY_EVENT -> doNotConsumeKeyEvents.value = value
        }

        return this
    }

    fun setValue(id: String, value: Int): TriggerKeyBehavior {
        when (id) {
            ID_CLICK_TYPE -> clickType.value = value
        }

        return this
    }

    fun applyToTriggerKey(triggerKeys: List<Trigger.Key>, triggerMode: Int): List<Trigger.Key> {
        var keyToApplyOptions: Trigger.Key? = null

        return triggerKeys
            .toMutableList()
            .map {
                if (it.uid == uniqueId) {
                    it.clickType = clickType.value

                    it.flags = it.flags.applyBehaviorOption(doNotConsumeKeyEvents, Trigger.Key.FLAG_DO_NOT_CONSUME_KEY_EVENT)

                    keyToApplyOptions = it
                }

                it
            }.map {
                //set the click type of all duplicate keys to the same click type
                if (triggerMode == Trigger.SEQUENCE
                    && it.keyCode == keyToApplyOptions?.keyCode && it.deviceId == keyToApplyOptions?.deviceId) {

                    it.clickType = clickType.value
                }

                it
            }
    }
}