package io.github.sds100.keymapper.base.sorting.comparators

import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.DisplayActionUseCase
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.base.keymaps.KeyMap

class KeyMapActionsComparator(
    private val displayActions: DisplayActionUseCase,
    /**
     * Each comparator is reversed separately instead of the entire key map list
     * and Comparator.reversed() requires API level 24 so use a custom reverse field.
     */
    private val reverse: Boolean = false,
) : Comparator<KeyMap> {
    override fun compare(
        keyMap: KeyMap?,
        otherKeyMap: KeyMap?,
    ): Int {
        if (keyMap == null || otherKeyMap == null) {
            return 0
        }

        val keyMapActionsLength = keyMap.actionList.size
        val otherKeyMapActionsLength = otherKeyMap.actionList.size
        val maxLength = keyMapActionsLength.coerceAtMost(otherKeyMapActionsLength)

        // compare actions one by one
        for (i in 0 until maxLength) {
            val action1 = keyMap.actionList[i]
            val action2 = otherKeyMap.actionList[i]

            val result = compareValuesBy(
                action1,
                action2,
                { it.data.id },
                { getSecondarySortField(it.data).valueOrNull() ?: it.data.id },
                { it.repeat },
                { it.multiplier },
                { it.repeatLimit },
                { it.repeatRate },
                { it.repeatDelay },
                { it.repeatMode },
                { it.delayBeforeNextAction },
            )

            if (result != 0) {
                return invertIfReverse(result)
            }
        }

        // if actions are equal compare length
        val comparison = keyMap.actionList.size.compareTo(otherKeyMap.actionList.size)

        return invertIfReverse(comparison)
    }

    private fun invertIfReverse(result: Int) = if (reverse) {
        result * -1
    } else {
        result
    }

    private fun getSecondarySortField(action: ActionData): KMResult<String> {
        return when (action) {
            is ActionData.App -> displayActions.getAppName(action.packageName)
            is ActionData.AppShortcut -> Success(action.shortcutTitle)
            is ActionData.InputKeyEvent -> Success(action.keyCode.toString())
            is ActionData.Sound.SoundFile -> Success(action.soundDescription)
            is ActionData.Sound.Ringtone -> Success(action.uri)
            is ActionData.Volume.Stream -> Success(action.volumeStream.toString())
            is ActionData.Volume.SetRingerMode -> Success(action.ringerMode.toString())
            is ActionData.Flashlight -> Success(action.lens.toString())
            is ActionData.SwitchKeyboard -> Success(action.savedImeName)
            is ActionData.DoNotDisturb.Toggle -> Success(action.dndMode.toString())
            is ActionData.DoNotDisturb.Enable -> Success(action.dndMode.toString())
            is ActionData.ControlMediaForApp -> Success(action.packageName)
            is ActionData.Intent -> Success(action.description)
            is ActionData.TapScreen -> Success(action.description ?: "")
            is ActionData.SwipeScreen -> Success(action.description ?: "")
            is ActionData.PinchScreen -> Success(action.description ?: "")
            is ActionData.PhoneCall -> Success(action.number)
            is ActionData.Url -> Success(action.url)
            is ActionData.Text -> Success(action.text)
            is ActionData.Rotation.CycleRotations -> Success(action.orientations.joinToString())
            else -> Success("")
        }
    }
}
