package io.github.sds100.keymapper.system.apps

/**
 * Created by sds100 on 27/10/2018.
 */

enum class PackageInfoTypes {
    TYPE_PACKAGE_NAME,
    TYPE_VIEW_ID,
}

object PackageUtils {

    fun getInfoFromFullyQualifiedViewName(name: String, infoType: PackageInfoTypes): String? {
        val splitted = name.split('/')

        if (splitted.isNotEmpty() && splitted.size == 2) {
            if (infoType.name == PackageInfoTypes.TYPE_VIEW_ID.name) {
                return splitted[1]
            } else if (infoType.name == PackageInfoTypes.TYPE_PACKAGE_NAME.name) {
                return splitted[0].replace(":id", "", true)
            }
        }

        return null
    }
}
