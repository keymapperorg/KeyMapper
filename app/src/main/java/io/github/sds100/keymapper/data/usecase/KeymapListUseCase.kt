package io.github.sds100.keymapper.data.usecase

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 06/11/20.
 */

interface KeymapListUseCase {
    val keymapList: LiveData<List<KeyMap>>
    suspend fun duplicateKeymap(vararg id: Long)

    suspend fun enableKeymapById(vararg id: Long)
    suspend fun disableKeymapById(vararg id: Long)
    suspend fun deleteKeymap(vararg id: Long)
    suspend fun enableAll()
    suspend fun disableAll()
}