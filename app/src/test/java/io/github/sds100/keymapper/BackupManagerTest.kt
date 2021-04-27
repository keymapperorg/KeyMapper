package io.github.sds100.keymapper

import com.github.salomonbrys.kotson.byObject
import com.github.salomonbrys.kotson.contains
import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntity
import io.github.sds100.keymapper.system.devices.DeviceInfoCache
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.backup.BackupManagerImpl
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntityGroup
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.FlowUtils.toListWithTimeout
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.*

/**
 * Created by sds100 on 19/04/2021.
 */

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BackupManagerTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private val dispatcherProvider = object : DispatcherProvider {
        override fun main() = testDispatcher
        override fun default() = testDispatcher
        override fun io() = testDispatcher
        override fun unconfined() = testDispatcher
    }

    private lateinit var backupManager: BackupManagerImpl
    private lateinit var preferenceRepository: PreferenceRepository
    private lateinit var keyMapRepository: KeyMapRepository
    private lateinit var fingerprintMapRepository: FingerprintMapRepository
    private lateinit var outputStream: PipedOutputStream

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

        keyMapRepository = mock {
            on { requestBackup } doReturn MutableSharedFlow()
        }

        fingerprintMapRepository = mock {
            on { requestBackup } doReturn MutableSharedFlow()
        }

        preferenceRepository = mock {
            on { get<Any>(anyOrNull()) } doReturn flow { }
        }

        outputStream = PipedOutputStream()

        val deviceInfoCache: DeviceInfoCache = mock {
            onBlocking { getAll() } doReturn emptyList()
        }

        val fileAdapter = object : FileAdapter {
            override fun openOutputStream(uriString: String): Result<OutputStream> {
                return Success(outputStream)
            }

            override fun openInputStream(uriString: String): Result<InputStream> {
                return Success(getJson(uriString))
            }

            override fun getPicturesFolder(): File {
                throw Exception()
            }
        }

        backupManager = BackupManagerImpl(
            coroutineScope,
            fileAdapter = fileAdapter,
            keyMapRepository = keyMapRepository,
            deviceInfoRepository = deviceInfoCache,
            preferenceRepository = preferenceRepository,
            fingerprintMapRepository = fingerprintMapRepository,
            throwExceptions = true,
            dispatchers = dispatcherProvider
        )

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
    fun `restore keymaps with no db version, assume version is 9 and don't show error message`() =
        coroutineScope.runBlockingTest {
            val fileName = "restore-keymaps-no-db-version.json"

            coroutineScope.pauseDispatcher()

            backupManager.restoreMappings(fileName)

            assertThat(backupManager.onRestoreResult.toListWithTimeout().size, `is`(1))

            coroutineScope.resumeDispatcher()

            verify(keyMapRepository, times(1)).restore(9, getKeyMapJsonListFromFile(fileName))
        }

    @Test
    fun `backup all fingerprint maps, don't save keymap db version and show success message`() =
        coroutineScope.runBlockingTest {
            `when`(fingerprintMapRepository.fingerprintMaps).then {
                flow {
                    emit(
                        FingerprintMapEntityGroup()
                    )
                }
            }

            val inputStream =
                PipedInputStream(outputStream) // must become before async call in BackupManager.backup

            coroutineScope.pauseDispatcher()

            backupManager.backupFingerprintMaps("")

            assertThat(
                backupManager.onBackupResult.toListWithTimeout(),
                `is`(listOf(Success(Unit)))
            )

            coroutineScope.resumeDispatcher()

            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = parser.parse(json).asJsonObject

            assertThat(
                "keymap db version is in json but it wasn't supposed to be there",
                !jsonObject.contains(BackupManagerImpl.NAME_KEYMAP_DB_VERSION)
            )
        }

    @Test
    fun `restore a single fingerprint map, only restore a single fingerprint map and a success message`() =
        coroutineScope.runBlockingTest {

            coroutineScope.pauseDispatcher()

            backupManager.restoreMappings("restore-single-fingerprint-map.json")

            assertThat(
                backupManager.onRestoreResult.toListWithTimeout(),
                `is`(listOf(Success(Unit)))
            )

            coroutineScope.resumeDispatcher()

            verify(fingerprintMapRepository, times(1)).restore(anyString(), anyString())
        }

    @Test
    fun `restore all fingerprint maps, all fingerprint maps should be restored and a success message`() =
        coroutineScope.runBlockingTest {

            val fileName = "restore-all-fingerprint-maps.json"

            coroutineScope.pauseDispatcher()

            backupManager.restoreMappings(fileName)

            assertThat(
                backupManager.onRestoreResult.toListWithTimeout(),
                `is`(listOf(Success(Unit)))
            )

            coroutineScope.resumeDispatcher()

            getFingerprintMapsToGestureIdFromFile(fileName).forEach { (gestureId, json) ->
                verify(fingerprintMapRepository, times(1)).restore(gestureId, json)
            }
        }

    @Test
    fun `restore many key maps and device info, all key maps and device info should be restored and a success message`() =
        coroutineScope.runBlockingTest {
            val fileName = "restore-many-keymaps.json"

            coroutineScope.pauseDispatcher()

            backupManager.restoreMappings(fileName)

            assertThat(
                backupManager.onRestoreResult.toListWithTimeout(),
                `is`(listOf(Success(Unit)))
            )
            coroutineScope.resumeDispatcher()

            verify(keyMapRepository, times(1)).restore(10, getKeyMapJsonListFromFile(fileName))
        }

    @Test
    fun `restore with key map db version greater than allowed version, send incompatible backup event`() =
        coroutineScope.runBlockingTest {

            coroutineScope.pauseDispatcher()
            backupManager.restoreMappings("restore-keymap-db-version-too-big.json")

            assertThat(
                backupManager.onRestoreResult.toListWithTimeout(),
                `is`(listOf(Error.BackupVersionTooNew))
            )

            coroutineScope.resumeDispatcher()

            verify(keyMapRepository, never()).restore(anyInt(), anyList())
        }

    @Test
    fun `restore with fingerprint gesture map db version greater than allowed version, send incompatible backup event`() =
        coroutineScope.runBlockingTest {

            coroutineScope.pauseDispatcher()
            backupManager.restoreMappings("restore-fingerprint-map-version-too-big.json")

            assertThat(
                backupManager.onRestoreResult.toListWithTimeout(),
                `is`(listOf(Error.BackupVersionTooNew))
            )

            coroutineScope.resumeDispatcher()

            verify(fingerprintMapRepository, never()).restore(anyString(), anyString())
        }

    @Test
    fun `restore empty file, show empty json error message`() = coroutineScope.runBlockingTest {

        coroutineScope.pauseDispatcher()
        backupManager.restoreMappings("empty.json")

        assertThat(
            backupManager.onRestoreResult.toListWithTimeout(),
            `is`(listOf(Error.EmptyJson))
        )

        coroutineScope.resumeDispatcher()
    }

    @Test
    fun `restore corrupt file, show corrupt json message`() = coroutineScope.runBlockingTest {
        coroutineScope.pauseDispatcher()
        backupManager.restoreMappings("corrupt.json")

        assertThat(
            backupManager.onRestoreResult.toListWithTimeout().single(),
            IsInstanceOf(Error.CorruptJsonFile::class.java)
        )

        coroutineScope.resumeDispatcher()
    }

    @Test
    fun `backup all fingerprint maps, return list of default fingerprint maps`() =
        coroutineScope.runBlockingTest {
            `when`(fingerprintMapRepository.fingerprintMaps).then {
                flow {
                    emit(
                        FingerprintMapEntityGroup()
                    )
                }
            }

            val inputStream =
                PipedInputStream(outputStream) // must become before async call in BackupManager.backup

            coroutineScope.pauseDispatcher()

            backupManager.backupFingerprintMaps("")

            assertThat(
                backupManager.onBackupResult.toListWithTimeout(),
                `is`(listOf(Success(Unit)))
            )

            coroutineScope.resumeDispatcher()

            val json = inputStream.bufferedReader().use { it.readText() }
            val rootElement = parser.parse(json)

            GESTURE_ID_TO_JSON_KEY_MAP.values.forEach { jsonKey ->
                assertThat(
                    "doesn't contain $jsonKey fingerprint map",
                    gson.toJson(rootElement[jsonKey]),
                    `is`(gson.toJson(FingerprintMapEntity()))
                )
            }
        }

    @Test
    fun `backup key maps, return list of default key maps, keymap db version should be current database version`() =
        coroutineScope.runBlockingTest {
            val keyMapList = listOf(KeyMapEntity(0), KeyMapEntity(1))
            `when`(keyMapRepository.keyMapList).then { flow { emit(State.Data(keyMapList)) } }

            val inputStream =
                PipedInputStream(outputStream) // must become before async call in BackupManager.backup

            coroutineScope.pauseDispatcher()

            backupManager.backupKeyMaps("", keyMapList.map { it.uid })

            assertThat(
                backupManager.onBackupResult.toListWithTimeout(),
                `is`(listOf(Success(Unit)))
            )

            coroutineScope.resumeDispatcher()

            val rootJsonElement = inputStream.bufferedReader().use { it.readText() }
            val rootElement = parser.parse(rootJsonElement)
            val keymapListJsonArray = rootElement["keymap_list"].asJsonArray

            keyMapList.forEachIndexed { index, keymap ->
                val expectedKeymapJson = gson.toJson(keymap)
                val actualKeymapJson = gson.toJson(keymapListJsonArray[index])

                assertThat(actualKeymapJson, `is`(expectedKeymapJson))
            }

            assertThat(
                rootElement["keymap_db_version"].asInt,
                `is`(AppDatabase.DATABASE_VERSION)
            )
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

    private fun getKeyMapJsonListFromFile(fileName: String): List<String> {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return rootElement["keymap_list"].asJsonArray.map { gson.toJson(it) }
    }

    private fun getJson(fileName: String): InputStream {
        return this.javaClass.classLoader!!.getResourceAsStream("backup-manager-test/$fileName")
    }
}