package io.github.sds100.keymapper.data.usecase

/**
 * Created by sds100 on 06/11/20.
 */

interface MenuKeymapUseCase {
    suspend fun enableAll()
    suspend fun disableAll()
}