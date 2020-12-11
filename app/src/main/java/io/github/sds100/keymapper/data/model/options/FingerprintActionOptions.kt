package io.github.sds100.keymapper.data.model.options

import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.options.BoolOption.Companion.saveBoolOption
import io.github.sds100.keymapper.data.model.options.IntOption.Companion.saveIntOption
import io.github.sds100.keymapper.util.*
import kotlinx.android.parcel.Parcelize

/**
 * Created by sds100 on 22/11/20.
 */

@Parcelize
class FingerprintActionOptions(
    override val id: String,
    private val showVolumeUi: BoolOption,
    private val showPerformingActionToast: BoolOption,
    private val delayBeforeNextAction: IntOption,
    private val multiplier: IntOption

) : BaseOptions<Action> {

    companion object {
        const val ID_SHOW_VOLUME_UI = "show_volume_ui"
        const val ID_SHOW_PERFORMING_ACTION_TOAST = "show_performing_action_toast"
        const val ID_MULTIPLIER = "multiplier"
        const val ID_DELAY_BEFORE_NEXT_ACTION = "delay_before_next_action"
    }

    constructor(action: Action,
                actionCount: Int) : this(
        id = action.uid,

        showVolumeUi = BoolOption(
            id = ID_SHOW_VOLUME_UI,
            value = action.showVolumeUi,
            isAllowed = ActionUtils.isVolumeAction(action.data)
        ),

        showPerformingActionToast = BoolOption(
            id = ID_SHOW_PERFORMING_ACTION_TOAST,
            value = action.showPerformingActionToast,
            isAllowed = true
        ),

        multiplier = IntOption(
            id = ID_MULTIPLIER,
            value = action.multiplier ?: IntOption.DEFAULT,
            isAllowed = true
        ),

        delayBeforeNextAction = IntOption(
            id = ID_DELAY_BEFORE_NEXT_ACTION,
            value = action.delayBeforeNextAction ?: IntOption.DEFAULT,
            isAllowed = actionCount > 0
        )
    )

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
            .saveBoolOption(showVolumeUi, Action.ACTION_FLAG_SHOW_VOLUME_UI)
            .saveBoolOption(showPerformingActionToast,
                Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST)

        val newExtras = old.extras
            .saveIntOption(multiplier, Action.EXTRA_MULTIPLIER)
            .saveIntOption(delayBeforeNextAction, Action.EXTRA_DELAY_BEFORE_NEXT_ACTION)

        newExtras.removeAll {
            it.id in arrayOf(Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR,
                Action.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR)
        }

        return old.copy(flags = newFlags, extras = newExtras, uid = old.uid)
    }
}