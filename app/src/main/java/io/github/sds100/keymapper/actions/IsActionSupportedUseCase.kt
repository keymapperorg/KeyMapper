package io.github.sds100.keymapper.actions

import android.os.Build
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.util.Error

/**
 * Created by sds100 on 16/03/2021.
 */

class IsActionSupportedUseCaseImpl(
    private val adapter: SystemFeatureAdapter,
) : IsActionSupportedUseCase {

    override fun invoke(id: ActionId): Error? {
        if (Build.VERSION.SDK_INT != 0) {
            val minApi = ActionUtils.getMinApi(id)

            if (Build.VERSION.SDK_INT < minApi) {
                return Error.SdkVersionTooLow(minApi)
            }

            val maxApi = ActionUtils.getMaxApi(id)

            if (Build.VERSION.SDK_INT > maxApi) {
                return Error.SdkVersionTooHigh(maxApi)
            }
        }

        ActionUtils.getRequiredSystemFeatures(id).forEach { feature ->
            if (!adapter.hasSystemFeature(feature)) {
                return Error.SystemFeatureNotSupported(feature)
            }
        }

        return null
    }
}

interface IsActionSupportedUseCase {
    operator fun invoke(id: ActionId): Error?
}
