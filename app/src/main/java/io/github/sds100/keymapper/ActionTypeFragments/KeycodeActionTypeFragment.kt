package io.github.sds100.keymapper.ActionTypeFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.Adapters.KeycodeAdapter
import io.github.sds100.keymapper.OnItemClickListener
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.action_type_recyclerview.*

/**
 * Created by sds100 on 29/07/2018.
 */

/**
 * A Fragment which displays all keycodes which can be used
 */
class KeycodeActionTypeFragment : ActionTypeFragment(), OnItemClickListener<Int> {

    private val mKeycodeAdapter = KeycodeAdapter(onItemClickListener = this)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_type_recyclerview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewCaption.text = getString(R.string.caption_action_type_keycode)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mKeycodeAdapter
    }

    override fun onItemClick(item: Int) {
        val action = Action(ActionType.KEYCODE, item.toString())
        chooseSelectedAction(action)
    }
}