package io.github.sds100.keymapper.system.accessibility

import android.content.Intent
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

    override fun getController(): BaseAccessibilityServiceController? {
        return controller
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        /*
        I would put this in onCreate but for some reason on some devices getting the application
        context would return null
         */
        if (controller == null) {
            controller = controllerFactory.create(this)
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

        super.onDestroy()
    }
}
