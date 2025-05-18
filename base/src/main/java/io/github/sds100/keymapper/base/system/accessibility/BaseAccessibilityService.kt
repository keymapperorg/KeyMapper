package io.github.sds100.keymapper.base.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.FingerprintGestureController
import android.app.ActivityManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback
import io.github.sds100.keymapper.api.KeyEventRelayService
import io.github.sds100.keymapper.api.KeyEventRelayServiceWrapperImpl
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.inputevents.MyKeyEvent
import io.github.sds100.keymapper.system.inputevents.MyMotionEvent
import io.github.sds100.keymapper.base.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.base.utils.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber



class BaseAccessibilityService :
    AccessibilityService(),
    LifecycleOwner,
    IAccessibilityService,
    SavedStateRegistryOwner {

    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private var savedStateRegistryController: SavedStateRegistryController? =
        SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController!!.savedStateRegistry

    private var fingerprintGestureCallback: FingerprintGestureController.FingerprintGestureCallback? =
        null

    override val rootNode: AccessibilityNodeModel?
        get() {
            return rootInActiveWindow?.toModel()
        }

    private val _activeWindowPackage: MutableStateFlow<String?> = MutableStateFlow(null)
    override val activeWindowPackage: Flow<String?> = _activeWindowPackage

    override val isFingerprintGestureDetectionAvailable: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fingerprintGestureController.isGestureDetectionAvailable
        } else {
            false
        }

    private val _isKeyboardHidden by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MutableStateFlow(softKeyboardController.showMode == SHOW_MODE_HIDDEN)
        } else {
            MutableStateFlow(false)
        }
    }

    override val isKeyboardHidden: Flow<Boolean>
        get() = _isKeyboardHidden

    override var serviceFlags: Int?
        get() = serviceInfo?.flags
        set(value) {
            if (serviceInfo != null && value != null) {
                serviceInfo = serviceInfo.apply {
                    flags = value
                }
            }
        }

    override var serviceFeedbackType: Int?
        get() = serviceInfo?.feedbackType
        set(value) {
            if (serviceInfo != null && value != null) {
                serviceInfo = serviceInfo.apply {
                    feedbackType = value
                }
            }
        }

    override var serviceEventTypes: Int?
        get() = serviceInfo?.eventTypes
        set(value) {
            if (serviceInfo != null && value != null) {
                serviceInfo = serviceInfo.apply {
                    eventTypes = value
                }
            }
        }

    override var notificationTimeout: Long?
        get() = serviceInfo?.notificationTimeout
        set(value) {
            if (serviceInfo != null && value != null) {
                serviceInfo = serviceInfo.apply {
                    notificationTimeout = value
                }
            }
        }

    private val relayServiceCallback: IKeyEventRelayServiceCallback =
        object : IKeyEventRelayServiceCallback.Stub() {
            override fun onKeyEvent(event: KeyEvent?): Boolean {
                event ?: return false

                val device = event.device?.let { InputDeviceUtils.createInputDeviceInfo(it) }

                if (controller != null) {
                    return controller!!.onKeyEventFromIme(
                        MyKeyEvent(
                            keyCode = event.keyCode,
                            action = event.action,
                            metaState = event.metaState,
                            scanCode = event.scanCode,
                            device = device,
                            repeatCount = event.repeatCount,
                            source = event.source,
                        ),
                    )
                }

                return false
            }

            override fun onMotionEvent(event: MotionEvent?): Boolean {
                event ?: return false

                if (controller != null) {
                    return controller!!.onMotionEventFromIme(MyMotionEvent.fromMotionEvent(event))
                }

                return false
            }
        }

    private val keyEventRelayServiceWrapper: KeyEventRelayServiceWrapperImpl by lazy {
        KeyEventRelayServiceWrapperImpl(
            ctx = this,
            id = KeyEventRelayService.CALLBACK_ID_ACCESSIBILITY_SERVICE,
            callback = relayServiceCallback,
        )
    }

    var controller: AccessibilityServiceController? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("Accessibility service: onCreate")

        savedStateRegistryController?.performAttach()
        savedStateRegistryController?.performRestore(null)

        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            softKeyboardController.addOnShowModeChangedListener { _, showMode ->
                when (showMode) {
                    SHOW_MODE_AUTO -> _isKeyboardHidden.value = false
                    SHOW_MODE_HIDDEN -> _isKeyboardHidden.value = true
                }
            }
        }

        keyEventRelayServiceWrapper.onCreate()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        Timber.i("Accessibility service: onServiceConnected")
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        setTheme(R.style.AppTheme)

        _activeWindowPackage.update { rootInActiveWindow?.packageName?.toString() }
        /*
        I would put this in onCreate but for some reason on some devices getting the application
        context would return null
         */
        if (controller == null) {
            controller = Inject.accessibilityServiceController(this, keyEventRelayServiceWrapper)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fingerprintGestureCallback =
                object : FingerprintGestureController.FingerprintGestureCallback() {
                    override fun onGestureDetected(gesture: Int) {
                        super.onGestureDetected(gesture)

                        val id: FingerprintGestureType = when (gesture) {
                            FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN ->
                                FingerprintGestureType.SWIPE_DOWN

                            FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP ->
                                FingerprintGestureType.SWIPE_UP

                            FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT ->
                                FingerprintGestureType.SWIPE_LEFT

                            FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT ->
                                FingerprintGestureType.SWIPE_RIGHT

                            else -> return
                        }
                        controller?.onFingerprintGesture(id)
                    }
                }

            fingerprintGestureCallback?.let {
                fingerprintGestureController.registerFingerprintGestureCallback(it, null)
            }
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
