package io.github.sds100.keymapper.base.repositories

import io.github.sds100.keymapper.base.TestDispatcherProvider
import io.github.sds100.keymapper.base.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.repositories.RoomKeyMapRepository
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class KeyMapRepositoryTest {

    companion object {
        private val FAKE_KEYBOARD = InputDeviceInfo(
            descriptor = "fake_keyboard_descriptor",
            name = "fake keyboard",
            id = 1,
            isExternal = true,
            isGameController = false,
        )
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: RoomKeyMapRepository
    private lateinit var devicesAdapter: FakeDevicesAdapter
    private lateinit var mockDao: KeyMapDao

    private lateinit var keyMaps: MutableSharedFlow<List<KeyMapEntity>>

    @Before
    fun init() {
        keyMaps = MutableSharedFlow(replay = 1)
        devicesAdapter = FakeDevicesAdapter()

        mockDao = mock {
            on { getAll() }.then { keyMaps }
        }

        repository = RoomKeyMapRepository(
            keyMapDao = mockDao,
            fingerprintMapDao = mock {
                on { getAll() }.then { MutableStateFlow(emptyList<FingerprintMapEntity>()) }
            },
            testScope,
            dispatchers = TestDispatcherProvider(testDispatcher),
        )
    }

    /**
     * issue #641
     */
    @Test
    fun `if modifying a huge number of key maps then split job into batches`() = runTest(testDispatcher) {
        // GIVEN
        val keyMapList = sequence {
            repeat(991) {
                yield(KeyMapEntity(id = it.toLong()))
            }
        }.toList()

        keyMaps.emit(keyMapList)

        inOrder(mockDao) {
            // WHEN, THEN
            // split job up into batches of 200 key maps
            repository.enableById(*keyMapList.map { it.uid }.toTypedArray())
            verify(mockDao, times(5)).enableKeyMapByUid(anyVararg())

            repository.disableById(*keyMapList.map { it.uid }.toTypedArray())
            verify(mockDao, times(5)).disableKeyMapByUid(anyVararg())

            repository.delete(*keyMapList.map { it.uid }.toTypedArray())
            verify(mockDao, times(5)).deleteById(anyVararg())

            repository.duplicate(*keyMapList.map { it.uid }.toTypedArray())
            verify(mockDao, times(5)).insert(anyVararg())

            repository.insert(*keyMapList.toTypedArray())
            verify(mockDao, times(5)).insert(anyVararg())

            repository.update(*keyMapList.toTypedArray())
            verify(mockDao, times(5)).update(anyVararg())
        }
    }
}
