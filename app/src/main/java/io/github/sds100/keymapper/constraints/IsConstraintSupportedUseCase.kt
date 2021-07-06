package io.github.sds100.keymapper.constraints

import android.os.Build
import io.github.sds100.keymapper.util.Error

/**
 * Created by sds100 on 06/07/2021.
 */

class IsConstraintSupportedUseCaseImpl : IsConstraintSupportedUseCase {

    override operator fun invoke(constraint: ChooseConstraintType): Error? {
        when (constraint) {
            ChooseConstraintType.FLASHLIGHT_ON, ChooseConstraintType.FLASHLIGHT_OFF ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
                }
        }

        return null
    }
}

interface IsConstraintSupportedUseCase {
    operator fun invoke(constraint: ChooseConstraintType): Error?
}