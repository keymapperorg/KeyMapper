package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import io.github.sds100.keymapper.Trigger

/**
 * Created by sds100 on 31/07/2018.
 */

class TriggerChipGroup(
        context: Context?,
        attrs: AttributeSet?
) : KeyEventChipGroup(context, attrs) {

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
        val keys = chips.map { it.keyCode }

        return Trigger(keys)
    }
}