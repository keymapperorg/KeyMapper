package io.github.sds100.keymapper.Delegates

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.github.sds100.keymapper.Interfaces.IContext

/**
 * Created by sds100 on 19/01/2019.
 */

@RequiresApi(Build.VERSION_CODES.M)
class FlashlightController(iContext: IContext) : IContext by iContext, LifecycleObserver {

    private var mIsFlashEnabled = false

    private val mTorchCallback = @RequiresApi(Build.VERSION_CODES.M)
    object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)

            mIsFlashEnabled = enabled
        }
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
    fun toggleFlashlight() {
        mIsFlashEnabled = !mIsFlashEnabled

        setFlashlightMode(mIsFlashEnabled)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setFlashlightMode(enabled: Boolean) {
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