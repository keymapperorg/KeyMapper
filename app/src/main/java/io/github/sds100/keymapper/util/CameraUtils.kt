package io.github.sds100.keymapper.util

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.annotation.RequiresApi
import splitties.systemservices.cameraManager

/**
 * Created by sds100 on 24/12/2019.
 */

object CameraUtils {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun hasFlashFacing(face: Int): Boolean {
        cameraManager.apply {
            return cameraIdList.toList().any { cameraId ->
                val camera = getCameraCharacteristics(cameraId)
                val hasFlash = camera.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: return false

                return hasFlash && camera.get(CameraCharacteristics.LENS_FACING) == face
            }
        }
    }
}