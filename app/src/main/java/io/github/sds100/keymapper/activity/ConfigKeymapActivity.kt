package io.github.sds100.keymapper.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.adapter.TriggerAdapter
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_RECORD_TRIGGER
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_RECORD_TRIGGER_TIMER_INCREMENTED
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_STOP_RECORDING_TRIGGER
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_PERMISSION_DENIED
import io.github.sds100.keymapper.util.PermissionUtils.REQUEST_CODE_PERMISSION
import io.github.sds100.keymapper.viewmodel.ConfigKeyMapViewModel
import kotlinx.android.synthetic.main.activity_config_key_map.*
import kotlinx.android.synthetic.main.content_config_key_map.*
import org.jetbrains.anko.*

/**
 * Created by sds100 on 04/10/2018.
 */

abstract class ConfigKeymapActivity : AppCompatActivity() {

    companion object {
        const val ACTION_ADD_KEY_CHIP = "${Constants.PACKAGE_NAME}.ADD_KEY_CHIP"
        const val EXTRA_KEY_EVENT = "extra_key_event"

        const val REQUEST_CODE_ACTION = 821
        const val REQUEST_CODE_DEVICE_ADMIN = 213
    }

    /**
     * Listens for key events from the accessibility service
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_ADD_KEY_CHIP -> {
                    val keyEvent = intent.getParcelableExtra<KeyEvent>(EXTRA_KEY_EVENT)

                    //only add the chip to the group if it doesn't contain already it.
                    if (!chipGroupTriggerPreview.containsChip(keyEvent.keyCode)) {
                        chipGroupTriggerPreview.addChip(keyEvent)
                    }
                }

                ACTION_RECORD_TRIGGER_TIMER_INCREMENTED -> {
                    val timeLeft = intent.getLongExtra(MyAccessibilityService.EXTRA_TIME_LEFT, 5000L)
                    onIncrementRecordTriggerTimer(timeLeft)
                }

                ACTION_STOP_RECORDING_TRIGGER -> onStopRecordingTrigger()

                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    viewModel.keyMap.notifyObservers()
                }
            }
        }
    }

    private var mRecordTriggerDisabledTapTarget: TapTargetView? = null

    private val mTriggerAdapter = TriggerAdapter()

    private var mIsRecordingTrigger = false

    abstract val viewModel: ConfigKeyMapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_key_map)
        setSupportActionBar(toolbar)

        //this needs to be enabled for vector drawables from resources to work on kitkat
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        /*listen for broadcasts from the accessibility service to show chips and
         to stop recording a trigger */

        val intentFilter = IntentFilter()

