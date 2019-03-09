package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.github.sds100.keymapper.util.KeycodeUtils

/**
 * Created by sds100 on 13/07/2018.
 */

/**
 * Controls displaying a key event as a chip
 * @see Chip
 * @see ChipGroup
 */
open class KeyEventChipGroup(
        context: Context?,
        attrs: AttributeSet?) : ChipGroup(context, attrs) {

    /**
     * The chips currently being shown in the chip group
     */
    val chips = mutableListOf<KeyChip>()

    /**
     * Create a new chip view and show it in the group
     */
    fun addChip(event: KeyEvent) {
        val chip = KeyChip(context, event.keyCode)
        val text = KeycodeUtils.keyEventToString(event)

        chip.text = text

        chips.add(chip)

        addView(chip)
    }

    /**
     * @return whether the ChipGroup already contains a [keyCode]
     */
    fun containsChip(keyCode: Int): Boolean {
        return chips.any { it.keyCode == keyCode }
    }

    fun removeChip(keyCode: Int) {
        val chip = chips.find { it.keyCode == keyCode }

        removeView(chip)
        chips.remove(chip)
    }

    fun removeAllChips() {
        removeAllViews()
        chips.clear()
    }
}