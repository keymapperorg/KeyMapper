package io.github.sds100.keymapper.view

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.annotation.AttrRes
import com.google.android.material.button.MaterialButton
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 23/03/2019.
 */

class BottomSheetMenuItem(
        context: Context?,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int
) : MaterialButton(context, attrs, defStyleAttr) {

    var intent: Intent? = null

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, R.attr.bottomSheetMenuItemStyle)

    init {
        val array = context?.obtainStyledAttributes(attrs,
                R.styleable.BottomSheetMenuItem,
                defStyleAttr,
                R.style.BottomSheetMenuItem)

        if (array != null) {
            array.getString(R.styleable.BottomSheetMenuItem_activityToOpen)?.let { activityToOpenAttr ->

                intent = Intent().apply {
                    //must be in format "io.github.sds100.keymapper/io.github.sds100.keymapper.activity.SettingsActivity"
                    setClassName(context, activityToOpenAttr)
                }

                array.recycle()
            }
        }

        setOnClickListener {
            if (intent != null) {
                context!!.startActivity(intent)
            }
        }
    }
}