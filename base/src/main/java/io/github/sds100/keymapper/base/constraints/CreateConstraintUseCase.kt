package io.github.sds100.keymapper.base.constraints

import android.view.Display
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CreateConstraintUseCaseImpl @Inject constructor(
    private val networkAdapter: NetworkAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val cameraAdapter: CameraAdapter,
    private val displayAdapter: DisplayAdapter,
    systemFeatureAdapter: SystemFeatureAdapter,
) : CreateConstraintUseCase,
    IsConstraintSupportedUseCase by IsConstraintSupportedUseCaseImpl(
        systemFeatureAdapter,
        cameraAdapter,
    ) {

    override fun getKnownWiFiSSIDs(): List<String> = networkAdapter.getKnownWifiSSIDs()

    override fun getEnabledInputMethods(): List<ImeInfo> = inputMethodAdapter.inputMethods.value

    override suspend fun saveWifiSSID(ssid: String) {
        val savedWifiSSIDsList = getSavedWifiSSIDs().first().toMutableList()

        if (!savedWifiSSIDsList.contains(ssid)) {
            if (savedWifiSSIDsList.size == 3) {
                savedWifiSSIDsList.removeAt(savedWifiSSIDsList.lastIndex)
            }

            if (savedWifiSSIDsList.isEmpty()) {
                savedWifiSSIDsList.add(ssid)
            } else {
                savedWifiSSIDsList.add(0, ssid)
            }
        }

        preferenceRepository.set(
            Keys.savedWifiSSIDs,
            savedWifiSSIDsList.toSet(),
        )
    }

    override fun getSavedWifiSSIDs(): Flow<List<String>> =
        preferenceRepository.get(Keys.savedWifiSSIDs)
            .map { it?.toList() ?: emptyList() }

    override fun getFlashlightLenses(): Set<CameraLens> {
        return CameraLens.entries.filter { cameraAdapter.getFlashInfo(it) != null }.toSet()
    }

    override fun getSupportedResolutions(): Set<SizeKM> =
        displayAdapter.displays.value.flatMap { it.supportedSizes }.toSet()

    override fun getCurrentResolution(): SizeKM {
        return displayAdapter.getDisplay(
            displayAdapter.activityDisplayId ?: Display.DEFAULT_DISPLAY,
        )!!.physicalSize
    }
}

interface CreateConstraintUseCase : IsConstraintSupportedUseCase {
    fun getKnownWiFiSSIDs(): List<String>
    fun getEnabledInputMethods(): List<ImeInfo>

    suspend fun saveWifiSSID(ssid: String)
    fun getSavedWifiSSIDs(): Flow<List<String>>

    fun getFlashlightLenses(): Set<CameraLens>

    fun getSupportedResolutions(): Set<SizeKM>
    fun getCurrentResolution(): SizeKM
}
