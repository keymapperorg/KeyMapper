package io.github.sds100.keymapper.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.FingerprintGestureController
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.ActivityManager
import android.app.Service
import android.content.*
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.github.sds100.keymapper.api.Api
import io.github.sds100.keymapper.api.IKeyEventReceiver
import io.github.sds100.keymapper.api.IKeyEventReceiverCallback
import io.github.sds100.keymapper.api.KeyEventReceiver
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapId
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber


/**
 * Created by sds100 on 05/04/2020.
 */

class MyAccessibilityService : AccessibilityService(), LifecycleOwner, IAccessibilityService {

    /**
     * Broadcast receiver for all intents sent from within the app.
     */
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                Api.ACTION_TRIGGER_KEYMAP_BY_UID -> {
                    intent.getStringExtra(Api.EXTRA_KEYMAP_UID)?.let {
                        controller?.triggerKeyMapFromIntent(it)
                    }
                }
            }
        }
    }

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private var fingerprintGestureCallback:
            FingerprintGestureController.FingerprintGestureCallback? = null

    override val rootNode: AccessibilityNodeModel?
        get() {
            return rootInActiveWindow?.toModel()
        }

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

    private val keyEventReceiverCallback: IKeyEventReceiverCallback = object : IKeyEventReceiverCallback.Stub() {
        override fun onKeyEvent(event: KeyEvent?): Boolean {
            event ?: return false

            val device = if (event.device == null) {
                null
            } else {
                InputDeviceUtils.createInputDeviceInfo(event.device)
            }

            if (controller != null) {
                return controller!!.onKeyEventFromIme(
                    event.keyCode,
                    event.action,
                    device,
                    event.metaState,
                    event.scanCode,
                    event.eventTime
                )
            }

            return false
        }
    }

    private val keyEventReceiverLock: Any = Any()
    private var keyEventReceiverBinder: IKeyEventReceiver? = null

    private val keyEventReceiverConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(keyEventReceiverLock) {
                keyEventReceiverBinder = IKeyEventReceiver.Stub.asInterface(service)
                keyEventReceiverBinder?.registerCallback(keyEventReceiverCallback)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(keyEventReceiverLock) {
                keyEventReceiverBinder?.unregisterCallback(keyEventReceiverCallback)
                keyEventReceiverBinder = null
            }
        }
    }

    private var controller: AccessibilityServiceController? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("Accessibility service: onCreate")

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        IntentFilter().apply {
            addAction(Api.ACTION_TRIGGER_KEYMAP_BY_UID)
            addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
            registerReceiver(broadcastReceiver, this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            softKeyboardController.addOnShowModeChangedListener { _, showMode ->
                when (showMode) {
                    SHOW_MODE_AUTO -> _isKeyboardHidden.value = false
                    SHOW_MODE_HIDDEN -> _isKeyboardHidden.value = true
                }
            }
        }

        Intent(this, KeyEventReceiver::class.java).also { intent ->
            bindService(intent, keyEventReceiverConnection, Service.BIND_AUTO_CREATE)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        Timber.i("Accessibility service: onServiceConnected")

        /*
        I would put this in onCreate but for some reason on some devices getting the application
        context would return null
         */
        if (controller == null) {
            controller = Inject.accessibilityServiceController(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            fingerprintGestureCallback = object : FingerprintGestureController.FingerprintGestureCallback() {
                override fun onGestureDetected(gesture: Int) {
                    super.onGestureDetected(gesture)

                    val id: FingerprintMapId = when (gesture) {
                        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN ->
                            FingerprintMapId.SWIPE_DOWN

                        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP ->
                            FingerprintMapId.SWIPE_UP

                        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT ->
                            FingerprintMapId.SWIPE_LEFT

                        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT ->
                            FingerprintMapId.SWIPE_RIGHT

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

        controller = null

        if (::lifecycleRegistry.isInitialized) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        unregisterReceiver(broadcastReceiver)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fingerprintGestureController
                .unregisterFingerprintGestureCallback(fingerprintGestureCallback)
        }

        unbindService(keyEventReceiverConnection)

        Timber.i("Accessibility service: onDestroy")

        super.onDestroy()
    }

    override fun onLowMemory() {

        val memoryInfo = ActivityManager.MemoryInfo()
        getSystemService<ActivityManager>()?.getMemoryInfo(memoryInfo)

        Timber.i("Accessibility service: onLowMemory, total: ${memoryInfo.totalMem}, available: ${memoryInfo.availMem}, is low memory: ${memoryInfo.lowMemory}, threshold: ${memoryInfo.threshold}")

        super.onLowMemory()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        controller?.onAccessibilityEvent(event?.toModel())
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
                    event.keyCode,
                    event.action,
                    device,
                    event.metaState,
                    event.scanCode,
                    event.eventTime
            )
        }

        return false
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

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

    override fun switchIme(imeId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            softKeyboardController.switchToInputMethod(imeId)
        }
    }

    override fun performActionOnNode(
            findNode: (node: AccessibilityNodeModel) -> Boolean,
            performAction: (node: AccessibilityNodeModel) -> AccessibilityNodeAction?
    ): Result<*> {
        val node = rootInActiveWindow.findNodeRecursively {
            findNode(it.toModel())
        }

        if (node == null) {
            return Error.FailedToFindAccessibilityNode
        }

        val (action, extras) = performAction(node.toModel()) ?: return Success(Unit)

        node.performAction(action, bundleOf(*extras.toList().toTypedArray()))
        node.recycle()

        return Success(Unit)
    }

    override fun doGlobalAction(action: Int): Result<*> {
        val success = performGlobalAction(action)

        if (success) {
            return Success(Unit)
        } else {
            return Error.FailedToPerformAccessibilityGlobalAction(action)
        }
    }

    override fun tapScreen(x: Int, y: Int, inputEventType: InputEventType): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val duration = 1L //ms

            val path = Path().apply {
                moveTo(x.toFloat(), y.toFloat())
            }

            val strokeDescription =
                    when {
                        inputEventType == InputEventType.DOWN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                            GestureDescription.StrokeDescription(
                                    path,
                                    0,
                                    duration,
                                    true
                            )

                        inputEventType == InputEventType.UP && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                            GestureDescription.StrokeDescription(
                                    path,
                                    59999,
                                    duration,
                                    false
                            )

                        else -> GestureDescription.StrokeDescription(path, 0, duration)
                    }

            strokeDescription.let {
                val gestureDescription = GestureDescription.Builder().apply {
                    addStroke(it)
                }.build()

                val success = dispatchGesture(gestureDescription, null, null)

                return if (success) {
                    Success(Unit)
                } else {
                    Error.FailedToDispatchGesture
                }
            }
        }

        return Error.SdkVersionTooLow(Build.VERSION_CODES.N)
    }

    override fun swipeScreen(xStart: Int, yStart: Int, xEnd: Int, yEnd: Int, fingerCount: Int, duration: Int, inputEventType: InputEventType): Result<*> {
        Timber.d("ACCESSIBILITY SWIPE SCREEN %d, %d, %d, %d, %s, %d, %s", xStart, yStart, xEnd, yEnd, fingerCount, duration, inputEventType);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val pStart = Point(xStart, yStart)
            val pEnd = Point(xEnd, yEnd)

            val gestureBuilder = GestureDescription.Builder()

            if (fingerCount == 1) {
                val p = Path()
                p.moveTo(pStart.x.toFloat(), pStart.y.toFloat())
                p.lineTo(pEnd.x.toFloat(), pEnd.y.toFloat())
                gestureBuilder.addStroke(StrokeDescription(p, 0, duration.toLong()))
            } else {
                // virtual distance between the fingers
                val fingerDistance = 10L
                // segments between fingers
                val segmentCount = fingerCount - 1
                // the line of the perpendicular line which will be created to place the virtual fingers on it
                val perpendicularLineLength = (fingerDistance * fingerCount).toInt()
                // the length of each segment between fingers
                val segmentLength = perpendicularLineLength / segmentCount
                // perpendicular line of the start swipe point
                val perpendicularLineStart = getPerpendicularOfLine(pStart, pEnd,
                    perpendicularLineLength
                );
                // perpendicular line of the end swipe point
                val perpendicularLineEnd = getPerpendicularOfLine(pEnd, pStart,
                    perpendicularLineLength, true);


                val startFingerCoordinatesList = mutableListOf<Point>()
                val endFingerCoordinatesList = mutableListOf<Point>()
                // this is the angle between start and end point to rotate all virtual fingers on the perpendicular lines in the same direction
                val angle = angleBetweenPoints(Point(xStart, yStart), Point(xEnd, yEnd)) - 90;

                // create the virtual fingers
                for (index in 0..segmentCount) {
                    // offset of each finger
                    val fingerOffsetLength = index * segmentLength * 2;
                    // move the coordinates of the current virtual finger on the perpendicular line for the start coordinates
                    val startFingerCoordinateWithOffset = movePointByDistanceAndAngle(perpendicularLineStart.start, fingerOffsetLength, angle)
                    // move the coordinates of the current virtual finger on the perpendicular line for the end coordinates
                    val endFingerCoordinateWithOffset = movePointByDistanceAndAngle(perpendicularLineEnd.start, fingerOffsetLength, angle)

                    // create a path for each finger, move the the coordinates on the perpendicular line and draw it to the end coordinates of the perpendicular line of the end swipe point
                    val p = Path()
                    p.moveTo(startFingerCoordinateWithOffset.x.toFloat(), startFingerCoordinateWithOffset.y.toFloat())
                    p.lineTo(endFingerCoordinateWithOffset.x.toFloat(), endFingerCoordinateWithOffset.y.toFloat())

                    //startFingerCoordinatesList.add(startFingerCoordinateWithOffset)
                    //endFingerCoordinatesList.add(endFingerCoordinateWithOffset)
                    gestureBuilder.addStroke(StrokeDescription(p, 0, duration.toLong()))
                }

            }

            val success = dispatchGesture(gestureBuilder.build(), null, null);

            return if (success) {
                Success(Unit)
            } else {
                Error.FailedToDispatchGesture
            }
        }

        return Error.SdkVersionTooLow(Build.VERSION_CODES.N)
    }

    override fun findFocussedNode(focus: Int): AccessibilityNodeModel? {
        return findFocus(focus)?.toModel()
    }
}