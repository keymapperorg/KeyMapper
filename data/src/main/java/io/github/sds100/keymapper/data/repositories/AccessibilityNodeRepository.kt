package io.github.sds100.keymapper.data.repositories

import android.database.sqlite.SQLiteConstraintException
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface AccessibilityNodeRepository {
    val nodes: Flow<State<List<AccessibilityNodeEntity>>>
    suspend fun get(id: Long): AccessibilityNodeEntity?
    fun insert(vararg node: AccessibilityNodeEntity)
    suspend fun deleteAll()
}

class AccessibilityNodeRepositoryImpl(
    private val coroutineScope: CoroutineScope,
    private val dao: AccessibilityNodeDao,
) : AccessibilityNodeRepository {

    override val nodes: StateFlow<State<List<AccessibilityNodeEntity>>> =
        dao.getAll()
            .map { list ->
                // Distinct by all fields except the ID.
                State.Data(list.distinctBy { it.copy(id = 0) })
            }
            .flowOn(Dispatchers.IO)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(10000), State.Loading)

    override fun insert(vararg node: AccessibilityNodeEntity) {
        coroutineScope.launch(Dispatchers.IO) {
            for (n in node) {
                try {
                    dao.insert(n)
                } catch (e: SQLiteConstraintException) {
                    // Do nothing if the node already exists.
                }
            }
        }
    }

    override suspend fun get(id: Long): AccessibilityNodeEntity? {
        return dao.getById(id)
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            dao.deleteAll()
        }
    }
}
