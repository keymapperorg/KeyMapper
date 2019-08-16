package io.github.sds100.keymapper.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.service.MyAccessibilityService
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton

/**
 * Created by sds100 on 06/08/2019.
 */

object AccessibilityUtils {
    fun enableService(ctx: Context) {
        when {
            ctx.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS) -> {
                val enabledServices = ctx.getSecureSetting<String>(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

                val className = MyAccessibilityService::class.java.name

                val keyMapperEntry = "${Constants.PACKAGE_NAME}/$className"

                val newEnabledServices = if (enabledServices == null) {
                    keyMapperEntry
                } else {
                    "$keyMapperEntry:$enabledServices"
                }
                ctx.putSecureSetting(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newEnabledServices)
            }

            else -> openAccessibilitySettings(ctx)
        }
    }

    fun disableService(ctx: Context) {
        when {
            ctx.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS) -> {
                val enabledServices = ctx.getSecureSetting<String>(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

                enabledServices ?: return

                val className = MyAccessibilityService::class.java.name

                val keyMapperEntry = "${Constants.PACKAGE_NAME}/$className"

                val newEnabledServices = if (enabledServices.contains(keyMapperEntry)) {
                    val services = enabledServices.split(':').toMutableList()
                    services.remove(keyMapperEntry)

                    services.joinToString(":")
                } else {
                    enabledServices
                }
                ctx.putSecureSetting(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newEnabledServices)
            }

            else -> openAccessibilitySettings(ctx)
        }
    }

    fun openAccessibilitySettings(ctx: Context) {
        try {
            val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            ctx.startActivity(settingsIntent)
        } catch (e: ActivityNotFoundException) {
            ctx.alert {
                titleResource = R.string.dialog_title_cant_find_accessibility_settings_page
                messageResource = R.string.dialog_message_cant_find_accessibility_settings_page

                okButton {
                    PermissionUtils.requestWriteSecureSettingsPermission(ctx)
                }
            }.show()
        }
    }

    /**
     * @return whether the accessibility service is enabled
     */
    fun isServiceEnabled(ctx: Context): Boolean {
        /* get a list of all the enabled accessibility services.
         * The AccessibilityManager.getEnabledAccessibilityServices() method just returns an empty
         * list. :(*/
        val settingValue = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        //it can be null if the user has never interacted with accessibility settings before
        if (settingValue != null) {
            /* cant just use .contains because the debug and release accessibility service both contain
               io.github.sds100.keymapper. the enabled_accessibility_services are stored as

                 io.github.sds100.keymapper.debug/io.github.sds100.keymapper.service.MyAccessibilityService
                 :io.github.sds100.keymapper/io.github.sds100.keymapper.service.MyAccessibilityService

                 without the new line before the :
            */
            return settingValue.split(':').any { it.split('/')[0] == ctx.packageName }
        }

        return false
    }
}

/**
 * @return The node to find. Returns null if the node doesn't match the predicate
 */
fun AccessibilityNodeInfo?.findNodeRecursively(
    nodeInfo: AccessibilityNodeInfo? = this,
    depth: Int = 0,
    predicate: (node: AccessibilityNodeInfo) -> Boolean
): AccessibilityNodeInfo? {
    if (nodeInfo == null) return null

    if (predicate(nodeInfo)) return nodeInfo

    for (i in 0 until nodeInfo.childCount) {
        val node = findNodeRecursively(nodeInfo.getChild(i), depth + 1, predicate)

        if (node != null) {
            return node
        }
    }

    return null
}