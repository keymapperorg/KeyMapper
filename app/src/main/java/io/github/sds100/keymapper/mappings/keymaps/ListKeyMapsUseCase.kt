package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.then
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 16/04/2021.
 */
class ListKeyMapsUseCaseImpl(
    private val keyMapRepository: KeyMapRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
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
            send(State.Loading)

            withContext(Dispatchers.Default) {
                if (keyMapListState is State.Loading || buttonListState is State.Loading) {
                    send(State.Loading)
                }

                val keyMapList = keyMapListState.dataOrNull() ?: return@withContext
                val buttonList = buttonListState.dataOrNull() ?: return@withContext

                val keyMaps = keyMapList.map { keyMap ->
                    KeyMapEntityMapper.fromEntity(keyMap, buttonList)
                }

                send(State.Data(keyMaps))
            }
        }
    }

    override val areAllEnabled: Flow<Boolean> = keyMapList.map { state ->
        state.dataOrNull()?.all { it.isEnabled } == true
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

    override suspend fun backupKeyMaps(vararg uid: String, uri: String): Result<String> = backupManager.backupKeyMaps(uid.asList()).then { Success(it.uri) }
}

interface ListKeyMapsUseCase : DisplayKeyMapUseCase {
    val keyMapList: Flow<State<List<KeyMap>>>
    val areAllEnabled: Flow<Boolean>

    fun deleteKeyMap(vararg uid: String)
    fun enableKeyMap(vararg uid: String)
    fun disableKeyMap(vararg uid: String)
    fun duplicateKeyMap(vararg uid: String)
    suspend fun backupKeyMaps(vararg uid: String, uri: String): Result<String>
}
