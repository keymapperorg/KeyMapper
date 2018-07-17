package io.github.sds100.keymapper

import android.app.ProgressDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.Adapters.ActionTypeSpinnerAdapter
import io.github.sds100.keymapper.Adapters.AppListAdapter
import kotlinx.android.synthetic.main.activity_choose_action.*
import kotlinx.android.synthetic.main.content_choose_action.*
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.buildSequence

class ChooseActionActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    //The adapters for the RecyclerView
    private var mAppListAdapter: AppListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_action)
        setSupportActionBar(toolbar)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        spinnerActionTypes.adapter = ActionTypeSpinnerAdapter(this)
        spinnerActionTypes.onItemSelectedListener = this

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
        /*when the back button in the toolbar is pressed, call onBackPressed so it acts like the
        hardware back button */
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (ActionTypeSpinnerAdapter.getItemTypeFromPosition(position)) {
            ActionTypeSpinnerAdapter.Item.APP -> {
                editText.visibility = View.GONE
                populateRecyclerViewWithApps()
            }
            ActionTypeSpinnerAdapter.Item.APP_SHORTCUT -> TODO()
            ActionTypeSpinnerAdapter.Item.KEY -> TODO()
            ActionTypeSpinnerAdapter.Item.ACTION -> TODO()
            ActionTypeSpinnerAdapter.Item.SETTING -> TODO()
            ActionTypeSpinnerAdapter.Item.TEXT_BLOCK -> TODO()
            ActionTypeSpinnerAdapter.Item.KEYCODE -> TODO()
        }
    }

    /**
     * Show all installed apps in the RecyclerView
     */
    private fun populateRecyclerViewWithApps() {
        if (mAppListAdapter == null) {
            val progress = ProgressDialog.show(this, "Loading Apps...",
                    "loading...", true)

            launch {
                val apps = getApps(packageManager)

                mAppListAdapter = AppListAdapter(apps, packageManager)

                runOnUiThread {
                    progress.dismiss()
                    recyclerView.adapter = mAppListAdapter
                }
            }
        } else {
            recyclerView.adapter = mAppListAdapter
        }
    }

    private fun getApps(packageManager: PackageManager): List<ApplicationInfo> {
        val userApps: List<ApplicationInfo> = buildSequence {

            //get all the installed apps
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            installedApps.forEach { app ->
                //only allow apps which can be launched by the user
                if (packageManager.getLaunchIntentForPackage(app.packageName) != null) yield(app)
            }
        }.toList()

        //sort the apps in name order
        return userApps.sortedWith(Comparator { app1, app2 ->
            fun getAppName(app: ApplicationInfo) = app.loadLabel(packageManager).toString()

            getAppName(app1).compareTo(getAppName(app2), ignoreCase = true)
        })
    }
}
