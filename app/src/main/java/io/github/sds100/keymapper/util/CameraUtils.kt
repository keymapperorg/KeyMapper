package io.github.sds100.keymapper.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Created by sds100 on 24/12/2019.
 */

object CameraUtils {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun hasFlashFacing(ctx: Context, face: Int): Boolean {
        (ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager).apply {
            return cameraIdList.toList().any { cameraId ->
                val camera = getCameraCharacteristics(cameraId)
                val hasFlash = camera.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: return false

                return hasFlash && camera.get(CameraCharacteristics.LENS_FACING) == face
            }
        }
    }
}