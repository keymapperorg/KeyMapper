package io.github.sds100.keymapper.delegate

import android.content.Context
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
import io.github.sds100.keymapper.interfaces.IContext
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 19/01/2019.
 */

@RequiresApi(Build.VERSION_CODES.M)
class FlashlightController(iContext: IContext) : IContext by iContext, LifecycleObserver {
    private val mFlashEnabled = SparseBooleanArray()

    private val mTorchCallback = @RequiresApi(Build.VERSION_CODES.M)
    object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)

            (ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager).apply {
                try {
                    val camera = getCameraCharacteristics(cameraId) ?: return
                    val lensFacing = camera.get(CameraCharacteristics.LENS_FACING)!!

                    mFlashEnabled.put(lensFacing, enabled)
                } catch (e: Exception) {
                }
            }
        }
    }

    init {
        mFlashEnabled.put(CameraCharacteristics.LENS_FACING_FRONT, false)
        mFlashEnabled.put(CameraCharacteristics.LENS_FACING_BACK, false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun registerTorchCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.registerTorchCallback(mTorchCallback, null)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unregisterTorchCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.unregisterTorchCallback(mTorchCallback)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun toggleFlashlight(lens: Int = CameraCharacteristics.LENS_FACING_BACK) {
        mFlashEnabled[lens] = !mFlashEnabled[lens]

        setFlashlightMode(mFlashEnabled[lens], lens)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setFlashlightMode(enabled: Boolean, lens: Int = CameraCharacteristics.LENS_FACING_BACK) {
        //get the CameraManager
        (ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager).apply {

            for (cameraId in cameraIdList) {
                try {
                    val flashAvailable =
                            getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: return

                    val lensFacing = getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)

                    //try to find a camera with a flash
                    if (flashAvailable && lensFacing == lens) {
                        setTorchMode(cameraId, enabled)
                    }

                } catch (e: CameraAccessException) {
                    when (e.reason) {
                        CameraAccessException.CAMERA_IN_USE -> ctx.toast(R.string.error_camera_in_use)
                        CameraAccessException.CAMERA_DISCONNECTED -> ctx.toast(R.string.error_camera_disconnected)
                        CameraAccessException.CAMERA_DISABLED -> ctx.toast(R.string.error_camera_disabled)
                        CameraAccessException.CAMERA_ERROR -> ctx.toast(R.string.error_camera_error)
                        CameraAccessException.MAX_CAMERAS_IN_USE -> ctx.toast(R.string.error_max_cameras_in_use)
                        else -> ctx.toast(R.string.error_camera_access_exception)
                    }
                }
            }
        }
    }
}