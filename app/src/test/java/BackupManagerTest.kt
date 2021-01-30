import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.salomonbrys.kotson.byObject
import com.github.salomonbrys.kotson.contains
import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.BackupManager
import io.github.sds100.keymapper.data.IBackupManager
import io.github.sds100.keymapper.data.IGlobalPreferences
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.data.usecase.BackupRestoreUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.BackupVersionTooNew
import io.github.sds100.keymapper.util.result.CorruptJsonFile
import io.github.sds100.keymapper.util.result.EmptyJson
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import util.LiveDataTestWrapper
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
    private val dispatcherProvider = object : DispatcherProvider {
        override fun main() = testDispatcher
        override fun default() = testDispatcher
        override fun io() = testDispatcher
        override fun unconfined() = testDispatcher
    }

    private lateinit var backupManager: IBackupManager
    private lateinit var mockContentResolver: IContentResolver
    private lateinit var mockGlobalPreferences: IGlobalPreferences
    private lateinit var mockKeymapRepository: BackupRestoreUseCase
    private lateinit var mockFingerprintMapRepository: FingerprintMapRepository
    private lateinit var eventStream: LiveDataTestWrapper<Event>

    private lateinit var parser: JsonParser
    private lateinit var gson: Gson

    private val GESTURE_ID_TO_JSON_KEY_MAP = mapOf(
        "swipe_down" to "fingerprint_swipe_down",
        "swipe_up" to "fingerprint_swipe_up",
        "swipe_left" to "fingerprint_swipe_left",
        "swipe_right" to "fingerprint_swipe_right",
    )

    private val GESTURE_IDS = arrayOf("swipe_down", "swipe_up", "swipe_left", "swipe_right")

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
            throwExceptions = true,
            dispatcherProvider
        )

        eventStream = LiveDataTestWrapper(backupManager.eventStream)

        parser = JsonParser()
        gson = Gson()

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `restore keymaps with no db version, assume version is 9 and don't show error message`() = coroutineScope.runBlockingTest {
        val fileName = "restore-keymaps-no-db-version.json"
        backupManager.restore(getJson(fileName))
        advanceUntilIdle()

        assertThat(eventStream.history, `is`(listOf(RestoreResult(Success(Unit)))))

        verify(mockKeymapRepository).restore(9, getKeymapJsonListFromFile(fileName))
    }

    @Test
    fun `backup all fingerprint maps, don't save keymap db version and show success message`() = coroutineScope.runBlockingTest {
        `when`(mockFingerprintMapRepository.swipeDown).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeUp).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeLeft).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeRight).then { flow { emit(FingerprintMap()) } }

        val outputStream = PipedOutputStream()
        val inputStream = PipedInputStream(outputStream) // must become before async call in BackupManager.backup

        backupManager.backupFingerprintMaps(outputStream)

        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = parser.parse(json).asJsonObject

        assertThat("keymap db version is in json", !jsonObject.contains(BackupManager.NAME_KEYMAP_DB_VERSION))

        assertThat(eventStream.history, `is`(listOf(BackupResult(Success(Unit)))))
    }

    @Test
    fun `restore a single fingerprint map, only restore a single fingerprint map and a success message`() = coroutineScope.runBlockingTest {
        backupManager.restore(getJson("restore-single-fingerprint-map.json"))
        advanceUntilIdle()

        assertThat(eventStream.history, `is`(listOf(RestoreResult(Success(Unit)))))

        verify(mockFingerprintMapRepository, times(1)).restore(anyString(), anyString())
    }

    @Test
    fun `restore all fingerprint maps, all fingerprint maps should be restored and a success message`() = coroutineScope.runBlockingTest {

        val fileName = "restore-all-fingerprint-maps.json"
        backupManager.restore(getJson(fileName))

        advanceUntilIdle()

        assertThat(eventStream.history, `is`(listOf(RestoreResult(Success(Unit)))))

        getFingerprintMapsToGestureIdFromFile(fileName).forEach { (gestureId, json) ->
            verify(mockFingerprintMapRepository).restore(gestureId, json)
        }
    }

    @Test
    fun `restore many key maps and device info, all key maps and device info should be restored and a success message`() = coroutineScope.runBlockingTest {
        val fileName = "restore-many-keymaps.json"

        backupManager.restore(getJson(fileName))
        advanceUntilIdle()

        assertThat(eventStream.history, `is`(listOf(RestoreResult(Success(Unit)))))

        verify(mockKeymapRepository).restore(10, getKeymapJsonListFromFile(fileName))
    }

    @Test
    fun `restore with key map db version greater than allowed version, send incompatible backup event`() = coroutineScope.runBlockingTest {

        backupManager.restore(getJson("restore-keymap-db-version-too-big.json"))

        advanceUntilIdle()

        assertThat(eventStream.history, `is`(listOf(RestoreResult(BackupVersionTooNew))))

        verify(mockKeymapRepository, times(0)).restore(anyInt(), anyList())
    }

    @Test
    fun `restore with fingerprint gesture map db version greater than allowed version, send incompatible backup event`() = coroutineScope.runBlockingTest {

        backupManager.restore(getJson("restore-fingerprint-map-version-too-big.json"))

        advanceUntilIdle()

        assertThat(eventStream.history, `is`(listOf(RestoreResult(BackupVersionTooNew))))

        verify(mockFingerprintMapRepository, times(0)).restore(anyString(), anyString())
    }

    @Test
    fun `restore empty file, show empty json error message`() = coroutineScope.runBlockingTest {
        val emptyFileInputStream = getJson("empty.json")

        backupManager.restore(emptyFileInputStream)
        advanceUntilIdle()

        assertThat(eventStream.history, `is`(listOf(RestoreResult(EmptyJson))))
    }

    @Test
    fun `restore corrupt file, show corrupt json message`() = coroutineScope.runBlockingTest {
        val emptyFileInputStream = getJson("corrupt.json")

        backupManager.restore(emptyFileInputStream)
        advanceUntilIdle()

        assertThat(eventStream.history, `is`(listOf(RestoreResult(CorruptJsonFile))))
    }

    @Test
    fun `backup all fingerprint maps, return list of default fingerprint maps`() = coroutineScope.runBlockingTest {
        `when`(mockFingerprintMapRepository.swipeDown).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeUp).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeLeft).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeRight).then { flow { emit(FingerprintMap()) } }

        val outputStream = PipedOutputStream()
        val inputStream = PipedInputStream(outputStream) // must become before async call in BackupManager.backup

        backupManager.backupFingerprintMaps(outputStream)

        val json = inputStream.bufferedReader().use { it.readText() }
        val rootElement = parser.parse(json)

        GESTURE_ID_TO_JSON_KEY_MAP.values.forEach { jsonKey ->
            assertThat("doesn't contain $jsonKey fingerprint map", gson.toJson(rootElement[jsonKey]), `is`(gson.toJson(FingerprintMap())))
        }

        assertThat(eventStream.history, `is`(listOf(BackupResult(Success(Unit)))))
    }

    @Test
    fun `backup key maps, return list of default key maps, keymap db version should be current database version`() = coroutineScope.runBlockingTest {
        val keymapList = listOf(KeyMap(0), KeyMap(1))
        `when`(mockKeymapRepository.getKeymaps()).then { keymapList }

        val outputStream = PipedOutputStream()
        val inputStream = PipedInputStream(outputStream) // must become before async call in BackupManager.backup

        backupManager.backupKeymaps(outputStream, listOf(0L, 1L))

        advanceUntilIdle()

        val rootJsonElement = inputStream.bufferedReader().use { it.readText() }
        val rootElement = parser.parse(rootJsonElement)
        val keymapListJsonArray = rootElement["keymap_list"].asJsonArray

        keymapList.forEachIndexed { index, keymap ->
            val expectedKeymapJson = gson.toJson(keymap)
            val actualKeymapJson = gson.toJson(keymapListJsonArray[index])

            assertThat(actualKeymapJson, `is`(expectedKeymapJson))
        }

        assertThat(rootElement["keymap_db_version"].asInt, `is`(AppDatabase.DATABASE_VERSION))

        assertThat(eventStream.history, `is`(listOf(BackupResult(Success(Unit)))))
    }

    @Test
    fun `backup everything, return list of default keymaps and default fingerprint maps, keymap db version should be current database version`() = coroutineScope.runBlockingTest {
        val keymapList = listOf(KeyMap(0), KeyMap(1))
        `when`(mockKeymapRepository.getKeymaps()).then { keymapList }
        `when`(mockFingerprintMapRepository.swipeDown).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeUp).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeLeft).then { flow { emit(FingerprintMap()) } }
        `when`(mockFingerprintMapRepository.swipeRight).then { flow { emit(FingerprintMap()) } }

        val outputStream = PipedOutputStream()
        val inputStream = PipedInputStream(outputStream) // must become before async call in BackupManager.backup

        backupManager.backupEverything(outputStream)

        advanceUntilIdle()

        val rootJsonElement = inputStream.bufferedReader().use { it.readText() }
        val rootElement = parser.parse(rootJsonElement)
        val keymapListJsonArray = rootElement["keymap_list"].asJsonArray

        keymapList.forEachIndexed { index, keymap ->
            val expectedKeymapJson = gson.toJson(keymap)
            val actualKeymapJson = gson.toJson(keymapListJsonArray[index])

            assertThat(actualKeymapJson, `is`(expectedKeymapJson))
        }

        GESTURE_ID_TO_JSON_KEY_MAP.values.forEach { jsonKey ->
            assertThat("doesn't contain $jsonKey fingerprint map", gson.toJson(rootElement[jsonKey]), `is`(gson.toJson(FingerprintMap())))
        }

        assertThat(rootElement["keymap_db_version"].asInt, `is`(AppDatabase.DATABASE_VERSION))

        assertThat(eventStream.history, `is`(listOf(BackupResult(Success(Unit)))))
    }

    private fun getFingerprintMapsToGestureIdFromFile(fileName: String): Map<String, String> {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return GESTURE_ID_TO_JSON_KEY_MAP.map { (gestureId, jsonKey) ->
            val map by rootElement.byObject(jsonKey)
            gestureId to gson.toJson(map)
        }.toMap()
    }

    private fun getKeymapJsonListFromFile(fileName: String): List<String> {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return rootElement["keymap_list"].asJsonArray.map { gson.toJson(it) }
    }

    private fun getJson(fileName: String): InputStream {
        return this.javaClass.classLoader!!.getResourceAsStream("backup-manager-test/$fileName")
    }
}