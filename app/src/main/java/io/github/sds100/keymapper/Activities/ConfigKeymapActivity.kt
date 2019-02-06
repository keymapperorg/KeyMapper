package io.github.sds100.keymapper.Activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Adapters.TriggerAdapter
import io.github.sds100.keymapper.Services.MyAccessibilityService
import io.github.sds100.keymapper.Utils.ActionUtils
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_PERMISSION_DENIED
import io.github.sds100.keymapper.Utils.FlagUtils
import io.github.sds100.keymapper.Utils.PermissionUtils
import io.github.sds100.keymapper.Utils.RootUtils
import io.github.sds100.keymapper.ViewModels.ConfigKeyMapViewModel
import kotlinx.android.synthetic.main.activity_config_key_map.*
import kotlinx.android.synthetic.main.content_config_key_map.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by sds100 on 04/10/2018.
 */

abstract class ConfigKeymapActivity : AppCompatActivity() {

    companion object {
        const val ACTION_ADD_KEY_CHIP = "${Constants.PACKAGE_NAME}.ADD_KEY_CHIP"
        const val EXTRA_KEY_EVENT = "extra_key_event"

        const val REQUEST_CODE_ACTION = 821
        const val PERMISSION_REQUEST_CODE = 344
    }

    /**
     * Listens for key events from the accessibility service
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_ADD_KEY_CHIP -> {
                    val keyEvent = intent.getParcelableExtra<KeyEvent>(EXTRA_KEY_EVENT)

                    chipGroupTriggerPreview.addChip(keyEvent)
                }

                MyAccessibilityService.ACTION_RECORD_TRIGGER_TIMER_STOPPED -> {
                    stopRecordingTrigger()
                }

                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    viewModel.keyMap.notifyObservers()
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

        //this needs to be enabled for vector drawables from resources to work on kitkat
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        /*listen for broadcasts from the accessibility service to show chips and
         to stop recording a trigger */

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_ADD_KEY_CHIP)
        intentFilter.addAction(MyAccessibilityService.ACTION_RECORD_TRIGGER_TIMER_STOPPED)
        intentFilter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)

        registerReceiver(mBroadcastReceiver, intentFilter)

        //observing stuff
        viewModel.keyMap.observe(this, Observer { keyMap ->
            doAsync {
                val actionDescription = ActionUtils.getDescription(this@ConfigKeymapActivity, keyMap.action)

                uiThread {
                    loadActionDescriptionLayout(actionDescription)

                    mTriggerAdapter.triggerList = keyMap.triggerList
                }
            }

            switchEnabled.isChecked = keyMap.isEnabled
        })

        //button stuff
        buttonRecordTrigger.setOnClickListener {
            if (mIsRecordingTrigger) {
                addTrigger()
            } else {
                recordTrigger()
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

        buttonFlags.setOnClickListener {
            viewModel.keyMap.value!!.let { keyMap ->
                FlagUtils.showFlagDialog(this, keyMap) { newItems ->
                    keyMap.flags.clear()

                    newItems.forEach {
                        val flag = it.second
                        val isChecked = it.third

                        if (isChecked) {
                            keyMap.flags.add(flag)
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
        val isAccessibilityServiceEnabled =
                MyAccessibilityService.isServiceEnabled(this)

        buttonRecordTrigger.isEnabled = isAccessibilityServiceEnabled

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

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    //reload the ActionDescriptionLayout so it stops saying the app needs permission.
                    viewModel.keyMap.notifyObservers()
                }
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
                        requestPermission(errorResult?.data!!)
                    }

                    errorResult?.fix(this@ConfigKeymapActivity)
                } else {
                    testAction()
                }
            }
        }
    }


    private fun requestPermission(permission: String) {
        if (PermissionUtils.requiresActivityToRequest(permission)) {

            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)

        } else {
            //WRITE_SETTINGS permission only has to be granted on Marshmallow or higher
            if (permission == Manifest.permission.WRITE_SETTINGS &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                //open settings to grant permission{
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${Constants.PACKAGE_NAME}")
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                startActivity(intent)

            } else if (permission == Constants.PERMISSION_ROOT) {
                RootUtils.promptForRootPermission(this)
            }
        }
    }
}
