package io.github.sds100.keymapper.system.accessibility

import android.app.ActivityManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityService
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.inputevents.MyKeyEvent
import io.github.sds100.keymapper.trigger.KeyEventDetectionSource
import kotlinx.coroutines.flow.update
import timber.log.Timber


class MyAccessibilityService : BaseAccessibilityService() {

    var controller: AccessibilityServiceController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        /*
        I would put this in onCreate but for some reason on some devices getting the application
        context would return null
         */
        if (controller == null) {
            controller = AccessibilityServiceController()
        }

        controller?.onServiceConnected()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("Accessibility service: onUnbind")
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        controller?.onDestroy()
        controller = null

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fingerprintGestureController
                .unregisterFingerprintGestureCallback(fingerprintGestureCallback)
        }

        keyEventRelayServiceWrapper.onDestroy()

        Timber.i("Accessibility service: onDestroy")

        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        controller?.onConfigurationChanged(newConfig)
    }

    override fun onTrimMemory(level: Int) {
        val memoryInfo = ActivityManager.MemoryInfo()
        getSystemService<ActivityManager>()?.getMemoryInfo(memoryInfo)

        Timber.i("Accessibility service: onLowMemory, total: ${memoryInfo.totalMem}, available: ${memoryInfo.availMem}, is low memory: ${memoryInfo.lowMemory}, threshold: ${memoryInfo.threshold}")

        super.onTrimMemory(level)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            _activeWindowPackage.update { rootInActiveWindow?.packageName?.toString() }
        }

        controller?.onAccessibilityEvent(event)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return super.onKeyEvent(event)

        val device = if (event.device == null) {
            null
        } else {
            InputDeviceUtils.createInputDeviceInfo(event.device)
        }

        if (controller != null) {
            return controller!!.onKeyEvent(
                MyKeyEvent(
                    keyCode = event.keyCode,
                    action = event.action,
                    metaState = event.metaState,
                    scanCode = event.scanCode,
                    device = device,
                    repeatCount = event.repeatCount,
                    source = event.source,
                ),
                KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
            )
        }

        return false
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

}
