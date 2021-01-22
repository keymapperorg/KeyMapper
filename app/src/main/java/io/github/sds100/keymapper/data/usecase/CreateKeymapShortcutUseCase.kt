package io.github.sds100.keymapper.data.usecase

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 06/11/20.
 */

interface CreateKeymapShortcutUseCase {
    val keymapList: LiveData<List<KeyMap>>
    suspend fun updateKeymap(keymap: KeyMap)
}