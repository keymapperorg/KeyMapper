package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.chip.ChipDrawable
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 15/07/2018.
 */

class KeyChip(context: Context?, attrs: AttributeSet?, val keyCode: Int
) : AppCompatTextView(context, attrs) {

    constructor(context: Context?, keyCode: Int) : this(context, null, keyCode)

    private val mChipDrawable = ChipDrawable.createFromAttributes(
            context,
            null,
            0,
            R.style.Widget_MaterialComponents_Chip_Action
    )

    /* Uses a ChipDrawable as the background because it only has to look like a Chip and not behave
    * like one. Also if it inherits Chip, then it shows a ripple animation when clicked, which is
    * not what should happen. */
    init {
        background = mChipDrawable
    }

    var text: String = ""
        set(value) {
            mChipDrawable.setText(value)
        }
}
