package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.common.utils.DefaultDispatcherProvider
import io.github.sds100.keymapper.common.utils.DispatcherProvider
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import io.github.sds100.keymapper.data.entities.FloatingButtonEntityWithLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomFloatingButtonRepository
    @Inject
    constructor(
        private val dao: FloatingButtonDao,
        private val coroutineScope: CoroutineScope,
        private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    ) : FloatingButtonRepository {
        override val buttonsList: StateFlow<State<List<FloatingButtonEntityWithLayout>>> =
            dao
                .getAll()
                .map { State.Data(it) }
                .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

        override fun insert(vararg button: FloatingButtonEntity) {
            coroutineScope.launch(dispatchers.default()) {
                dao.insert(*button)
            }
        }

        override fun update(button: FloatingButtonEntity) {
            coroutineScope.launch(dispatchers.default()) {
                dao.update(button)
            }
        }

        override suspend fun get(uid: String): FloatingButtonEntityWithLayout? = dao.getByUidWithLayout(uid)

        override fun delete(vararg uid: String) {
            coroutineScope.launch(dispatchers.default()) {
                dao.deleteByUid(*uid)
            }
        }
    }
