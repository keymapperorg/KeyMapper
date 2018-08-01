package io.github.sds100.keymapper.ActionTypeFragments

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.Adapters.AppListAdapter
import io.github.sds100.keymapper.Adapters.SimpleItemAdapter
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.action_type_recyclerview.*
import kotlin.coroutines.experimental.buildSequence

/**
 * Created by sds100 on 29/07/2018.
 */

class AppActionTypeFragment : ActionTypeFragment(),
        SimpleItemAdapter.OnItemClickListener<ApplicationInfo> {

    private val mApps by lazy { getApps() }

    private lateinit var mAppListAdapter: AppListAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_type_recyclerview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAppListAdapter = AppListAdapter(
                mApps,
                packageManager = this@AppActionTypeFragment.context!!.packageManager,
                onItemClickListener = this@AppActionTypeFragment
        )

        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.adapter = mAppListAdapter
    }

    override fun onItemClick(item: ApplicationInfo) {
        val action = Action(Action.TYPE_APP, item.packageName)
        chooseSelectedAction(action)
    }

    private fun getApps(): List<ApplicationInfo> {
        val packageManager = context!!.packageManager

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