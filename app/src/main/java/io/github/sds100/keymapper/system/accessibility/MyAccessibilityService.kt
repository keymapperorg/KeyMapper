package io.github.sds100.keymapper.system.accessibility

import android.content.Intent
import android.os.UserManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityService
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityServiceController
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class MyAccessibilityService : BaseAccessibilityService() {

    @Inject
    lateinit var controllerFactory: AccessibilityServiceController.Factory

    private var controller: AccessibilityServiceController? = null
    private var loggedLockedInitDelay = false

    private val userManager: UserManager? by lazy { getSystemService<UserManager>() }

    override fun getController(): BaseAccessibilityServiceController? {
        return controller
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        initializeControllerIfUserUnlocked()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!initializeControllerIfUserUnlocked()) {
            return
        }

        super.onAccessibilityEvent(event)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (!initializeControllerIfUserUnlocked()) {
            return false
        }

        return super.onKeyEvent(event)
    }

    private fun initializeControllerIfUserUnlocked(): Boolean {
        if (userManager?.isUserUnlocked == false) {
            if (!loggedLockedInitDelay) {
                Timber.i("Accessibility service: Delay init because locked.")
                loggedLockedInitDelay = true
            }

            return false
        }

        loggedLockedInitDelay = false

        /*
        I would put this in onCreate but for some reason on some devices getting the application
        context would return null
         */
        if (controller == null) {
            controller = controllerFactory.create(this)
            controller?.onServiceConnected()
        }

        return true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("Accessibility service: onUnbind")
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        controller?.onDestroy()
        controller = null

        super.onDestroy()
    }
}
