package io.github.sds100.keymapper.system.camera

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import timber.log.Timber
import kotlin.collections.set

/**
 * Created by sds100 on 17/03/2021.
 */
class AndroidCameraAdapter(context: Context) : CameraAdapter {
    private val ctx = context.applicationContext

    private val cameraManager: CameraManager by lazy { ctx.getSystemService()!! }

    private val initialMap = mapOf(
        CameraLens.FRONT to false,
        CameraLens.BACK to false,
    )
    private val isFlashEnabledMap = MutableStateFlow(initialMap)

    private val torchCallback by lazy {
        @RequiresApi(Build.VERSION_CODES.M)
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                super.onTorchModeChanged(cameraId, enabled)

                cameraManager.apply {
                    try {
                        val camera = getCameraCharacteristics(cameraId)
                        val lensFacing = camera.get(CameraCharacteristics.LENS_FACING)!!

                        when (lensFacing) {
                            CameraCharacteristics.LENS_FACING_FRONT ->
                                updateState(CameraLens.FRONT, enabled)

                            CameraCharacteristics.LENS_FACING_BACK ->
                                updateState(CameraLens.BACK, enabled)
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraManager.registerTorchCallback(torchCallback, null)
        }
    }

    override fun hasFlashFacing(lens: CameraLens): Boolean {
        return cameraManager.cameraIdList.any { cameraId ->
            val camera = cameraManager.getCameraCharacteristics(cameraId)
            val hasFlash =
                camera.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: return false

            val lensToCompareSdkValue = when (lens) {
                CameraLens.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                CameraLens.BACK -> CameraCharacteristics.LENS_FACING_BACK
            }

            return hasFlash && camera.get(CameraCharacteristics.LENS_FACING) == lensToCompareSdkValue
        }
    }

    override fun enableFlashlight(lens: CameraLens): Result<*> = setFlashlightMode(true, lens)

    override fun disableFlashlight(lens: CameraLens): Result<*> = setFlashlightMode(false, lens)

    override fun toggleFlashlight(lens: CameraLens): Result<*> = setFlashlightMode(!isFlashEnabledMap.value[lens]!!, lens)

    override fun isFlashlightOn(lens: CameraLens): Boolean = isFlashEnabledMap.value[lens] ?: false

    override fun isFlashlightOnFlow(lens: CameraLens): Flow<Boolean> {
        return isFlashEnabledMap.map { it[lens] ?: false }
    }

    private fun setFlashlightMode(
        enabled: Boolean,
        lens: CameraLens,
    ): Result<*> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
        }

        // get the CameraManager
        cameraManager.apply {
            for (cameraId in cameraIdList) {
                try {
                    val flashAvailable = getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!

                    val lensFacing = getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.LENS_FACING)

                    val lensSdkValue = when (lens) {
                        CameraLens.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                        CameraLens.BACK -> CameraCharacteristics.LENS_FACING_BACK
                    }

                    // try to find a camera with a flash
                    if (flashAvailable && lensFacing == lensSdkValue) {
                        setTorchMode(cameraId, enabled)
                        return Success(Unit)
                    }
                } catch (e: CameraAccessException) {
                    return when (e.reason) {
                        CameraAccessException.CAMERA_IN_USE -> Error.CameraInUse
                        CameraAccessException.CAMERA_DISCONNECTED -> Error.CameraDisconnected
                        CameraAccessException.CAMERA_DISABLED -> Error.CameraDisabled
                        CameraAccessException.CAMERA_ERROR -> Error.CameraError
                        CameraAccessException.MAX_CAMERAS_IN_USE -> Error.MaxCamerasInUse
                        else -> Error.Exception(e)
                    }
                } catch (e: Exception) {
                    return Error.Exception(e)
                }
            }
        }

        return when (lens) {
            CameraLens.FRONT -> Error.FrontFlashNotFound
            CameraLens.BACK -> Error.BackFlashNotFound
        }
    }

    private fun updateState(lens: CameraLens, enabled: Boolean) {
        isFlashEnabledMap.update { map ->
            map.toMutableMap().apply { this[lens] = enabled }
        }
    }
}
