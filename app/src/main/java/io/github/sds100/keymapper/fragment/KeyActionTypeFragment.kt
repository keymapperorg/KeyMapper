package io.github.sds100.keymapper.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.activity.ConfigKeymapActivity
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.action_type_key.*

/**
 * Created by sds100 on 29/07/2018.
 */

/**
 * A Fragment which listens for a key typed by the user then displays it as a [Chip]
 */
class KeyActionTypeFragment : ActionTypeFragment() {

    companion object {
        const val ACTION_ON_KEY_EVENT = "action_on_key_event"
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ON_KEY_EVENT) {
                if (isVisible) {
                    val keyEvent = intent.getParcelableExtra<KeyEvent>(ConfigKeymapActivity.EXTRA_KEY_EVENT)
                    showKeyEventChip(keyEvent)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_ON_KEY_EVENT)
        activity!!.registerReceiver(mBroadcastReceiver, intentFilter)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_type_key, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonDone.setOnClickListener {
            //check if
            if (keyEventChipGroup.chips.isEmpty()) {
                Toast.makeText(context!!, R.string.error_must_type_a_key, LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val action = Action(ActionType.KEY, keyEventChipGroup.chips[0].keyCode.toString())
            chooseSelectedAction(action)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        activity!!.unregisterReceiver(mBroadcastReceiver)
    }

    /**
     * Show a chip for a [KeyEvent]
     */
    private fun showKeyEventChip(event: KeyEvent) {
        keyEventChipGroup.removeAllChips()
        keyEventChipGroup.addChip(event)
    }
}