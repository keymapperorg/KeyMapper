package io.github.sds100.keymapper.data.usecase

import androidx.lifecycle.LiveData
import com.google.gson.JsonArray
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.util.RequestBackup

/**
 * Created by sds100 on 06/11/20.
 */
interface BackupRestoreUseCase {
    val requestBackup: LiveData<RequestBackup>
    suspend fun getKeymaps(): List<KeyMap>

    fun restore(dbVersion: Int, keymapListJson: List<String>)
}