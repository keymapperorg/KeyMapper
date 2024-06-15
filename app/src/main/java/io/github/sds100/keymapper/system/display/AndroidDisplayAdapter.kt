package io.github.sds100.keymapper.system.display

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Surface
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.system.SettingsUtils
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by sds100 on 17/04/2021.
 */
class AndroidDisplayAdapter(context: Context) : DisplayAdapter {
    companion object {

        /**
         * How much to change the brightness by.
         */
        private const val BRIGHTNESS_CHANGE_STEP = 20
    }

    private val ctx = context.applicationContext

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            context ?: return

            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> isScreenOn.value = true
                Intent.ACTION_SCREEN_OFF -> isScreenOn.value = false
            }
        }
    }

    override val isScreenOn = MutableStateFlow(true)

    private val displayManager: DisplayManager = ctx.getSystemService()!!

    init {
        displayManager.registerDisplayListener(
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {
                    orientation = getDisplayOrientation()
                }

                override fun onDisplayRemoved(displayId: Int) {
                    orientation = getDisplayOrientation()
                }

                override fun onDisplayChanged(displayId: Int) {
                    orientation = getDisplayOrientation()
                }
            },
            null,
        )
    }

    override var orientation: Orientation = getDisplayOrientation()

    init {
        IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)

            ctx.registerReceiver(broadcastReceiver, this)
        }
    }

    override fun isAutoRotateEnabled(): Boolean =
        SettingsUtils.getSystemSetting<Int>(ctx, Settings.System.ACCELEROMETER_ROTATION) == 1

    override fun enableAutoRotate(): Result<*> {
        val success = SettingsUtils.putSystemSetting(ctx, Settings.System.ACCELEROMETER_ROTATION, 1)

        if (success) {
            return Success(Unit)
        } else {
            return Error.FailedToModifySystemSetting(Settings.System.ACCELEROMETER_ROTATION)
        }
    }

    override fun disableAutoRotate(): Result<*> {
        val success = SettingsUtils.putSystemSetting(ctx, Settings.System.ACCELEROMETER_ROTATION, 0)

        if (success) {
            return Success(Unit)
        } else {
            return Error.FailedToModifySystemSetting(Settings.System.ACCELEROMETER_ROTATION)
        }
    }

    override fun setOrientation(orientation: Orientation): Result<*> {
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
            return Error.FailedToModifySystemSetting(Settings.System.USER_ROTATION)
        }
    }

    override fun isAutoBrightnessEnabled(): Boolean = SettingsUtils.getSystemSetting<Int>(
        ctx,
        Settings.System.SCREEN_BRIGHTNESS_MODE,
    ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    override fun increaseBrightness(): Result<*> {
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
            Error.FailedToModifySystemSetting(Settings.System.SCREEN_BRIGHTNESS)
        }
    }

    override fun decreaseBrightness(): Result<*> {
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
            Error.FailedToModifySystemSetting(Settings.System.SCREEN_BRIGHTNESS)
        }
    }

    override fun enableAutoBrightness(): Result<*> =
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)

    override fun disableAutoBrightness(): Result<*> =
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)

    private fun setBrightnessMode(mode: Int): Result<*> {
        val success =
            SettingsUtils.putSystemSetting(ctx, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)

        return if (success) {
            Success(Unit)
        } else {
            Error.FailedToModifySystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE)
        }
    }

    private fun getDisplayOrientation(): Orientation =
        when (val sdkRotation = displayManager.displays[0].rotation) {
            Surface.ROTATION_0 -> Orientation.ORIENTATION_0
            Surface.ROTATION_90 -> Orientation.ORIENTATION_90
            Surface.ROTATION_180 -> Orientation.ORIENTATION_180
            Surface.ROTATION_270 -> Orientation.ORIENTATION_270

            else -> throw Exception("Don't know how to convert $sdkRotation to Orientation")
        }
}
