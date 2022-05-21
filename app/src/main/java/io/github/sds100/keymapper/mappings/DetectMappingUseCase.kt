package io.github.sds100.keymapper.mappings

/**
 * Created by sds100 on 21/05/2022.
 */
interface DetectMappingUseCase {
    fun showTriggeredToast()
    fun vibrate(duration: Long)
}