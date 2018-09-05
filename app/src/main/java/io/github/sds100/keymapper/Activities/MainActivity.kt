package io.github.sds100.keymapper.Activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.Adapters.KeyMapAdapter
import io.github.sds100.keymapper.BuildConfig
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Selection.SelectableActionMode
import io.github.sds100.keymapper.Selection.SelectionCallback
import io.github.sds100.keymapper.Services.MyAccessibilityService
import io.github.sds100.keymapper.ViewModels.KeyMapListViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), SelectionCallback {

    private val mKeyMapAdapter: KeyMapAdapter = KeyMapAdapter()
    private lateinit var mViewModel: KeyMapListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mViewModel = ViewModelProviders.of(this).get(KeyMapListViewModel::class.java)

        mViewModel.keyMapList.observe(this, Observer { keyMapList ->
            mKeyMapAdapter.itemList = keyMapList
            mKeyMapAdapter.notifyDataSetChanged()
        })

        //start NewKeyMapActivity when the fab is pressed
        fabNewKeyMap.setOnClickListener {
            val intent = Intent(this, NewKeyMapActivity::class.java)
            startActivity(intent)
        }

        /*if the app is a debug build then enable the accessibility service in settings
        / automatically so I don't have to! :)*/
        if (BuildConfig.DEBUG) {
            MyAccessibilityService.enableServiceInSettings()
        }

        mKeyMapAdapter.iSelectionProvider.subscribeToSelectionEvents(this)

        recyclerViewKeyMaps.layoutManager = LinearLayoutManager(this)
        recyclerViewKeyMaps.adapter = mKeyMapAdapter
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

    override fun onStartMultiSelect() {
        startSupportActionMode(SelectableActionMode(this, mKeyMapAdapter.iSelectionProvider))
    }

    override fun onStopMultiSelect() {}
    override fun onItemSelected(id: Long) {}
    override fun onItemUnselected(id: Long) {}
}
