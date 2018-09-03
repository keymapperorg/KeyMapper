package io.github.sds100.keymapper.Activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Adapters.KeymapAdapter
import io.github.sds100.keymapper.Selection.SelectableActionMode
import io.github.sds100.keymapper.Selection.SelectionCallback
import io.github.sds100.keymapper.Services.MyAccessibilityService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), SelectionCallback {

    companion object {
        const val ACTION_REFRESH_KEY_MAP_LIST = "action_refresh_key_map_list"
    }

    private val mKeymapRepository by lazy { KeyMapRepository.getInstance(this) }
    private val mKeymapAdapter by lazy { KeymapAdapter(mKeymapRepository.getKeyMapList()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //start NewKeyMapActivity when the fab is pressed
        fabNewKeyMap.setOnClickListener {
            val intent = Intent(this, NewKeyMapActivity::class.java)
            startActivityForResult(intent, NewKeyMapActivity.REQUEST_CODE_NEW_KEY_MAP)
        }

        /*if the app is a debug build then enable the accessibility service in settings
        / automatically so I don't have to! :)*/
        if (BuildConfig.DEBUG) {
            MyAccessibilityService.enableServiceInSettings()
        }

        populateKeyMapRecyclerView()
        mKeymapAdapter.iSelectionProvider.subscribeToSelectionEvents(this)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == NewKeyMapActivity.REQUEST_CODE_NEW_KEY_MAP && resultCode == RESULT_OK) {
            if (data?.action == ACTION_REFRESH_KEY_MAP_LIST) {
                mKeymapAdapter.itemList = mKeymapRepository.getKeyMapList()
                mKeymapAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onStartMultiSelect() {
        startSupportActionMode(SelectableActionMode(this, mKeymapAdapter.iSelectionProvider))
    }

    override fun onStopMultiSelect() {}
    override fun onItemSelected(id: Long) {}
    override fun onItemUnselected(id: Long) {}

    /**
     * Populate [recyclerViewKeyMaps] with a list of key maps.
     * @see [KeyMap]
     */
    private fun populateKeyMapRecyclerView() {
        recyclerViewKeyMaps.layoutManager = LinearLayoutManager(this)
        recyclerViewKeyMaps.adapter = mKeymapAdapter
    }
}
