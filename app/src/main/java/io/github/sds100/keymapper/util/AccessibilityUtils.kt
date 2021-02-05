package io.github.sds100.keymapper.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.service.TapperAccessibilityService
import io.github.sds100.keymapper.ui.activity.HomeActivity

/**
 * Created by sds100 on 06/08/2019.
 */

object AccessibilityUtils {
    fun enableService(ctx: Context) {
        when {
            PermissionUtils.haveWriteSecureSettingsPermission(ctx) -> {
                val enabledServices = SettingsUtils.getSecureSetting<String>(
                    ctx,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )

                val className = MyAccessibilityService::class.java.name

                val keyMapperEntry = "${Constants.PACKAGE_NAME}/$className"

                val newEnabledServices = if (enabledServices == null) {
                    keyMapperEntry
                } else {
                    //append the keymapper entry to the rest of the other services.
                    "$keyMapperEntry:$enabledServices"
                }

                SettingsUtils.putSecureSetting(ctx,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newEnabledServices)
            }

            else -> openAccessibilitySettings(ctx)
        }
    }

    fun disableService(ctx: Context) {
        when {
            PermissionUtils.haveWriteSecureSettingsPermission(ctx) -> {
                val enabledServices = SettingsUtils.getSecureSetting<String>(
                    ctx,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )

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

                SettingsUtils.putSecureSetting(
                    ctx,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    newEnabledServices
                )
            }

            else -> openAccessibilitySettings(ctx)
        }
    }

    private fun openAccessibilitySettings(context: Context) {
        try {
            val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

            context.startActivity(settingsIntent)

        } catch (e: ActivityNotFoundException) {
            //open the app to show a dialog to tell the user to give the app WRITE_SECURE_SETTINGS permission
            Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(HomeActivity.KEY_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG, true)

                context.startActivity(this)
            }
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
            return settingValue.split(':').any {
                ComponentName.unflattenFromString(it) == ComponentName(ctx, MyAccessibilityService::class.java)
            }
        }

        return false
    }

    fun isTapperServiceEnabled(ctx: Context): Boolean {
        /* get a list of all the enabled accessibility services.
         * The AccessibilityManager.getEnabledAccessibilityServices() method just returns an empty
         * list. :(*/
        val settingValue = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        //it can be null if the user has never interacted with accessibility settings before
        if (settingValue != null) {
            return settingValue.split(':').any {
                ComponentName.unflattenFromString(it) == ComponentName(ctx, TapperAccessibilityService::class.java)
            }
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

fun AccessibilityNodeInfo?.focusedNode(func: (node: AccessibilityNodeInfo?) -> Unit) {
    func.invoke(findNodeRecursively { it.isFocused })
}

fun AccessibilityNodeInfo?.performActionOnFocusedNode(action: Int) {
    focusedNode {
        it?.performAction(action)
        it?.recycle()
    }
}