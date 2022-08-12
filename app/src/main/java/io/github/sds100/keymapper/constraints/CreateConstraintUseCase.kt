package io.github.sds100.keymapper.constraints

import android.os.Build
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.util.Error
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Created by sds100 on 06/07/2021.
 */

class CreateConstraintUseCaseImpl @Inject constructor(
    private val networkAdapter: NetworkAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val preferenceRepository: PreferenceRepository
) : CreateConstraintUseCase {

    override fun isSupported(constraint: ChooseConstraintType): Error? {
        when (constraint) {
            ChooseConstraintType.FLASHLIGHT_ON, ChooseConstraintType.FLASHLIGHT_OFF ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
                }

            ChooseConstraintType.DEVICE_IS_LOCKED, ChooseConstraintType.DEVICE_IS_UNLOCKED ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.LOLLIPOP_MR1)
                }

            else -> return null
        }

        return null
    }

    override fun getKnownWiFiSSIDs(): List<String>? {
        return networkAdapter.getKnownWifiSSIDs()
    }

    override fun getEnabledInputMethods(): List<ImeInfo> {
        return inputMethodAdapter.inputMethods.value
    }

    override suspend fun saveWifiSSID(ssid: String) {
        val savedWifiSSIDsList = getSavedWifiSSIDs().first().toMutableList()

        if (!savedWifiSSIDsList.contains(ssid)) {

            if (savedWifiSSIDsList.size == 3) {
                savedWifiSSIDsList.removeLast()
            }

            if (savedWifiSSIDsList.isEmpty()) {
                savedWifiSSIDsList.add(ssid)
            } else {
                savedWifiSSIDsList.add(0, ssid)
            }
        }

        preferenceRepository.set(
            Keys.savedWifiSSIDs,
            savedWifiSSIDsList.toSet()
        )
    }

    override fun getSavedWifiSSIDs(): Flow<List<String>> {
        return preferenceRepository.get(Keys.savedWifiSSIDs)
            .map { it?.toList() ?: emptyList() }
    }
}

interface CreateConstraintUseCase {
    fun isSupported(constraint: ChooseConstraintType): Error?
    fun getKnownWiFiSSIDs(): List<String>?
    fun getEnabledInputMethods(): List<ImeInfo>

    suspend fun saveWifiSSID(ssid: String)
    fun getSavedWifiSSIDs(): Flow<List<String>>
}