package io.github.sds100.keymapper.Activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.Adapters.TriggerAdapter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Services.MyAccessibilityService
import io.github.sds100.keymapper.Utils.ActionUtils
import io.github.sds100.keymapper.ViewModels.ConfigKeyMapViewModel
import kotlinx.android.synthetic.main.activity_config_key_map.*
import kotlinx.android.synthetic.main.content_config_key_map.*
import org.jetbrains.anko.alert

/**
 * Created by sds100 on 04/10/2018.
 */

abstract class ConfigKeymapActivity : AppCompatActivity() {

    companion object {
        const val ACTION_ADD_KEY_CHIP = "io.github.sds100.keymapper.ADD_KEY_CHIP"
        const val EXTRA_KEY_EVENT = "extra_key_event"

        const val REQUEST_CODE_ACTION = 821
    }

    /**
     * Listens for key events from the accessibility service
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val keyEvent = intent!!.getParcelableExtra<KeyEvent>(EXTRA_KEY_EVENT)

            when (intent.action) {
                ACTION_ADD_KEY_CHIP -> {
                    chipGroupTriggerPreview.addChip(keyEvent)
                }
                MyAccessibilityService.ACTION_RECORD_TRIGGER_TIMER_STOPPED -> {
                    stopRecordingTrigger()
                }
            }
        }
    }

    private val mTriggerAdapter = TriggerAdapter()

    private var mIsRecordingTrigger = false

    abstract val viewModel: ConfigKeyMapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_key_map)
        setSupportActionBar(toolbar)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //listen for key events so they can be shown as chips
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_ADD_KEY_CHIP)
        intentFilter.addAction(MyAccessibilityService.ACTION_RECORD_TRIGGER_TIMER_STOPPED)

        registerReceiver(mBroadcastReceiver, intentFilter)

        viewModel.keyMap.observe(this, Observer { keyMap ->

            if (keyMap.action != null) {
                val action = keyMap.action!!

                textViewAction.text = ActionUtils.getDescription(ctx = this, action = action)

                val drawable = ActionUtils.getIcon(ctx = this, action = action)

                if (drawable == null) {
                    imageView.setImageDrawable(null)
                    imageView.visibility = View.GONE
                } else {
                    imageView.setImageDrawable(drawable)
                    imageView.visibility = View.VISIBLE
                }

                buttonTestAction.visibility = View.VISIBLE
            } else {
                buttonTestAction.visibility = View.GONE
            }

            mTriggerAdapter.triggerList = keyMap.triggerList
        })

        buttonRecordTrigger.setOnClickListener {
            if (mIsRecordingTrigger) {
                addTrigger()
            } else {
                recordTrigger()
            }
        }

        buttonTestAction.setOnClickListener {
            val action = viewModel.keyMap.action

            if (action != null) {
                val intent = Intent(MyAccessibilityService.ACTION_TEST_ACTION)
                intent.putExtra(MyAccessibilityService.EXTRA_ACTION, action)

                sendBroadcast(intent)
            }
        }

        buttonClearKeys.setOnClickListener {
            sendBroadcast(Intent(MyAccessibilityService.ACTION_CLEAR_PRESSED_KEYS))
            chipGroupTriggerPreview.removeAllChips()
        }

        buttonChooseAction.setOnClickListener {
            val intent = Intent(this, ChooseActionActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ACTION)
        }

        recyclerViewTriggers.layoutManager = LinearLayoutManager(this)
        recyclerViewTriggers.adapter = mTriggerAdapter
    }

    override fun onResume() {
        super.onResume()

        /* disable "Record Trigger" button if the service is disabled because otherwise the button
         * wouldn't do anything*/
        val isAccessibilityServiceEnabled =
                MyAccessibilityService.isAccessibilityServiceEnabled(this, coordinatorLayout)

        buttonRecordTrigger.isEnabled = isAccessibilityServiceEnabled
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_config_key_map, menu)
        return true
    }

    override fun onBackPressed() {
        //ask the user whether they are sure they want to leave the activity
        alert {
            title = getString(R.string.dialog_title_are_you_sure)
            message = getString(R.string.dialog_message_are_you_sure_want_to_leave)
            positiveButton(android.R.string.yes, onClicked = { super.onBackPressed() })
            negativeButton(android.R.string.no, onClicked = { dialog -> dialog.cancel() })
        }.show()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            /*when the back button in the toolbar is pressed, call onBackPressed so it acts like the
            hardware back button */
            android.R.id.home -> {
                onBackPressed()
                true
            }

            R.id.action_done -> {
                //if the key map isn't valid, return.
                if (!viewModel.keyMap.value!!.isValid(this)) {
                    return true
                }

                viewModel.saveKeymap()

                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        if (mIsRecordingTrigger) stopRecordingTrigger()

        super.onPause()
    }

    override fun onStop() {
        //If the user manages to leave the app, allow the accessibility service to accept key events
        //so they can use the device
        if (mIsRecordingTrigger) stopRecordingTrigger()

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }

    //When the user chooses an action in ChooseActionActivity, the result is returned here
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ACTION) {
            if (data != null) {
                viewModel.keyMap.action =
                        Gson().fromJson(data.getStringExtra(Action.EXTRA_ACTION))
            }
        }
    }

    private fun addTrigger() {
        val trigger = chipGroupTriggerPreview.createTriggerFromChips()

        if (trigger.keys.isNotEmpty()) {
            viewModel.keyMap.addTrigger(trigger)
        }

        stopRecordingTrigger()
    }

    /**
     * Start recording a new trigger
     */
    private fun recordTrigger() {
        mIsRecordingTrigger = true
        buttonRecordTrigger.text = getString(R.string.button_stop_recording_trigger)

        //tell the accessibility service to record key events
        val intent = Intent(MyAccessibilityService.ACTION_RECORD_TRIGGER)
        sendBroadcast(intent)
    }

    /**
     * Stop recording a new trigger
     */
    private fun stopRecordingTrigger() {
        mIsRecordingTrigger = false

        buttonRecordTrigger.text = getString(R.string.button_record_trigger)

        chipGroupTriggerPreview.removeAllChips()

        //tell the accessibility service to stop recording key events
        val intent = Intent(MyAccessibilityService.ACTION_STOP_RECORDING_TRIGGER)
        sendBroadcast(intent)
    }
}