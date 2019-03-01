package io.github.sds100.keymapper.asynctask

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.AsyncTask

/**
 * Created by sds100 on 03/10/2018.
 */

class LoadAppListAsyncTask(
        private val mPackageManager: PackageManager,
        private val onResult: (result: List<ApplicationInfo>) -> Unit
) : AsyncTask<Unit, Int, List<ApplicationInfo>>() {

    override fun doInBackground(vararg params: Unit?): List<ApplicationInfo> {

        val userApps: List<ApplicationInfo> = sequence {

            //get all the installed apps
            val installedApps = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            installedApps.forEach { app ->
                //only allow apps which can be launched by the user
                if (mPackageManager.getLaunchIntentForPackage(app.packageName) != null) yield(app)
            }
        }.toList()

        //sort the apps in name order
        return userApps.sortedWith(Comparator { app1, app2 ->
            fun getAppName(app: ApplicationInfo) = app.loadLabel(mPackageManager).toString()

            getAppName(app1).compareTo(getAppName(app2), ignoreCase = true)
        })
    }

    override fun onPostExecute(result: List<ApplicationInfo>?) {
        this.onResult(result!!)

        super.onPostExecute(result)
    }
}