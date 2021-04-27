package io.github.sds100.keymapper.actions

import android.os.Build
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.util.Error

/**
 * Created by sds100 on 16/03/2021.
 */

class IsSystemActionSupportedUseCaseImpl(
   private val adapter: SystemFeatureAdapter
) : IsSystemActionSupportedUseCase {

    override fun invoke(id: SystemActionId): Error? {
        val minApi = SystemActionUtils.getMinApi(id)

        if (Build.VERSION.SDK_INT < minApi) {
            return Error.SdkVersionTooLow(minApi)
        }

        val maxApi = SystemActionUtils.getMaxApi(id)
        if (Build.VERSION.SDK_INT > maxApi) {
            return Error.SdkVersionTooHigh(maxApi)
        }

        SystemActionUtils.getRequiredSystemFeatures(id).forEach { feature ->
            if (!adapter.hasSystemFeature(feature)) {
                return Error.FeatureUnavailable(feature)
            }
        }

        return null
    }
}

interface IsSystemActionSupportedUseCase {
    operator fun invoke(id: SystemActionId): Error?
}