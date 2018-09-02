package io.github.sds100.keymapper

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.Adapters.TriggerAdapter
import kotlinx.android.synthetic.main.activity_new_key_map.*
import kotlinx.android.synthetic.main.content_new_key_map.*
import org.jetbrains.anko.alert

class NewKeyMapActivity : AppCompatActivity() {

    companion object {
        const val ACTION_ADD_KEY_CHIP = "io.github.sds100.keymapper.ADD_KEY_CHIP"
        const val EXTRA_KEY_EVENT = "extra_key_event"

        const val REQUEST_CODE_NEW_KEY_MAP = 432
        const val REQUEST_CODE_ACTION = 821
    }

    private val mAddKeyChipBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val keyEvent = intent!!.getParcelableExtra<KeyEvent>(EXTRA_KEY_EVENT)

            when (intent.action) {
                ACTION_ADD_KEY_CHIP -> {
                    chipGroupTriggerPreview.addChip(keyEvent)
                }
            }
        }
    }

    private val mTriggerAdapter = TriggerAdapter()
    private val mKeymapRepository by lazy { KeyMapRepository.getInstance(this) }

    private var mChosenAction: Action? = null
        set(value) {
            textViewAction.text = Action.getDescription(ctx = this, action = value!!)

            val drawable = Action.getIcon(ctx = this, action = value)

            if (drawable == null) {
                imageView.setImageDrawable(null)
                imageView.visibility = View.GONE
            } else {
                imageView.setImageDrawable(drawable)
                imageView.visibility = View.VISIBLE
            }

            field = value
        }

    private var mRecordingTrigger = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_key_map)
        setSupportActionBar(toolbar)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        buttonRecordTrigger.setOnClickListener {
            if (mRecordingTrigger) {
                addTrigger()
                buttonRecordTrigger.text = getString(R.string.button_record_trigger)
            } else {
                recordTrigger()
                buttonRecordTrigger.text = getString(R.string.button_stop_recording_trigger)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_new_key_map, menu)
        return true
    }

    override fun onBackPressed() {
        //ask the user whether they are sure they want to leave the activity
        alert {
            title = getString(R.string.dialog_title_are_you_sure)
            message = getString(R.string.dialog_message_are_you_sure_cancel_new_key_map)
            positiveButton(R.string.pos_yes, onClicked = { super.onBackPressed() })
            negativeButton(R.string.neg_no, onClicked = { dialog -> dialog.cancel() })
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
                if (mChosenAction == null) {
                    Toast.makeText(this, R.string.error_must_choose_action, LENGTH_SHORT).show()
                    return true
                }

                val keyMap = KeyMap(
                        mKeymapRepository.createUniqueId(),
                        mTriggerAdapter.triggerList,
                        mChosenAction!!
                )

                if (!keyMap.isValid(this)) {
                    return true
                }

                mKeymapRepository.saveKeyMap(keyMap)

                //tell MainActivity to refresh the list of key maps since a new one was just created
                setResult(Activity.RESULT_OK, Intent(MainActivity.ACTION_REFRESH_KEY_MAP_LIST))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        //If the user manages to leave the app, allow the accessibility service to accept key events
        //so they can use the device
        if (mRecordingTrigger) stopRecordingTrigger()

        super.onStop()
    }

    //When the user chooses an action in ChooseActionActivity, the result is returned here
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ACTION) {
            if (data != null) {
                mChosenAction = Gson().fromJson(data.getStringExtra(Action.EXTRA_ACTION))
            }
        }
    }

    private fun addTrigger() {
        val trigger = chipGroupTriggerPreview.createTriggerFromChips()

        if (trigger.keys.isNotEmpty()) {
            mTriggerAdapter.addTrigger(trigger)
        }

        stopRecordingTrigger()
    }

    /**
     * Start recording a new trigger
     */
    private fun recordTrigger() {
        mRecordingTrigger = true

        //tell the accessibility service to record key events
        val intent = Intent(MyAccessibilityService.ACTION_RECORD_TRIGGER)
        sendBroadcast(intent)

        //listen for key events so they can be shown as chips
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_ADD_KEY_CHIP)

        registerReceiver(mAddKeyChipBroadcastReceiver, intentFilter)
    }

    /**
     * Stop recording a new trigger
     */
    private fun stopRecordingTrigger() {
        mRecordingTrigger = false
        chipGroupTriggerPreview.removeAllChips()

        //tell the accessibility service to stop recording key events
        val intent = Intent(MyAccessibilityService.ACTION_STOP_RECORDING_TRIGGER)
        sendBroadcast(intent)

        //stop listening for key events from the accessibility service
        unregisterReceiver(mAddKeyChipBroadcastReceiver)
    }
}
