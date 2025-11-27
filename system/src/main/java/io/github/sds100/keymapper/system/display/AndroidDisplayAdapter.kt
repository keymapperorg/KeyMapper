package io.github.sds100.keymapper.system.display

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.getRealDisplaySize
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * Android implementation of DisplayAdapter.
 *
 * This is a Singleton that lives for the lifetime of the application.
 * Listeners (DisplayManager.DisplayListener, BroadcastReceiver, OrientationEventListener)
 * are intentionally not unregistered because the adapter needs to continuously track
 * display state changes throughout the app's lifecycle.
 */
@Singleton
class AndroidDisplayAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
) : DisplayAdapter {
    companion object {

        /**
         * How much to change the brightness by.
         */
        private const val BRIGHTNESS_CHANGE_STEP = 20

        /**
         * Tolerance in degrees for orientation detection.
         * This helps avoid rapid switching at orientation boundaries.
         */
        private const val ORIENTATION_TOLERANCE = 45
    }

    private val ctx = context.applicationContext

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            context ?: return

            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    // This intent is received before the assistant activity is launched so
                    // wait 100ms before updating. This is so that one can use the screen-off
                    // constraint with the assistant triggers.
                    coroutineScope.launch {
                        delay(100)

                        isScreenOn.update { true }
                    }
                }

                Intent.ACTION_SCREEN_OFF -> {
                    isAmbientDisplayEnabled.update { isAodEnabled() }
                    isScreenOn.update { false }
                }
            }
        }
    }

    override val isScreenOn = MutableStateFlow(true)

    private val displayManager: DisplayManager = ctx.getSystemService()!!

    private val _orientation = MutableStateFlow(getDisplayOrientation())
    override val orientation: Flow<Orientation> = _orientation
    override val cachedOrientation: Orientation
        get() = _orientation.value

    private val _physicalOrientation = MutableStateFlow(PhysicalOrientation.PORTRAIT)
    override val physicalOrientation: Flow<PhysicalOrientation> = _physicalOrientation
    override val cachedPhysicalOrientation: PhysicalOrientation
        get() = _physicalOrientation.value

    override val size: SizeKM
        get() = ctx.getRealDisplaySize()

    override val isAmbientDisplayEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(isAodEnabled())

    private val orientationEventListener = object : OrientationEventListener(ctx) {
        override fun onOrientationChanged(orientationDegrees: Int) {
            if (orientationDegrees == ORIENTATION_UNKNOWN) {
                return
            }

            val newPhysicalOrientation = degreesToPhysicalOrientation(orientationDegrees)
            _physicalOrientation.update { newPhysicalOrientation }
        }
    }

    init {
        displayManager.registerDisplayListener(
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {
                    _orientation.update { getDisplayOrientation() }
                }

                override fun onDisplayRemoved(displayId: Int) {
                    _orientation.update { getDisplayOrientation() }
                }

                override fun onDisplayChanged(displayId: Int) {
                    _orientation.update { getDisplayOrientation() }

                    // This listener API has lower latency than the broadcast receiver so also use this.
                    val isScreenStateOn = displayManager.displays.first().state == Display.STATE_ON
                    isScreenOn.update { isScreenStateOn }
                }
            },
            null,
        )

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)

        ContextCompat.registerReceiver(
            ctx,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Enable physical orientation detection
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
    }

    override fun isAutoRotateEnabled(): Boolean =
        SettingsUtils.getSystemSetting<Int>(ctx, Settings.System.ACCELEROMETER_ROTATION) == 1

    override fun enableAutoRotate(): KMResult<*> {
        val success = SettingsUtils.putSystemSetting(ctx, Settings.System.ACCELEROMETER_ROTATION, 1)

        if (success) {
            return Success(Unit)
        } else {
            return KMError.FailedToModifySystemSetting(Settings.System.ACCELEROMETER_ROTATION)
        }
    }

    override fun disableAutoRotate(): KMResult<*> {
        val success = SettingsUtils.putSystemSetting(ctx, Settings.System.ACCELEROMETER_ROTATION, 0)

        if (success) {
            return Success(Unit)
        } else {
            return KMError.FailedToModifySystemSetting(Settings.System.ACCELEROMETER_ROTATION)
        }
    }

    override fun setOrientation(orientation: Orientation): KMResult<*> {
        val sdkRotationValue = when (orientation) {
            Orientation.ORIENTATION_0 -> Surface.ROTATION_0
            Orientation.ORIENTATION_90 -> Surface.ROTATION_90
            Orientation.ORIENTATION_180 -> Surface.ROTATION_180
            Orientation.ORIENTATION_270 -> Surface.ROTATION_270
        }

        val success =
            SettingsUtils.putSystemSetting(ctx, Settings.System.USER_ROTATION, sdkRotationValue)

        if (success) {
            return Success(Unit)
        } else {
            return KMError.FailedToModifySystemSetting(Settings.System.USER_ROTATION)
        }
    }

    override fun isAutoBrightnessEnabled(): Boolean = SettingsUtils.getSystemSetting<Int>(
        ctx,
        Settings.System.SCREEN_BRIGHTNESS_MODE,
    ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    override fun increaseBrightness(): KMResult<*> {
        // auto-brightness must be disabled
        disableAutoBrightness()

        val currentBrightness =
            SettingsUtils.getSystemSetting<Int>(ctx, Settings.System.SCREEN_BRIGHTNESS)

        var newBrightness = if (currentBrightness != null) {
            currentBrightness + BRIGHTNESS_CHANGE_STEP
        } else {
            255
        }

        // the brightness must be between 0 and 255
        if (newBrightness > 255) {
            newBrightness = 255
        }

        val success =
            SettingsUtils.putSystemSetting(ctx, Settings.System.SCREEN_BRIGHTNESS, newBrightness)

        return if (success) {
            Success(Unit)
        } else {
            KMError.FailedToModifySystemSetting(Settings.System.SCREEN_BRIGHTNESS)
        }
    }

    override fun decreaseBrightness(): KMResult<*> {
        // auto-brightness must be disabled
        disableAutoBrightness()

        val currentBrightness =
            SettingsUtils.getSystemSetting<Int>(ctx, Settings.System.SCREEN_BRIGHTNESS)

        var newBrightness = if (currentBrightness != null) {
            currentBrightness - BRIGHTNESS_CHANGE_STEP
        } else {
            255
        }

        // the brightness must be between 0 and 255
        if (newBrightness < 0) {
            newBrightness = 0
        }

        val success =
            SettingsUtils.putSystemSetting(ctx, Settings.System.SCREEN_BRIGHTNESS, newBrightness)

        return if (success) {
            Success(Unit)
        } else {
            KMError.FailedToModifySystemSetting(Settings.System.SCREEN_BRIGHTNESS)
        }
    }

    override fun enableAutoBrightness(): KMResult<*> =
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)

    override fun disableAutoBrightness(): KMResult<*> =
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)

    private fun setBrightnessMode(mode: Int): KMResult<*> {
        val success =
            SettingsUtils.putSystemSetting(ctx, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)

        return if (success) {
            Success(Unit)
        } else {
            KMError.FailedToModifySystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE)
        }
    }

    override fun fetchOrientation(): Orientation {
        return _orientation.updateAndGet { getDisplayOrientation() }
    }

    private fun getDisplayOrientation(): Orientation =
        when (val sdkRotation = displayManager.displays[0].rotation) {
            Surface.ROTATION_0 -> Orientation.ORIENTATION_0
            Surface.ROTATION_90 -> Orientation.ORIENTATION_90
            Surface.ROTATION_180 -> Orientation.ORIENTATION_180
            Surface.ROTATION_270 -> Orientation.ORIENTATION_270

            else -> throw Exception("Don't know how to convert $sdkRotation to Orientation")
        }

    private fun isAodEnabled(): Boolean {
        return SettingsUtils.getSecureSetting<Int>(ctx, "doze_always_on") == 1
    }

    /**
     * Converts sensor orientation degrees to PhysicalOrientation.
     *
     * The orientation degrees from OrientationEventListener represent how much
     * the device is rotated from its natural orientation:
     * - 0°: Device is upright (portrait for most phones)
     * - 90°: Device is rotated 90° counter-clockwise (landscape, home button on right)
     * - 180°: Device is upside down (portrait inverted)
     * - 270°: Device is rotated 90° clockwise (landscape, home button on left)
     *
     * Using a tolerance helps avoid rapid orientation changes at boundaries.
     */
    private fun degreesToPhysicalOrientation(degrees: Int): PhysicalOrientation {
        // OrientationEventListener returns 0-359 degrees.
        // Handle wraparound at 0/360 boundary for portrait detection.
        // Degrees outside the defined tolerance zones (e.g., 45-89° between portrait and landscape)
        // are intentionally kept as the current orientation to provide hysteresis and prevent
        // rapid orientation switching when the device is tilted at boundary angles.
        return when {
            degrees >= (360 - ORIENTATION_TOLERANCE) ||
                degrees < ORIENTATION_TOLERANCE ->
                PhysicalOrientation.PORTRAIT
            degrees in (90 - ORIENTATION_TOLERANCE) until (90 + ORIENTATION_TOLERANCE) ->
                PhysicalOrientation.LANDSCAPE
            degrees in (180 - ORIENTATION_TOLERANCE) until (180 + ORIENTATION_TOLERANCE) ->
                PhysicalOrientation.PORTRAIT_INVERTED
            degrees in (270 - ORIENTATION_TOLERANCE) until (270 + ORIENTATION_TOLERANCE) ->
                PhysicalOrientation.LANDSCAPE_INVERTED
            else -> _physicalOrientation.value // Keep current orientation in transition zone
        }
    }
}
