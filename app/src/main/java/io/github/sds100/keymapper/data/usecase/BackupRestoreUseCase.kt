package io.github.sds100.keymapper.data.usecase

import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 06/11/20.
 */
interface BackupRestoreUseCase {
    suspend fun getKeymaps(): List<KeyMap>
    suspend fun insertKeymap(vararg keymap: KeyMap)
}