package io.github.sds100.keymapper.fragment

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.action_type_edit_text.*
import kotlinx.android.synthetic.main.action_type_edit_text.view.*

/**
 * Created by sds100 on 29/07/2018.
 */

/**
 * A Fragment which displays all keycodes which can be used
 */
class UrlActionTypeFragment : ActionTypeFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_type_edit_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editText.inputType = InputType.TYPE_TEXT_VARIATION_URI
        textViewCaption.setText(R.string.caption_action_type_url)

        buttonDone.setOnClickListener {
            val action = Action(ActionType.URL, view.editText.text.toString())
            chooseSelectedAction(action)
        }
    }
}