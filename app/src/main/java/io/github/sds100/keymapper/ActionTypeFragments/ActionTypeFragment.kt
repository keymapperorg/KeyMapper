package io.github.sds100.keymapper.ActionTypeFragments

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.Action

/**
 * Created by sds100 on 29/07/2018.
 */

abstract class ActionTypeFragment : Fragment() {

    /**
     * Quit the activity and return the selected action
     */
    fun chooseSelectedAction(action: Action) {
        val intent = Intent()

        activity!!.setResult(Activity.RESULT_OK, intent)
        activity!!.finish()
    }
}