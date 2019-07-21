package io.github.sds100.keymapper.fragment

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.adapter.AppListAdapter
import io.github.sds100.keymapper.asynctask.LoadAppListAsyncTask
import io.github.sds100.keymapper.interfaces.OnItemClickListener
import kotlinx.android.synthetic.main.recyclerview_fragment.*

/**
 * Created by sds100 on 29/07/2018.
 */

/**
 * A Fragment which shows a list of all the installed apps
 */
class AppActionTypeFragment : FilterableActionTypeFragment(), OnItemClickListener<ApplicationInfo> {

    private lateinit var mAppList: List<ApplicationInfo>
    private var mAppListAdapter: AppListAdapter? = null

    override val filterable
        get() = mAppListAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val packageManager = context!!.packageManager

        LoadAppListAsyncTask(
                packageManager,
                onResult = { result ->
                    mAppList = result
                    if (mAppListAdapter == null) {
                        mAppListAdapter = AppListAdapter(
                                onItemClickListener = this@AppActionTypeFragment,
                                mAppList = mAppList,
                                mPackageManager = packageManager
                        )
                    }

                    //the task may be finished even if the fragment isn't showing
                    if (recyclerView != null) {
                        recyclerView.adapter = mAppListAdapter
                    }

                    if (progressBar != null) {
                        progressBar.visibility = View.GONE
                    }
                }).execute()

        progressBar.visibility = View.VISIBLE
        recyclerView.layoutManager = LinearLayoutManager(context!!)
    }

    override fun onItemClick(item: ApplicationInfo) {
        val action = Action(ActionType.APP, item.packageName)
        chooseSelectedAction(action)
    }
}