package io.github.sds100.keymapper.base.keymaps

import android.database.sqlite.SQLiteConstraintException
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.data.entities.FloatingButtonEntityWithLayout
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigKeyMapStateImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val keyMapRepository: KeyMapRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
) : ConfigKeyMapState {
    private var originalKeyMap: KeyMap? = null

    private val _keyMap: MutableStateFlow<State<KeyMap>> = MutableStateFlow(State.Loading)
    override val keyMap: StateFlow<State<KeyMap>> = _keyMap.asStateFlow()

    override val floatingButtonToUse: MutableStateFlow<String?> = MutableStateFlow(null)

    init {
        // Update button data in the key map whenever the floating buttons changes.
        coroutineScope.launch {
            floatingButtonRepository.buttonsList
                .filterIsInstance<State.Data<List<FloatingButtonEntityWithLayout>>>()
                .map { it.data }
                .collectLatest(::updateFloatingButtonTriggerKeys)
        }
    }

    private fun updateFloatingButtonTriggerKeys(buttons: List<FloatingButtonEntityWithLayout>) {
        update { keyMap ->
            keyMap.copy(trigger = keyMap.trigger.updateFloatingButtonData(buttons))
        }
    }

    /**
     * Whether any changes were made to the key map.
     */
    override val isEdited: Boolean
        get() = when (val keyMap = keyMap.value) {
            is State.Data<KeyMap> -> originalKeyMap?.let { it != keyMap.data } ?: false
            State.Loading -> false
        }

    override suspend fun loadKeyMap(uid: String) {
        _keyMap.update { State.Loading }
        val entity = keyMapRepository.get(uid) ?: return
        val floatingButtons = floatingButtonRepository.buttonsList
            .filterIsInstance<State.Data<List<FloatingButtonEntityWithLayout>>>()
            .map { it.data }
            .first()

        val keyMap = KeyMapEntityMapper.fromEntity(entity, floatingButtons)
        _keyMap.update { State.Data(keyMap) }
        originalKeyMap = keyMap
    }

    override fun loadNewKeyMap(groupUid: String?) {
        val keyMap = KeyMap(groupUid = groupUid)
        _keyMap.update { State.Data(keyMap) }
        originalKeyMap = keyMap
    }

    // Useful for testing
    fun setKeyMap(keyMap: KeyMap) {
        _keyMap.update { State.Data(keyMap) }
        originalKeyMap = keyMap
    }

    override fun save() {
        val keyMap = keyMap.value.dataOrNull() ?: return

        if (keyMap.dbId == null) {
            val entity = KeyMapEntityMapper.toEntity(keyMap, 0)
            try {
                keyMapRepository.insert(entity)
            } catch (e: SQLiteConstraintException) {
                keyMapRepository.update(entity)
            }
        } else {
            keyMapRepository.update(KeyMapEntityMapper.toEntity(keyMap, keyMap.dbId))
        }
    }

    override fun restoreState(keyMap: KeyMap) {
        _keyMap.update { State.Data(keyMap) }
    }

    override fun update(block: (keyMap: KeyMap) -> KeyMap) {
        _keyMap.update { value -> value.mapData { block.invoke(it) } }
    }
}

interface ConfigKeyMapState {
    val keyMap: StateFlow<State<KeyMap>>
    val isEdited: Boolean

    fun update(block: (keyMap: KeyMap) -> KeyMap)
    fun save()

    fun restoreState(keyMap: KeyMap)
    suspend fun loadKeyMap(uid: String)
    fun loadNewKeyMap(groupUid: String?)

    val floatingButtonToUse: MutableStateFlow<String?>
}
