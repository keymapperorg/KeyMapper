package io.github.sds100.keymapper.view

import android.app.Dialog
import android.content.Context
import android.view.Window
import androidx.annotation.StringRes
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.dialog_progress.*

/**
 * Created by sds100 on 12/12/2018.
 */

//inherit from Dialog and not AppCompatDialog so the title can be hidden easily.
class ProgressDialog(context: Context?, @StringRes messageRes: Int) : Dialog(context!!) {

    init {
        //hide the title bar at the top of the dialog
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.dialog_progress)
        textView.setText(messageRes)
    }
}