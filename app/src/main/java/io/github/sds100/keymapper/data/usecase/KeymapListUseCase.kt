package io.github.sds100.keymapper.data.usecase

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 06/11/20.
 */

interface KeymapListUseCase {
    val keymapList: LiveData<List<KeyMap>>
    fun duplicateKeymap(vararg id: Long)

    fun enableKeymapById(vararg id: Long)
    fun disableKeymapById(vararg id: Long)
    fun deleteKeymap(vararg id: Long)
    fun enableAll()
    fun disableAll()
}