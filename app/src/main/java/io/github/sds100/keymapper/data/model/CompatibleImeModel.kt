package io.github.sds100.keymapper.data.model

import android.content.Context
import android.os.Build
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.BuildUtils
import io.github.sds100.keymapper.util.PackageUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 24/07/20.
 */
class CompatibleImeListItemModel private constructor(
    val packageName: String,
    val imeName: String,
    val description: String,
    val errorMessage: String? = null,
    val playStoreLink: String? = null,
    val fdroidLink: String? = null,
    val githubLink: String? = null,
    val xdaLink: String? = null,
    val showLinks: Boolean
) {
    companion object {
        fun build(
            ctx: Context,
            packageName: String,
            minApi: Int = Constants.MIN_API,
            imeName: String,
            description: String,
            playStoreLink: String? = null,
            fdroidLink: String? = null,
            githubLink: String? = null,
            xdaLink: String? = null
        ): CompatibleImeListItemModel {

            var showLinks = false

            val errorMessage = when {
                Build.VERSION.SDK_INT < minApi -> {
                    ctx.str(R.string.error_sdk_version_too_low, BuildUtils.getSdkVersionName(minApi))
                }

                !PackageUtils.isAppInstalled(packageName) -> {
                    showLinks = true
                    ctx.str(R.string.error_app_isnt_installed_brief)
                }

                else -> null
            }

            return CompatibleImeListItemModel(
                packageName,
                imeName,
                description,
                errorMessage,
                playStoreLink,
                fdroidLink,
                githubLink,
                xdaLink,
                showLinks
            )
        }
    }

    val isSupported: Boolean
        get() = errorMessage == null
}