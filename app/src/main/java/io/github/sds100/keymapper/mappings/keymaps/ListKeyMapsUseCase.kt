package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.backup.BackupManagerImpl
import io.github.sds100.keymapper.backup.BackupUtils
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 16/04/2021.
 */
class ListKeyMapsUseCaseImpl(
    private val keyMapRepository: KeyMapRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val fileAdapter: FileAdapter,
    private val backupManager: BackupManager,
    displayKeyMapUseCase: DisplayKeyMapUseCase,
) : ListKeyMapsUseCase,
    DisplayKeyMapUseCase by displayKeyMapUseCase {

    override val keyMapList: Flow<State<List<KeyMap>>> = channelFlow {
        send(State.Loading)

        combine(
            keyMapRepository.keyMapList,
            floatingButtonRepository.buttonsList,
        ) { keyMapListState, buttonListState ->
            Pair(keyMapListState, buttonListState)
        }.collectLatest { (keyMapListState, buttonListState) ->
            if (keyMapListState is State.Loading || buttonListState is State.Loading) {
                send(State.Loading)
            }

            val keyMapList = keyMapListState.dataOrNull() ?: return@collectLatest
            val buttonList = buttonListState.dataOrNull() ?: return@collectLatest

            val keyMaps = withContext(Dispatchers.Default) {
                keyMapList.map { keyMap ->
                    KeyMapEntityMapper.fromEntity(keyMap, buttonList)
                }
            }

            send(State.Data(keyMaps))
        }
    }

    override fun deleteKeyMap(vararg uid: String) {
        keyMapRepository.delete(*uid)
    }

    override fun enableKeyMap(vararg uid: String) {
        keyMapRepository.enableById(*uid)
    }

    override fun disableKeyMap(vararg uid: String) {
        keyMapRepository.disableById(*uid)
    }

    override fun duplicateKeyMap(vararg uid: String) {
        keyMapRepository.duplicate(*uid)
    }

    override suspend fun backupKeyMaps(vararg uid: String): Result<String> {
        val fileName = BackupUtils.createBackupFileName()

        // Share in private files so the share sheet can show the file name. This is some quirk
        // of the storage access framework https://issuetracker.google.com/issues/268079113.
        // Saving it directly to Downloads with the MediaStore returns a content URI
        // that only contains a numerical ID, not the file name.
        return fileAdapter.getPrivateFile("${BackupManagerImpl.BACKUP_DIR}/$fileName").let { file ->
            file.createFile()
            backupManager.backupKeyMaps(file, uid.asList())
            Success(fileAdapter.getPublicUriForPrivateFile(file))
        }
    }
}

interface ListKeyMapsUseCase : DisplayKeyMapUseCase {
    val keyMapList: Flow<State<List<KeyMap>>>

    fun deleteKeyMap(vararg uid: String)
    fun enableKeyMap(vararg uid: String)
    fun disableKeyMap(vararg uid: String)
    fun duplicateKeyMap(vararg uid: String)
    suspend fun backupKeyMaps(vararg uid: String): Result<String>
}