        intentFilter.addAction(ACTION_ADD_KEY_CHIP)
        intentFilter.addAction(ACTION_STOP_RECORDING_TRIGGER)
        intentFilter.addAction(ACTION_RECORD_TRIGGER_TIMER_INCREMENTED)
        intentFilter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)

        registerReceiver(mBroadcastReceiver, intentFilter)

        //observing stuff
        viewModel.keyMap.observe(this, Observer {
            it?.let { keyMap ->
                doAsync {
                    val actionDescription = ActionUtils.getDescription(this@ConfigKeymapActivity, keyMap.action)

                    uiThread {
                        loadActionDescriptionLayout(actionDescription)
                    }
                }

                mTriggerAdapter.triggerList = keyMap.triggerList
                switchEnabled.isChecked = keyMap.isEnabled
            }
        })

        //button stuff
        buttonRecordTrigger.setOnClickListener {
            recordTrigger()
        }

        buttonClearKeys.setOnClickListener {
            sendBroadcast(Intent(MyAccessibilityService.ACTION_CLEAR_PRESSED_KEYS))
            chipGroupTriggerPreview.removeAllChips()
        }

        buttonChooseAction.setOnClickListener {
            val intent = Intent(this, ChooseActionActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ACTION)
        }

        buttonFlags.setOnClickListener {
            viewModel.keyMap.value?.let { keyMap ->
                FlagUtils.showFlagDialog(this, keyMap) { selectedItems ->
                    keyMap.flags = 0

                    selectedItems.forEach {
                        val flag = it.second
                        val isChecked = it.third

                        if (isChecked) {
                            keyMap.flags = addFlag(keyMap.flags, flag)

                            if (flag == FlagUtils.FLAG_LONG_PRESS) {
                                showLongPressWarning()
                            }
                        }
                    }
                }
            }
        }

        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            viewModel.keyMap.value!!.isEnabled = isChecked
        }

        recyclerViewTriggers.layoutManager = LinearLayoutManager(this)
        recyclerViewTriggers.adapter = mTriggerAdapter
    }

    override fun onResume() {
        super.onResume()

        /* disable "Record Trigger" button if the service is disabled because otherwise the button
         * wouldn't do anything*/
        val isAccessibilityServiceEnabled = AccessibilityUtils.isServiceEnabled(this)

        buttonRecordTrigger.isEnabled = isAccessibilityServiceEnabled

        if (isAccessibilityServiceEnabled) {
            mRecordTriggerDisabledTapTarget?.dismiss(true)
        } else {
            val tapTarget = TapTarget.forView(buttonRecordTrigger,
                str(R.string.showcase_record_trigger_title),
                str(R.string.showcase_record_trigger_description)).apply {
                tintTarget(false)
            }

            mRecordTriggerDisabledTapTarget = TapTargetView.showFor(this, tapTarget)
        }

        /* reload the action description since the user could have left the app and uninstalled
        the app chosen as the action so an error message should now be displayed */
        viewModel.keyMap.notifyObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_config_key_map, menu)
        return true
    }

    override fun onBackPressed() {
        //ask the user whether they are sure they want to leave the activity
        alert {
            titleResource = R.string.dialog_title_are_you_sure
            messageResource = R.string.dialog_message_are_you_sure_want_to_leave
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
                viewModel.saveKeymap()

                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        if (mIsRecordingTrigger) onStopRecordingTrigger()

        super.onPause()
    }

    override fun onStop() {
        //If the user manages to leave the app, allow the accessibility service to accept key events
        //so they can use the device
        if (mIsRecordingTrigger) onStopRecordingTrigger()

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }

    //When the user chooses an action in ChooseActionActivity, the result is returned here
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_ACTION -> {
                if (data != null) {
                    viewModel.keyMap.action =
                        Gson().fromJson(data.getStringExtra(Action.EXTRA_ACTION))
                }
            }

            /* need to refresh the action description layout so it stops showing an error message after they've enabled
            * the device admin. */
            REQUEST_CODE_DEVICE_ADMIN -> viewModel.keyMap.notifyObservers()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    //reload the ActionDescriptionLayout so it stops saying the app needs permission.
                    viewModel.keyMap.notifyObservers()
                }
            }
        }
    }

    /**
     * Start recording a new trigger
     */
    private fun recordTrigger() {
        mIsRecordingTrigger = true
        buttonRecordTrigger.text = str(R.string.button_recording_trigger)
        buttonRecordTrigger.isEnabled = false

        //tell the accessibility service to record key events
        sendBroadcast(Intent(ACTION_RECORD_TRIGGER))
    }

    private fun onIncrementRecordTriggerTimer(timeLeft: Long) {
        buttonRecordTrigger.isEnabled = false
        buttonRecordTrigger.text = str(R.string.button_recording_trigger_countdown, timeLeft / 1000)
    }

    /**
     * What to do when a trigger has stopped being recorded
     */
    private fun onStopRecordingTrigger() {
        mIsRecordingTrigger = false

        buttonRecordTrigger.text = getString(R.string.button_record_trigger)
        buttonRecordTrigger.isEnabled = true

        val trigger = chipGroupTriggerPreview.createTriggerFromChips()

        if (trigger.keys.isNotEmpty()) {
            viewModel.keyMap.addTrigger(trigger)
        }

        chipGroupTriggerPreview.removeAllChips()
    }

    private fun testAction() {
        val action = viewModel.keyMap.action

        if (action != null) {
            val intent = Intent(MyAccessibilityService.ACTION_TEST_ACTION)
            intent.putExtra(MyAccessibilityService.EXTRA_ACTION, action)

            sendBroadcast(intent)
        }
    }

    @UiThread
    private fun loadActionDescriptionLayout(actionDescription: ActionDescription) {
        actionDescription.apply {
            actionDescriptionLayout.setDescription(actionDescription)

            val isFixable = errorResult.isFixable

            /* if there is no error message, when the button is pressed, the user can test the
                action */
            if (errorCode == null) {
                buttonSecondary.text = getString(R.string.button_test)
                buttonSecondary.visibility = View.VISIBLE
            } else {
                //secondary button stuff.

                if (isFixable) {
                    buttonSecondary.text = getString(R.string.button_fix)
                }

                buttonSecondary.isVisible = isFixable
            }

            buttonSecondary.setOnClickListener {
                if (isFixable) {
                    if (errorCode == ERROR_CODE_PERMISSION_DENIED) {
                        PermissionUtils.requestPermission(this@ConfigKeymapActivity, errorResult?.data!!)
                    }

                    errorResult?.fix(this@ConfigKeymapActivity)
                } else {
                    testAction()
                }
            }
        }
    }

    private fun showLongPressWarning() {
        if (defaultSharedPreferences.getBoolean(str(R.string.key_pref_show_long_press_warning), true)) {
            alert {
                messageResource = R.string.dialog_message_long_press_warning
                okButton { }
                negativeButton(R.string.neg_dont_show_again) {
                    defaultSharedPreferences.edit {
                        putBoolean(str(R.string.key_pref_show_long_press_warning), false).apply()
                    }
                }
            }.show()
        }
    }
}
