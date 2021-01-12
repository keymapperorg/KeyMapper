package io.github.sds100.keymapper.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

/**
 * Created by sds100 on 12/01/21.
 */
interface PackageRepository {
    suspend fun getAllAppList(): List<ApplicationInfo>
    suspend fun getLaunchableAppList(): List<ApplicationInfo>
    fun getAppName(applicationInfo: ApplicationInfo): String
    fun getAppIcon(applicationInfo: ApplicationInfo): Drawable?
    fun getAppName(packageName: String): String
    fun getIntentLabel(resolveInfo: ResolveInfo): String
    suspend fun getAppShortcutList(): List<ResolveInfo>
    fun getIntentIcon(resolveInfo: ResolveInfo): Drawable?
}