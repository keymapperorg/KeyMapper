package io.github.sds100.keymapper.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.action_type_edit_text.*

/**
 * Created by sds100 on 29/07/2018.
 */

/**
 * A Fragment that allows a user to type any length of text
 */
class TextActionTypeFragment : ActionTypeFragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_type_edit_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonDone.setOnClickListener {
            val action = Action(ActionType.TEXT_BLOCK, editText.text.toString())
            chooseSelectedAction(action)
        }
    }
}