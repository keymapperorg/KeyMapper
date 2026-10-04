package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.VariableDao
import io.github.sds100.keymapper.data.entities.VariableEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

@OptIn(FlowPreview::class)
@Singleton
class VariableRepositoryImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val variableDao: VariableDao,
) : VariableRepository {

    companion object {
        /**
         * A variable can change as often as an action repeats so the values are only written to
         * disk once they have settled. The in memory value is always the authoritative one.
         */
        private const val PERSIST_DEBOUNCE_MS = 500L
    }

    private val _values: MutableStateFlow<Map<String, Long>> = MutableStateFlow(readFromDisk())
    override val values: StateFlow<Map<String, Long>> = _values.asStateFlow()

    init {
        _values
            .drop(1)
            .debounce(PERSIST_DEBOUNCE_MS.milliseconds)
            .onEach { values ->
                variableDao.replaceAll(values.map { VariableEntity(it.key, it.value) })
            }
            .launchIn(coroutineScope)
    }

    override fun set(name: String, value: Long) {
        _values.update { it.plus(name to value) }
    }

    override fun delete(name: String) {
        _values.update { it.minus(name) }
    }

    override fun deleteAll() {
        _values.update { emptyMap() }
    }

    /**
     * This blocks so that a key event handled immediately after the process starts reads the
     * persisted values rather than an empty map. It only happens once, when this repository is
     * created.
     */
    private fun readFromDisk(): Map<String, Long> {
        return runBlocking { variableDao.getAll() }.associate { it.name to it.value }
    }
}
