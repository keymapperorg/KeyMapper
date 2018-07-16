package io.github.sds100.keymapper

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Created by sds100 on 16/07/2018.
 */

class MyAccessibilityService : AccessibilityService() {
    companion object {
        /**
         * Enable this accessibility service in settings. REQUIRES ROOT
         */
        fun enableInSettings() {
            val className = MyAccessibilityService::class.java.name
            val packageName = this::class.java.`package`.name

            executeRootCommand("settings put secure enabled_accessibility_services $packageName/$className")
        }

        /**
         * Disable this accessibility service in settings. REQUIRES ROOT
         */
        fun disableInSettings() {
            val packageName = this::class.java.`package`.name

            executeRootCommand("settings put secure enabled_accessibility_services $packageName")
        }

        private fun executeRootCommand(command: String){
            Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        }
    }

    override fun onInterrupt() {

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }
}