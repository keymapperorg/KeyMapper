package io.github.sds100.keymapper

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Created by sds100 on 13/07/2018.
 */

/**
 * Controls displaying a trigger as chips
 * @see Chip
 * @see ChipGroup
 */
class TriggerChipGroup(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
) : ChipGroup(context, attrs, defStyleAttr) {

    companion object {

        /**
         * Maps keys which aren't single characters like the Control keys to a string representation
         */
        private val NON_CHARACTER_KEY_MAP = mapOf(
                Pair(KeyEvent.KEYCODE_VOLUME_DOWN, "Vol Down"),
                Pair(KeyEvent.KEYCODE_VOLUME_UP, "Vol Up"),

                Pair(KeyEvent.KEYCODE_CTRL_LEFT, "Ctrl"),
                Pair(KeyEvent.KEYCODE_CTRL_RIGHT, "Ctrl"),

                Pair(KeyEvent.KEYCODE_SHIFT_LEFT, "Shift"),
                Pair(KeyEvent.KEYCODE_SHIFT_RIGHT, "Shift"),

                Pair(KeyEvent.KEYCODE_DPAD_LEFT, "Left")
        )
    }

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     * The chips currently being shown in the chip group
     */
    private val mChips = mutableListOf<KeyChip>()

    /**
     * Create a new chip view and show it in the group
     */
    fun addChip(event: KeyEvent) {
        val chip = KeyChip(context, event.keyCode)
        val text = createTextFromKeyEvent(event)

        chip.text = text

        mChips.add(chip)

        addView(chip)
    }

    fun removeChip(keyCode: Int) {
        val chip = mChips.find { it.keyCode == keyCode }

        removeView(chip)
        mChips.remove(chip)
    }

    fun removeAllChips() {
        removeAllViews()
        mChips.clear()
    }

    /**
     * Show chips for each item in a trigger
     * @see [Trigger]
     */
    fun addChipsFromTrigger(trigger: Trigger) {
        removeAllChips()
        trigger.keys.forEach { addChip(KeyEvent(KeyEvent.ACTION_UP, it)) }
    }

    /**
     * @return a [Trigger] whose keys will be the chips in this chip group
     */
    fun createTriggerFromChips(): Trigger {
        val keys = mChips.map { it.keyCode }

        return Trigger(keys)
    }

    /**
     * Create a text representation of a key event. E.g if the control key was pressed,
     * "Ctrl" will be returned
     */
    private fun createTextFromKeyEvent(event: KeyEvent): String {

        if (NON_CHARACTER_KEY_MAP.containsKey(event.keyCode)) {
            return NON_CHARACTER_KEY_MAP.getValue(event.keyCode)
        }

        //for all other keys which input characters
        return event.displayLabel.toString()
    }
}