package io.github.sds100.keymapper.util

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.SparseBooleanArray
import androidx.annotation.RequiresApi
import androidx.core.util.set
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.github.sds100.keymapper.R
import splitties.systemservices.cameraManager
import splitties.toast.toast

/**
 * Created by sds100 on 19/01/2019.
 */

@RequiresApi(Build.VERSION_CODES.M)
class FlashlightController : LifecycleObserver {
    private val flashEnabled = SparseBooleanArray()

    private val torchCallback = @RequiresApi(Build.VERSION_CODES.M)
    object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)

            cameraManager.apply {
                try {
                    val camera = getCameraCharacteristics(cameraId)
                    val lensFacing = camera.get(CameraCharacteristics.LENS_FACING)!!

                    flashEnabled.put(lensFacing, enabled)
                } catch (e: Exception) {
                }
            }
        }
    }

    init {
        flashEnabled.put(CameraCharacteristics.LENS_FACING_FRONT, false)
        flashEnabled.put(CameraCharacteristics.LENS_FACING_BACK, false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun registerTorchCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraManager.registerTorchCallback(torchCallback, null)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unregisterTorchCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraManager.unregisterTorchCallback(torchCallback)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun toggleFlashlight(lens: Int = CameraCharacteristics.LENS_FACING_BACK) {
        flashEnabled[lens] = !flashEnabled[lens]

        setFlashlightMode(flashEnabled[lens], lens)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setFlashlightMode(enabled: Boolean, lens: Int = CameraCharacteristics.LENS_FACING_BACK) {
        //get the CameraManager
        cameraManager.apply {

            for (cameraId in cameraIdList) {
                try {
                    val flashAvailable = getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: return

                    val lensFacing = getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.LENS_FACING)

                    //try to find a camera with a flash
                    if (flashAvailable && lensFacing == lens) {
                        setTorchMode(cameraId, enabled)
                    }

                } catch (e: CameraAccessException) {
                    when (e.reason) {
                        CameraAccessException.CAMERA_IN_USE -> toast(R.string.error_camera_in_use)
                        CameraAccessException.CAMERA_DISCONNECTED -> toast(R.string.error_camera_disconnected)
                        CameraAccessException.CAMERA_DISABLED -> toast(R.string.error_camera_disabled)
                        CameraAccessException.CAMERA_ERROR -> toast(R.string.error_camera_error)
                        CameraAccessException.MAX_CAMERAS_IN_USE -> toast(R.string.error_max_cameras_in_use)
                        else -> toast(R.string.error_camera_access_exception)
                    }
                } catch (e: IllegalArgumentException) {
                }
            }
        }
    }
}