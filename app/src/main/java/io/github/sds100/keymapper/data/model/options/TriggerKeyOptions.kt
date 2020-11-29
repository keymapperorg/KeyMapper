package io.github.sds100.keymapper.data.model.options

import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.options.BehaviorOption.Companion.applyBehaviorOption
import splitties.bitflags.hasFlag
import java.io.Serializable

class TriggerKeyOptions(
    key: Trigger.Key,
    @Trigger.Mode mode: Int
) : BaseOptions<Trigger.Key>, Serializable {

    companion object {
        const val ID_DO_NOT_CONSUME_KEY_EVENT = "do_not_consume_key_event"
        const val ID_CLICK_TYPE = "click_type"
    }

    override val id: String = key.uniqueId

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

    override fun setValue(id: String, value: Boolean): TriggerKeyOptions {
        when (id) {
            ID_DO_NOT_CONSUME_KEY_EVENT -> doNotConsumeKeyEvents.value = value
        }

        return this
    }

    override fun setValue(id: String, value: Int): TriggerKeyOptions {
        when (id) {
            ID_CLICK_TYPE -> clickType.value = value
        }

        return this
    }

    override val intOptions: List<BehaviorOption<Int>>
        get() = listOf()

    override val boolOptions: List<BehaviorOption<Boolean>>
        get() = listOf()

    override fun apply(old: Trigger.Key): Trigger.Key {
        val newFlags = old.flags.applyBehaviorOption(doNotConsumeKeyEvents, Trigger.Key.FLAG_DO_NOT_CONSUME_KEY_EVENT)
        val newClickType = clickType.value

        return old.copy(flags = newFlags, clickType = newClickType)
    }
}