package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraFlashInfo
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge

/**
 * Created by sds100 on 25/07/2021.
 */

class CreateActionUseCaseImpl(
    private val inputMethodAdapter: InputMethodAdapter,
    private val systemFeatureAdapter: SystemFeatureAdapter,
    private val cameraAdapter: CameraAdapter,
) : CreateActionUseCase,
    IsActionSupportedUseCase by IsActionSupportedUseCaseImpl(systemFeatureAdapter, cameraAdapter) {
    override suspend fun getInputMethods(): List<ImeInfo> = inputMethodAdapter.inputMethods.first()

    override fun getFlashlightLenses(): Set<CameraLens> {
        return CameraLens.entries.filter { cameraAdapter.getFlashInfo(it) != null }.toSet()
    }

    override fun getFlashInfo(lens: CameraLens): CameraFlashInfo? {
        return cameraAdapter.getFlashInfo(lens)
    }

    override fun toggleFlashlight(lens: CameraLens, strength: Float) {
        cameraAdapter.toggleFlashlight(lens, strength)
    }

    override fun disableFlashlight() {
        cameraAdapter.disableFlashlight(CameraLens.FRONT)
        cameraAdapter.disableFlashlight(CameraLens.BACK)
    }

    override fun setFlashlightBrightness(lens: CameraLens, strength: Float) {
        cameraAdapter.enableFlashlight(lens, strength)
    }

    override fun isFlashlightEnabled(): Flow<Boolean> {
        return merge(
            cameraAdapter.isFlashlightOnFlow(CameraLens.FRONT),
            cameraAdapter.isFlashlightOnFlow(CameraLens.BACK),
        )
    }
}

interface CreateActionUseCase : IsActionSupportedUseCase {
    suspend fun getInputMethods(): List<ImeInfo>

    fun isFlashlightEnabled(): Flow<Boolean>
    fun setFlashlightBrightness(lens: CameraLens, strength: Float)
    fun toggleFlashlight(lens: CameraLens, strength: Float)
    fun disableFlashlight()
    fun getFlashlightLenses(): Set<CameraLens>
    fun getFlashInfo(lens: CameraLens): CameraFlashInfo?
}
