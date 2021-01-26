package io.github.sds100.keymapper

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.salomonbrys.kotson.contains
import com.google.gson.JsonParser
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.BackupManager
import io.github.sds100.keymapper.data.IBackupManager
import io.github.sds100.keymapper.data.IGlobalPreferences
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.data.usecase.BackupRestoreUseCase
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.IContentResolver
import io.github.sds100.keymapper.util.LiveDataTestWrapper
import io.github.sds100.keymapper.util.RestoreResult
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * Created by sds100 on 24/01/21.
 */

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BackupManagerTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)
    private lateinit var backupManager: IBackupManager
    private lateinit var mockContentResolver: IContentResolver
    private lateinit var mockGlobalPreferences: IGlobalPreferences
    private lateinit var mockKeymapRepository: BackupRestoreUseCase
    private lateinit var mockFingerprintMapRepository: FingerprintMapRepository
    private lateinit var eventStream: LiveDataTestWrapper<Event>
    private val parser = JsonParser()

    @Before
    fun init() {
        mockContentResolver = mock(IContentResolver::class.java)
        mockGlobalPreferences = mock(IGlobalPreferences::class.java)
        mockKeymapRepository = mock(BackupRestoreUseCase::class.java)
        mockFingerprintMapRepository = mock(FingerprintMapRepository::class.java)

        `when`(mockKeymapRepository.requestBackup).thenReturn(LiveEvent())

        backupManager = BackupManager(
            mockKeymapRepository,
            mockFingerprintMapRepository,
            mock(DeviceInfoRepository::class.java),
            coroutineScope,
            mockContentResolver,
            mockGlobalPreferences,
            throwExceptions = true
        )

        eventStream = LiveDataTestWrapper(backupManager.eventStream)
    }

    @Test
    fun `restore keymaps with no db version, assume version is 9 and don't show error message`() = coroutineScope.runBlockingTest {
        backupManager.restore(getJson("restore-keymaps-no-db-version.json"))
        advanceUntilIdle()

        verify(mockKeymapRepository).restore(intThat { it == 9 }, anyList())
        assertThat(eventStream.history, `is`(listOf(RestoreResult(Success(Unit)))))
    }

    @Test
    fun `backup all fingerprint maps, don't save keymap db version`() = coroutineScope.runBlockingTest {
        `when`(mockFingerprintMapRepository.swipeDown).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeUp).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeLeft).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeRight).then { flow { emit(FingerprintMap()) } }

        val outputStream = PipedOutputStream()

        backupManager.backupFingerprintMaps(outputStream)

        val inputStream = PipedInputStream(outputStream)
        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = parser.parse(json).asJsonObject

        assertThat("keymap db version is in json", !jsonObject.contains(BackupManager.NAME_KEYMAP_DB_VERSION))
    }

    @Test
    fun `restore a single fingerprint map, only restore a single fingerprint map and no error message`() = coroutineScope.runBlockingTest {
        backupManager.restore(getJson("restore-single-fingerprint-map.json"))
        advanceUntilIdle()

        verify(mockFingerprintMapRepository, times(1)).restore(anyString(), anyString())

        assertThat(eventStream.history, `is`(listOf(RestoreResult(Success(Unit)))))
    }

    private fun getJson(fileName: String): InputStream {
        return this.javaClass.classLoader!!.getResourceAsStream("backup-manager-test/$fileName")
    }

    // restoring all fingerprint maps test that they all have been restored

    // restoring with db version > allowed version send unsuccessful event
    //keymaps
    //fingerprint maps
    //restoring with db version < allowed version send successful event
    //keymaps
    //fingerprint maps

    // restoring with no db version in backup file
    //keymaps
    //fingerprint maps

    // restoring with empty file
    // restoring with corrupt file

    //backing up keymaps
    //backin up fingerprint maps
}