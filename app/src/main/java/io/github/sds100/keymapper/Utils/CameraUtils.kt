package io.github.sds100.keymapper.Utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Created by sds100 on 12/01/2019.
 */

object CameraUtils {

    @RequiresApi(Build.VERSION_CODES.M)
    fun setFlashlightMode(ctx: Context, enabled: Boolean) {
        //get the CameraManager
        (ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager).apply {

            for (cameraId in cameraIdList) {
                if (getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!) {
                    setTorchMode(cameraId, enabled)
                }
            }
        }
    }
}