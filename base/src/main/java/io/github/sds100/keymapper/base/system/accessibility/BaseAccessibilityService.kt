package io.github.sds100.keymapper.base.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.FingerprintGestureController
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.ActivityManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjectorImpl
import io.github.sds100.keymapper.base.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.common.utils.InputEventType
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.MathUtils
import io.github.sds100.keymapper.common.utils.PinchScreenType
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.inputevents.MyKeyEvent
import io.github.sds100.keymapper.system.inputevents.MyMotionEvent
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyEventRelayServiceWrapperImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseAccessibilityService :
    AccessibilityService(),
    LifecycleOwner,
    IAccessibilityService,
    SavedStateRegistryOwner {

    companion object {

        private const val CALLBACK_ID_ACCESSIBILITY_SERVICE = "accessibility_service"
    }

    @Inject
    lateinit var accessibilityServiceAdapter: AccessibilityServiceAdapterImpl

    @Inject
    lateinit var inputMethodAdapter: InputMethodAdapter

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

    override fun switchIme(imeId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            softKeyboardController.switchToInputMethod(imeId)
        }
    }

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

                return getController()
                    ?.onKeyEventFromIme(
                        MyKeyEvent(
                            keyCode = event.keyCode,
                            action = event.action,
                            metaState = event.metaState,
                            scanCode = event.scanCode,
                            device = device,
                            repeatCount = event.repeatCount,
                            source = event.source,
                        ),
                    ) ?: false
            }

            override fun onMotionEvent(event: MotionEvent?): Boolean {
                event ?: return false

                return getController()
                    ?.onMotionEventFromIme(MyMotionEvent.fromMotionEvent(event))
                    ?: return false
            }
        }

    val keyEventRelayServiceWrapper: KeyEventRelayServiceWrapperImpl by lazy {
        KeyEventRelayServiceWrapperImpl(
            ctx = this,
            id = CALLBACK_ID_ACCESSIBILITY_SERVICE,
            servicePackageName = packageName,
            callback = relayServiceCallback,
        )
    }

    val imeInputEventInjector by lazy {
        ImeInputEventInjectorImpl(
            this,
            keyEventRelayService = keyEventRelayServiceWrapper,
            inputMethodAdapter = inputMethodAdapter,
        )
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    abstract fun getController(): BaseAccessibilityServiceController?

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
                        getController()?.onFingerprintGesture(id)
                    }
                }

            fingerprintGestureCallback?.let {
                fingerprintGestureController.registerFingerprintGestureCallback(it, null)
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("Accessibility service: onUnbind")
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
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

        getController()?.onConfigurationChanged(newConfig)
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

        getController()?.onAccessibilityEvent(event)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return super.onKeyEvent(event)

        val device = if (event.device == null) {
            null
        } else {
            InputDeviceUtils.createInputDeviceInfo(event.device)
        }

        return getController()?.onKeyEvent(
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
        ) ?: false
    }

    override fun findFocussedNode(focus: Int): AccessibilityNodeModel? {
        return findFocus(focus)?.toModel()
    }

    override fun setInputMethodEnabled(imeId: String, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            softKeyboardController.setInputMethodEnabled(imeId, enabled)
        }
    }

    override fun hideKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            softKeyboardController.showMode = SHOW_MODE_HIDDEN
        }
    }

    override fun showKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            softKeyboardController.showMode = SHOW_MODE_AUTO
        }
    }

    override fun doGlobalAction(action: Int): KMResult<*> {
        val success = performGlobalAction(action)

        if (success) {
            return Success(Unit)
        } else {
            return KMError.FailedToPerformAccessibilityGlobalAction(action)
        }
    }

    override fun tapScreen(x: Int, y: Int, inputEventType: InputEventType): KMResult<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val duration = 1L // ms

            val path = Path().apply {
                moveTo(x.toFloat(), y.toFloat())
            }

            val strokeDescription =
                when {
                    inputEventType == InputEventType.DOWN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        StrokeDescription(
                            path,
                            0,
                            duration,
                            true,
                        )

                    inputEventType == InputEventType.UP && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        StrokeDescription(
                            path,
                            59999,
                            duration,
                            false,
                        )

                    else -> StrokeDescription(path, 0, duration)
                }

            strokeDescription.let {
                val gestureDescription = GestureDescription.Builder().apply {
                    addStroke(it)
                }.build()

                val success = dispatchGesture(gestureDescription, null, null)

                return if (success) {
                    Success(Unit)
                } else {
                    KMError.FailedToDispatchGesture
                }
            }
        }

        return KMError.SdkVersionTooLow(Build.VERSION_CODES.N)
    }

    override fun swipeScreen(
        xStart: Int,
        yStart: Int,
        xEnd: Int,
        yEnd: Int,
        fingerCount: Int,
        duration: Int,
        inputEventType: InputEventType,
    ): KMResult<*> {
        // virtual distance between fingers on multitouch gestures
        val fingerGestureDistance = 10L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (fingerCount >= GestureDescription.getMaxStrokeCount()) {
                return KMError.GestureStrokeCountTooHigh
            }
            if (duration >= GestureDescription.getMaxGestureDuration()) {
                return KMError.GestureDurationTooHigh
            }

            val pStart = Point(xStart, yStart)
            val pEnd = Point(xEnd, yEnd)

            val gestureBuilder = GestureDescription.Builder()

            if (fingerCount == 1) {
                val p = Path()
                p.moveTo(pStart.x.toFloat(), pStart.y.toFloat())
                p.lineTo(pEnd.x.toFloat(), pEnd.y.toFloat())
                gestureBuilder.addStroke(StrokeDescription(p, 0, duration.toLong()))
            } else {
                // segments between fingers
                val segmentCount = fingerCount - 1
                // the line of the perpendicular line which will be created to place the virtual fingers on it
                val perpendicularLineLength = (fingerGestureDistance * fingerCount).toInt()

                // the length of each segment between fingers
                val segmentLength = perpendicularLineLength / segmentCount
                // perpendicular line of the start swipe point
                val perpendicularLineStart = MathUtils.getPerpendicularOfLine(
                    pStart,
                    pEnd,
                    perpendicularLineLength,
                )
                // perpendicular line of the end swipe point
                val perpendicularLineEnd = MathUtils.getPerpendicularOfLine(
                    pEnd,
                    pStart,
                    perpendicularLineLength,
                    true,
                )

                // this is the angle between start and end point to rotate all virtual fingers on the perpendicular lines in the same direction
                val angle =
                    MathUtils.angleBetweenPoints(Point(xStart, yStart), Point(xEnd, yEnd)) - 90

                // create the virtual fingers
                for (index in 0..segmentCount) {
                    // offset of each finger
                    val fingerOffsetLength = index * segmentLength * 2
                    // move the coordinates of the current virtual finger on the perpendicular line for the start coordinates
                    val startFingerCoordinateWithOffset =
                        MathUtils.movePointByDistanceAndAngle(
                            perpendicularLineStart.start,
                            fingerOffsetLength,
                            angle,
                        )
                    // move the coordinates of the current virtual finger on the perpendicular line for the end coordinates
                    val endFingerCoordinateWithOffset =
                        MathUtils.movePointByDistanceAndAngle(
                            perpendicularLineEnd.start,
                            fingerOffsetLength,
                            angle,
                        )

                    // create a path for each finger, move the the coordinates on the perpendicular line and draw it to the end coordinates of the perpendicular line of the end swipe point
                    val p = Path()
                    p.moveTo(
                        startFingerCoordinateWithOffset.x.toFloat(),
                        startFingerCoordinateWithOffset.y.toFloat(),
                    )
                    p.lineTo(
                        endFingerCoordinateWithOffset.x.toFloat(),
                        endFingerCoordinateWithOffset.y.toFloat(),
                    )

                    gestureBuilder.addStroke(StrokeDescription(p, 0, duration.toLong()))
                }
            }

            val success = dispatchGesture(gestureBuilder.build(), null, null)

            return if (success) {
                Success(Unit)
            } else {
                KMError.FailedToDispatchGesture
            }
        }

        return KMError.SdkVersionTooLow(Build.VERSION_CODES.N)
    }

    override fun pinchScreen(
        x: Int,
        y: Int,
        distance: Int,
        pinchType: PinchScreenType,
        fingerCount: Int,
        duration: Int,
        inputEventType: InputEventType,
    ): KMResult<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (fingerCount >= GestureDescription.getMaxStrokeCount()) {
                return KMError.GestureStrokeCountTooHigh
            }
            if (duration >= GestureDescription.getMaxGestureDuration()) {
                return KMError.GestureDurationTooHigh
            }

            val gestureBuilder = GestureDescription.Builder()
            val distributedPoints: List<Point> =
                MathUtils.distributePointsOnCircle(Point(x, y), distance.toFloat() / 2, fingerCount)

            for (index in distributedPoints.indices) {
                val p = Path()
                if (pinchType == PinchScreenType.PINCH_IN) {
                    p.moveTo(x.toFloat(), y.toFloat())
                    p.lineTo(
                        distributedPoints[index].x.toFloat(),
                        distributedPoints[index].y.toFloat(),
                    )
                } else {
                    p.moveTo(
                        distributedPoints[index].x.toFloat(),
                        distributedPoints[index].y.toFloat(),
                    )
                    p.lineTo(x.toFloat(), y.toFloat())
                }

                gestureBuilder.addStroke(StrokeDescription(p, 0, duration.toLong()))
            }

            val success = dispatchGesture(gestureBuilder.build(), null, null)

            return if (success) {
                Success(Unit)
            } else {
                KMError.FailedToDispatchGesture
            }
        }

        return KMError.SdkVersionTooLow(Build.VERSION_CODES.N)
    }

    override fun performActionOnNode(
        findNode: (node: AccessibilityNodeModel) -> Boolean,
        performAction: (node: AccessibilityNodeModel) -> AccessibilityNodeAction?,
    ): KMResult<*> {
        val node = rootInActiveWindow.findNodeRecursively {
            findNode(it.toModel())
        }

        if (node == null) {
            return KMError.FailedToFindAccessibilityNode
        }

        val (action, extras) = performAction(node.toModel()) ?: return Success(Unit)

        node.performAction(action, bundleOf(*extras.toList().toTypedArray()))
        node.recycle()

        return Success(Unit)
    }
}
