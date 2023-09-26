package io.github.sds100.keymapper.system.apps

import android.content.Context

/**
 * Created by sds100 on 27/10/2018.
 */

enum class PACKAGE_INFO_TYPES {
    TYPE_PACKAGE_NAME,
    TYPE_VIEW_ID
}

object PackageUtils {

    fun isAppInstalled(ctx: Context, packageName: String): Boolean {
        try {
            ctx.packageManager.getApplicationInfo(packageName, 0)

            return true

        } catch (e: Exception) {
            return false
        }
    }

    fun getInfoFromFullyQualifiedViewName(name: String, infoType: PACKAGE_INFO_TYPES): String? {
        val splitted = name.split('/')

        if (splitted.isNotEmpty() && splitted.size == 2) {
            if (infoType.name == PACKAGE_INFO_TYPES.TYPE_VIEW_ID.name) {
                return splitted[1]
            } else if (infoType.name == PACKAGE_INFO_TYPES.TYPE_PACKAGE_NAME.name) {
                return splitted[0].replace(":id", "", true )
            }
        }

        return null
    }

}