package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.mapData
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
    private val backupManager: BackupManager,
    displayKeyMapUseCase: DisplayKeyMapUseCase,
    private val sortKeyMapsUseCase: SortKeyMapsUseCase,
) : ListKeyMapsUseCase,
    DisplayKeyMapUseCase by displayKeyMapUseCase {

    override val keyMapList: Flow<State<List<KeyMap>>> = channelFlow {
        send(State.Loading)

        combine(
            keyMapRepository.keyMapList,
            sortKeyMapsUseCase.observeKeyMapsSorter(),
        ) { keyMapEntitiesState, sorter ->
            keyMapEntitiesState.mapData { keyMapEntities ->
                keyMapEntities
                    .map { KeyMapEntityMapper.fromEntity(it) }
                    .sortedWith(sorter)
            }
        }.collectLatest {
            withContext(Dispatchers.Default) {
                send(it)
            }
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

    override suspend fun backupKeyMaps(vararg uid: String, uri: String): Result<String> =
        backupManager.backupKeyMaps(uri, uid.asList())
}

interface ListKeyMapsUseCase : DisplayKeyMapUseCase {
    val keyMapList: Flow<State<List<KeyMap>>>
    fun deleteKeyMap(vararg uid: String)
    fun enableKeyMap(vararg uid: String)
    fun disableKeyMap(vararg uid: String)
    fun duplicateKeyMap(vararg uid: String)
    suspend fun backupKeyMaps(vararg uid: String, uri: String): Result<String>
}
