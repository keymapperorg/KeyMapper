package io.github.sds100.keymapper.data.model.options

import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.data.model.options.BehaviorOption.Companion.applyBehaviorOption
import io.github.sds100.keymapper.util.ActionUtils
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.bitflags.hasFlag
import java.io.Serializable

/**
 * Created by sds100 on 22/11/20.
 */
class ActionShortcutOptions(action: Action,
                            actionCount: Int) : Serializable, BaseOptions<Action> {

    companion object {
        const val ID_SHOW_VOLUME_UI = "show_volume_ui"
        const val ID_SHOW_PERFORMING_ACTION_TOAST = "show_performing_action_toast"
        const val ID_MULTIPLIER = "multiplier"
        const val ID_DELAY_BEFORE_NEXT_ACTION = "delay_before_next_action"
    }

    override val id = action.uid

    val showVolumeUi = BehaviorOption(
        id = ID_SHOW_VOLUME_UI,
        value = action.flags.hasFlag(Action.ACTION_FLAG_SHOW_VOLUME_UI),
        isAllowed = ActionUtils.isVolumeAction(action.data)
    )

    val showPerformingActionToast = BehaviorOption(
        id = ID_SHOW_PERFORMING_ACTION_TOAST,
        value = action.flags.hasFlag(Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST),
        isAllowed = true
    )

    private val delayBeforeNextAction: BehaviorOption<Int>

    private val multiplier = BehaviorOption(
        id = ID_MULTIPLIER,
        value = action.extras.getData(Action.EXTRA_MULTIPLIER).valueOrNull()?.toInt() ?: BehaviorOption.DEFAULT,
        isAllowed = true
    )

    /*
     It is very important that any new options are only allowed with a valid combination of other options. Make sure
     the "isAllowed" property considers all the other options.
     */

    init {
        val delayBeforeNextActionValue =
            action.extras.getData(Action.EXTRA_DELAY_BEFORE_NEXT_ACTION).valueOrNull()?.toInt()

        delayBeforeNextAction = BehaviorOption(
            id = ID_DELAY_BEFORE_NEXT_ACTION,
            value = delayBeforeNextActionValue ?: BehaviorOption.DEFAULT,
            isAllowed = actionCount > 0
        )
    }

    override val intOptions = listOf(
        delayBeforeNextAction,
        multiplier
    )

    override val boolOptions = listOf(
        showPerformingActionToast,
        showVolumeUi
    )

    override fun setValue(id: String, value: Int): BaseOptions<Action> {
        when (id) {
            ID_MULTIPLIER -> multiplier.value = value
            ID_DELAY_BEFORE_NEXT_ACTION -> delayBeforeNextAction.value = value
        }

        return this
    }

    override fun setValue(id: String, value: Boolean): BaseOptions<Action> {
        when (id) {

            ID_SHOW_VOLUME_UI -> showVolumeUi.value = value
            ID_SHOW_PERFORMING_ACTION_TOAST -> showPerformingActionToast.value = value
        }

        return this
    }

    override fun apply(old: Action): Action {
        val newFlags = old.flags
            .applyBehaviorOption(showVolumeUi, Action.ACTION_FLAG_SHOW_VOLUME_UI)
            .applyBehaviorOption(showPerformingActionToast, Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST)

        val newExtras = old.extras
            .applyBehaviorOption(multiplier, Action.EXTRA_MULTIPLIER)
            .applyBehaviorOption(delayBeforeNextAction, Action.EXTRA_DELAY_BEFORE_NEXT_ACTION)

        newExtras.removeAll {
            it.id in arrayOf(Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR, Action.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR)
        }

        return old.copy(flags = newFlags, extras = newExtras, uid = old.uid)
    }
}