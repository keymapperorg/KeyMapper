package io.github.sds100.keymapper.constraints

import android.os.Build
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.util.Error

/**
 * Created by sds100 on 06/07/2021.
 */

class CreateConstraintUseCaseImpl(
    private val networkAdapter: NetworkAdapter,
    private val inputMethodAdapter: InputMethodAdapter
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
        }

        return null
    }

    override fun getKnownWiFiSSIDs(): List<String>? {
        return networkAdapter.getKnownWifiSSIDs()
    }

    override fun getEnabledInputMethods(): List<ImeInfo> {
        return inputMethodAdapter.inputMethods.value
    }
}

interface CreateConstraintUseCase {
    fun isSupported(constraint: ChooseConstraintType): Error?
    fun getKnownWiFiSSIDs(): List<String>?
    fun getEnabledInputMethods(): List<ImeInfo>
}