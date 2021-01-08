package io.github.sds100.keymapper.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.GET_META_DATA
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 27/01/2020.
 */
class SystemRepository(private val mContext: Context) {

    suspend fun getAllAppList() = withContext(Dispatchers.Default) {
        sequence {
            mContext.packageManager.apply {

                val installedApps = getInstalledApplications(GET_META_DATA)

                installedApps.forEach { app ->
                    yield(app)
                }
            }

        }.toList()
    }

    suspend fun getLaunchableAppList() = withContext(Dispatchers.Default) {
        sequence {
            mContext.packageManager.apply {

                val installedApps = getInstalledApplications(GET_META_DATA)

                installedApps.forEach { app ->
                    //only allow apps that can be launched by the user
                    if (getLaunchIntentForPackage(app.packageName) != null) {
                        yield(app)
                    }
                }
            }

        }.toList()
    }

    suspend fun getAppShortcutList(): List<ResolveInfo> = withContext(Dispatchers.Default) {
        val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        return@withContext mContext.packageManager.queryIntentActivities(shortcutIntent, 0)
    }

    fun getAppIcon(applicationInfo: ApplicationInfo): Drawable? {
        return applicationInfo.loadIcon(mContext.packageManager)
    }

    fun getIntentIcon(resolveInfo: ResolveInfo): Drawable? {
        return resolveInfo.loadIcon(mContext.packageManager)
    }

    fun getAppName(applicationInfo: ApplicationInfo): String {
        return applicationInfo.loadLabel(mContext.packageManager).toString()
    }

    fun getAppName(packageName: String): String {
        val applicationInfo = mContext.packageManager.getApplicationInfo(packageName, 0)
        return getAppName(applicationInfo)
    }

    fun getIntentLabel(resolveInfo: ResolveInfo): String {
        return resolveInfo.loadLabel(mContext.packageManager).toString()
    }
}