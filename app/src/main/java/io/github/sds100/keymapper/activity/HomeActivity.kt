package io.github.sds100.keymapper.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import io.github.sds100.keymapper.BuildConfig
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.KeymapAdapterModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.adapter.KeymapAdapter
import io.github.sds100.keymapper.data.KeyMapRepository
import io.github.sds100.keymapper.interfaces.OnItemClickListener
import io.github.sds100.keymapper.selection.SelectableActionMode
import io.github.sds100.keymapper.selection.SelectionCallback
import io.github.sds100.keymapper.selection.SelectionEvent
import io.github.sds100.keymapper.selection.SelectionProvider
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.service.MyIMEService
import io.github.sds100.keymapper.util.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import org.jetbrains.anko.*

class HomeActivity : AppCompatActivity(), SelectionCallback,
        OnItemClickListener<KeymapAdapterModel>, MenuItem.OnMenuItemClickListener {

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                Intent.ACTION_INPUT_METHOD_CHANGED -> updateActionDescriptions()
            }
        }
    }

    private val mKeymapAdapter: KeymapAdapter = KeymapAdapter(this)
    private val mRepository by lazy { KeyMapRepository.getInstance(application.applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(appBar)

        if (defaultSharedPreferences.getBoolean(
                        str(R.string.key_pref_show_notification),
                        bool(R.bool.default_value_show_notifications))) {
            NotificationUtils.showIMEPickerNotification(this)
        } else {
            NotificationUtils.hideImePickerNotification(this)
        }

        /*if the app is a debug build then enable the accessibility service in settings
        / automatically so I don't have to! :)*/
        if (BuildConfig.DEBUG) {
            MyAccessibilityService.enableServiceInSettings()
        }

        mRepository.keyMapList.observe(this, Observer { keyMapList ->
            populateKeymapsAsync(keyMapList)

            updateAccessibilityServiceKeymapCache(keyMapList)
        })

        //start NewKeymapActivity when the fab is pressed
        fabNewKeyMap.setOnClickListener {
            val intent = Intent(this, NewKeymapActivity::class.java)
            startActivity(intent)
        }

        accessibilityServiceStatusLayout.setOnFixClickListener(View.OnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            startActivity(intent)
        })

        imeServiceStatusLayout.setOnFixClickListener(View.OnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            startActivity(intent)
        })

        mKeymapAdapter.iSelectionProvider.subscribeToSelectionEvents(this)

        //recyclerview stuff
        recyclerViewKeyMaps.layoutManager = LinearLayoutManager(this)
        recyclerViewKeyMaps.adapter = mKeymapAdapter

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
        registerReceiver(mBroadcastReceiver, intentFilter)

        //ask the user whether they want to enable analytics
        val isFirstTime = defaultSharedPreferences.getBoolean(
                str(R.string.key_pref_first_time), true
        )

        defaultSharedPreferences.edit {
            if (isFirstTime) {
                alert {
                    titleResource = R.string.title_pref_data_collection
                    messageResource = R.string.summary_pref_data_collection
                    positiveButton(R.string.pos_opt_in) {
                        putBoolean(str(R.string.key_pref_data_collection), true).commit()
                        setFirebaseDataCollection()
                        putBoolean(str(R.string.key_pref_first_time), false).commit()
                    }

                    negativeButton(R.string.neg_opt_out) {
                        putBoolean(str(R.string.key_pref_data_collection), false).commit()
                        setFirebaseDataCollection()
                        putBoolean(str(R.string.key_pref_first_time), false).commit()
                    }
                }.show()

            } else {
                setFirebaseDataCollection()
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        val selectedItemIds = mKeymapAdapter.iSelectionProvider.selectedItemIds

        return when (item?.itemId) {
            R.id.action_delete -> {
                mRepository.deleteKeyMapById(*selectedItemIds)
                mKeymapAdapter.iSelectionProvider.stopSelecting()
                true
            }
            R.id.action_enable -> {
                mRepository.enableKeymapById(*selectedItemIds)
                true
            }

            R.id.action_disable -> {
                mRepository.disableKeymapById(*selectedItemIds)
                true
            }

            else -> false
        }
    }

    override fun onResume() {
        super.onResume()

        if (MyAccessibilityService.isServiceEnabled(this)) {
            accessibilityServiceStatusLayout.changeToServiceEnabledState()
        } else {
            accessibilityServiceStatusLayout.changeToServiceDisabledState()
        }

        if (MyIMEService.isServiceEnabled(this)) {
            imeServiceStatusLayout.changeToServiceEnabledState()
        } else {
            imeServiceStatusLayout.changeToServiceDisabledState()
        }

        updateActionDescriptions()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState!!.putBundle(
                SelectionProvider.KEY_SELECTION_PROVIDER_STATE,
                mKeymapAdapter.iSelectionProvider.saveInstanceState())

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState!!.containsKey(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)) {
            val selectionProviderState =
                    savedInstanceState.getBundle(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)!!

            mKeymapAdapter.iSelectionProvider.restoreInstanceState(selectionProviderState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.action_help -> {
                val intent = Intent(this, HelpActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.action_disable_all_keymaps -> {
                mRepository.disableAllKeymaps()
                true
            }

            R.id.action_enable_all_keymaps -> {
                mRepository.enableAllKeymaps()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectionEvent(id: Long?, event: SelectionEvent) {
        if (event == SelectionEvent.START) {
            val actionMode = SelectableActionMode(
                    mCtx = this,
                    mISelectionProvider = mKeymapAdapter.iSelectionProvider,
                    mMenuId = R.menu.menu_multi_select,
                    mOnMenuItemClickListener = this)
            startSupportActionMode(actionMode)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setStatusBarColor(R.color.actionModeStatusBar)
            }

        } else if (event == SelectionEvent.STOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setStatusBarColor(R.color.colorPrimaryDark)
            }
        }
    }

    override fun onItemClick(item: KeymapAdapterModel) {
        val intent = Intent(this, EditKeymapActivity::class.java)
        intent.putExtra(EditKeymapActivity.EXTRA_KEYMAP_ID, item.id)

        startActivity(intent)
    }

    private fun updateAccessibilityServiceKeymapCache(keyMapList: List<KeyMap>) {
        val intent = Intent(MyAccessibilityService.ACTION_UPDATE_KEYMAP_CACHE)
        val jsonString = Gson().toJson(keyMapList)

        intent.putExtra(MyAccessibilityService.EXTRA_KEYMAP_CACHE_JSON, jsonString)

        sendBroadcast(intent)
    }

    private fun populateKeymapsAsync(keyMapList: List<KeyMap>) {
        doAsync {
            val adapterModels = mutableListOf<KeymapAdapterModel>()

            keyMapList.forEach { keyMap ->

                val actionDescription = ActionUtils.getDescription(this@HomeActivity, keyMap.action)

                adapterModels.add(KeymapAdapterModel(keyMap, actionDescription))
            }

            mKeymapAdapter.itemList = adapterModels

            uiThread {
                mKeymapAdapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                setCaption()
            }
        }
    }

    private fun updateActionDescriptions() {
        doAsync {
            mKeymapAdapter.itemList.forEach { model ->
                val keyMapId = model.id
                val keyMap = mRepository.keyMapList.value!!.find { it.id == keyMapId }

                if (keyMap != null) {
                    val actionDescription = ActionUtils.getDescription(this.weakRef.get()!!.baseContext, keyMap.action)
                    model.actionDescription = actionDescription
                }
            }

            uiThread {
                mKeymapAdapter.invalidateBoundViewHolders()
            }
        }
    }

    /**
     * Controls what message is displayed to the user on the home-screen
     */
    private fun setCaption() {
        //tell the user if they haven't created any KeyMaps
        if (mKeymapAdapter.itemCount == 0) {
            val spannableBuilder = SpannableStringBuilder()

            spannableBuilder.append(getString(R.string.shrug), RelativeSizeSpan(2f))
            spannableBuilder.append("\n\n")
            spannableBuilder.append(getString(R.string.no_key_maps))

            textViewCaption.visibility = View.VISIBLE
            textViewCaption.text = spannableBuilder
        } else {
            textViewCaption.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setStatusBarColor(@ColorRes colorId: Int) {
        window.statusBarColor = color(colorId)
    }

    private fun setFirebaseDataCollection() {
        val isDataCollectionEnabled = defaultSharedPreferences.getBoolean(
                str(R.string.key_pref_data_collection),
                bool(R.bool.default_value_data_collection))

        FirebaseAnalytics.getInstance(this@HomeActivity).setAnalyticsCollectionEnabled(isDataCollectionEnabled)
    }
}
