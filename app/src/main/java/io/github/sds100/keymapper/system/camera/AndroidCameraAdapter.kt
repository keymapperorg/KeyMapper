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

    override fun getFlashInfo(lens: CameraLens): CameraFlashInfo? {
        if (getCharacteristicForLens(lens, CameraCharacteristics.FLASH_INFO_AVAILABLE) != true) {
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val maxFlashStrength = getCharacteristicForLens(
                lens,
                CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL,
            ) ?: 1

            val defaultFlashStrength = getCharacteristicForLens(
                lens,
                CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL,
            )

            return CameraFlashInfo(
                supportsVariableStrength = maxFlashStrength > 1,
                defaultStrength = defaultFlashStrength ?: 1,
                maxStrength = maxFlashStrength,
            )
        } else {
            return CameraFlashInfo(
                supportsVariableStrength = false,
                defaultStrength = 1,
                maxStrength = 1,
            )
        }
    }

    private fun <T> getCharacteristicForLens(
        lens: CameraLens,
        characteristic: CameraCharacteristics.Key<T>,
    ): T? {
        for (cameraId in cameraManager.cameraIdList) {
            val camera = cameraManager.getCameraCharacteristics(cameraId)
            val lensFacing = camera.get(CameraCharacteristics.LENS_FACING)!!

            val lensToCompareSdkValue = when (lens) {
                CameraLens.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                CameraLens.BACK -> CameraCharacteristics.LENS_FACING_BACK
            }

            if (lensFacing == lensToCompareSdkValue) {
                return camera.get(characteristic)
            }
        }

        return null
    }

    private fun getFlashlightCameraIdForLens(lens: CameraLens): String? {
        for (cameraId in cameraManager.cameraIdList) {
            val camera = cameraManager.getCameraCharacteristics(cameraId)
            val lensFacing = camera.get(CameraCharacteristics.LENS_FACING)!!

            val lensToCompareSdkValue = when (lens) {
                CameraLens.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                CameraLens.BACK -> CameraCharacteristics.LENS_FACING_BACK
            }

            val flashAvailable = cameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!

            if (flashAvailable && lensFacing == lensToCompareSdkValue) {
                return cameraId
            }
        }

        return null
    }

    override fun enableFlashlight(lens: CameraLens, strengthPercent: Float?): Result<*> = setFlashlightMode(true, lens, strengthPercent)

    override fun disableFlashlight(lens: CameraLens): Result<*> = setFlashlightMode(false, lens)

    override fun toggleFlashlight(lens: CameraLens, strengthPercent: Float?): Result<*> = setFlashlightMode(!isFlashEnabledMap.value[lens]!!, lens, strengthPercent)

    override fun isFlashlightOn(lens: CameraLens): Boolean = isFlashEnabledMap.value[lens] ?: false

    override fun isFlashlightOnFlow(lens: CameraLens): Flow<Boolean> {
        return isFlashEnabledMap.map { it[lens] ?: false }
    }

    override fun changeFlashlightStrength(lens: CameraLens, percent: Float): Result<*> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.TIRAMISU)
        }

        try {
            val cameraId = getFlashlightCameraIdForLens(lens)

            if (cameraId == null) {
                return when (lens) {
                    CameraLens.FRONT -> Error.FrontFlashNotFound
                    CameraLens.BACK -> Error.BackFlashNotFound
                }
            }

            val currentStrength = cameraManager.getTorchStrengthLevel(cameraId)

            val maxStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getCharacteristicForLens(
                    lens,
                    CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL,
                )
            } else {
                null
            }

            if (maxStrength != null) {
                val newStrength =
                    (currentStrength + (percent * maxStrength))
                        .toInt()
                        .coerceAtMost(maxStrength)

                // If we want to go below the current strength then turn off the flashlight.
                if (newStrength < 1) {
                    cameraManager.setTorchMode(cameraId, false)
                } else {
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, newStrength)
                }
            }

            return Success(Unit)
        } catch (e: CameraAccessException) {
            return convertCameraException(e)
        }
    }

    private fun setFlashlightMode(
        enabled: Boolean,
        lens: CameraLens,
        strengthPercent: Float? = null,
    ): Result<*> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
        }

        try {
            val cameraId = getFlashlightCameraIdForLens(lens)

            if (cameraId == null) {
                return when (lens) {
                    CameraLens.FRONT -> Error.FrontFlashNotFound
                    CameraLens.BACK -> Error.BackFlashNotFound
                }
            }

            val maxStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getCharacteristicForLens(
                    lens,
                    CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL,
                )
            } else {
                null
            }

            val defaultStrength =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getCharacteristicForLens(
                        lens,
                        CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL,
                    )
                } else {
                    null
                }

            // try to find a camera with a flash
            if (enabled && maxStrength != null && defaultStrength != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val strength = if (strengthPercent == null) {
                    defaultStrength
                } else {
                    (strengthPercent * maxStrength).toInt().coerceAtLeast(1)
                }
                cameraManager.turnOnTorchWithStrengthLevel(cameraId, strength)
            } else {
                cameraManager.setTorchMode(cameraId, enabled)
            }

            return Success(Unit)
        } catch (e: CameraAccessException) {
            return convertCameraException(e)
        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    private fun convertCameraException(e: CameraAccessException) = when (e.reason) {
        CameraAccessException.CAMERA_IN_USE -> Error.CameraInUse
        CameraAccessException.CAMERA_DISCONNECTED -> Error.CameraDisconnected
        CameraAccessException.CAMERA_DISABLED -> Error.CameraDisabled
        CameraAccessException.CAMERA_ERROR -> Error.CameraError
        CameraAccessException.MAX_CAMERAS_IN_USE -> Error.MaxCamerasInUse
        else -> Error.Exception(e)
    }

    private fun updateState(lens: CameraLens, enabled: Boolean) {
        isFlashEnabledMap.update { map ->
            map.toMutableMap().apply { this[lens] = enabled }
        }
    }
}
