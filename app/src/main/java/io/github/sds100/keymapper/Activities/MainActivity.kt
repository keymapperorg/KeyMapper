package io.github.sds100.keymapper.Activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import io.github.sds100.keymapper.Adapters.KeyMapAdapter
import io.github.sds100.keymapper.BuildConfig
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.OnDeleteMenuItemClickListener
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Selection.SelectableActionMode
import io.github.sds100.keymapper.Selection.SelectionCallback
import io.github.sds100.keymapper.Selection.SelectionEvent
import io.github.sds100.keymapper.Selection.SelectionProvider
import io.github.sds100.keymapper.Services.MyAccessibilityService
import io.github.sds100.keymapper.Utils.NotificationUtils
import io.github.sds100.keymapper.ViewModels.KeyMapListViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), SelectionCallback, OnDeleteMenuItemClickListener {

    private val mKeyMapAdapter: KeyMapAdapter = KeyMapAdapter()
    private lateinit var mViewModel: KeyMapListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        NotificationUtils.showIMEPickerNotification(this)

        /*if the app is a debug build then enable the accessibility service in settings
        / automatically so I don't have to! :)*/
        if (BuildConfig.DEBUG) {
            MyAccessibilityService.enableServiceInSettings()
        }

        mViewModel = ViewModelProviders.of(this).get(KeyMapListViewModel::class.java)

        mViewModel.keyMapList.observe(this, Observer { keyMapList ->
            mKeyMapAdapter.itemList = keyMapList
            mKeyMapAdapter.notifyDataSetChanged()

            updateAccessibilityServiceKeymapCache(keyMapList)
        })

        //start NewKeyMapActivity when the fab is pressed
        fabNewKeyMap.setOnClickListener {
            val intent = Intent(this, NewKeyMapActivity::class.java)
            startActivity(intent)
        }

        mKeyMapAdapter.iSelectionProvider.subscribeToSelectionEvents(this)

        recyclerViewKeyMaps.layoutManager = LinearLayoutManager(this)
        recyclerViewKeyMaps.adapter = mKeyMapAdapter
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState!!.putBundle(
                SelectionProvider.KEY_SELECTION_PROVIDER_STATE,
                mKeyMapAdapter.iSelectionProvider.saveInstanceState())

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState!!.containsKey(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)) {
            val selectionProviderState =
                    savedInstanceState.getBundle(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)!!

            mKeyMapAdapter.iSelectionProvider.restoreInstanceState(selectionProviderState)
        }
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDeleteMenuButtonClick() {
        mViewModel.deleteKeyMapsById(*mKeyMapAdapter.iSelectionProvider.selectedItemIds)
    }

    override fun onSelectionEvent(id: Long?, event: SelectionEvent) {
        if (event == SelectionEvent.START) {
            startSupportActionMode(SelectableActionMode(this, mKeyMapAdapter.iSelectionProvider, this))
        }
    }

    private fun updateAccessibilityServiceKeymapCache(keyMapList: List<KeyMap>) {
        val intent = Intent(MyAccessibilityService.ACTION_UPDATE_KEYMAP_CACHE)
        val jsonString = Gson().toJson(keyMapList)

        intent.putExtra(MyAccessibilityService.EXTRA_KEYMAP_CACHE_JSON, jsonString)

        sendBroadcast(intent)
    }
}
