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

/**
 * Created by sds100 on 06/07/2021.
 */

class CreateConstraintUseCaseImpl(
    private val networkAdapter: NetworkAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val preferenceRepository: PreferenceRepository,
) : CreateConstraintUseCase {

    override fun isSupported(constraint: ConstraintId): Error? {
        when (constraint) {
            ConstraintId.FLASHLIGHT_ON, ConstraintId.FLASHLIGHT_OFF ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
                }

            ConstraintId.DEVICE_IS_LOCKED, ConstraintId.DEVICE_IS_UNLOCKED ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.LOLLIPOP_MR1)
                }

            else -> Unit
        }

        return null
    }

    override fun getKnownWiFiSSIDs(): List<String>? = networkAdapter.getKnownWifiSSIDs()

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

    override fun getSavedWifiSSIDs(): Flow<List<String>> = preferenceRepository.get(Keys.savedWifiSSIDs)
        .map { it?.toList() ?: emptyList() }
}

interface CreateConstraintUseCase {
    fun isSupported(constraint: ConstraintId): Error?
    fun getKnownWiFiSSIDs(): List<String>?
    fun getEnabledInputMethods(): List<ImeInfo>

    suspend fun saveWifiSSID(ssid: String)
    fun getSavedWifiSSIDs(): Flow<List<String>>
}
