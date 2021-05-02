package io.github.sds100.keymapper

import com.github.salomonbrys.kotson.byObject
import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.sds100.keymapper.backup.BackupManagerImpl
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.FlowUtils.toListWithTimeout
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
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
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
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

    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    private lateinit var backupManager: BackupManagerImpl
    private lateinit var mockPreferenceRepository: PreferenceRepository
    private lateinit var mockKeyMapRepository: KeyMapRepository
    private lateinit var mockFingerprintMapRepository: FingerprintMapRepository
    private lateinit var outputStream: PipedOutputStream

    private lateinit var parser: JsonParser
    private lateinit var gson: Gson

    @Before
    fun init() {

        mockKeyMapRepository = mock {
            on { requestBackup } doReturn MutableSharedFlow()
        }

        mockFingerprintMapRepository = mock {
            on { requestBackup } doReturn MutableSharedFlow()
        }

        mockPreferenceRepository = mock {
            on { get<Any>(anyOrNull()) } doReturn flow { }
        }

        outputStream = PipedOutputStream()

        val fileAdapter = object : FileAdapter {
            override fun openOutputStream(uriString: String): Result<OutputStream> {
                return Success(outputStream)
            }

            override fun openInputStream(uriString: String): Result<InputStream> {
                return Success(getJson(uriString))
            }

            override fun openAsset(fileName: String): InputStream {
                throw Exception()
            }

            override fun getPicturesFolder(): File {
                throw Exception()
            }
        }

        backupManager = BackupManagerImpl(
            coroutineScope,
            fileAdapter = fileAdapter,
            keyMapRepository = mockKeyMapRepository,
            preferenceRepository = mockPreferenceRepository,
            fingerprintMapRepository = mockFingerprintMapRepository,
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
    fun `restore legacy backup with device info, success`() = coroutineScope.runBlockingTest {
        coroutineScope.pauseDispatcher()

        backupManager.restoreMappings("legacy-backup-test-data.json")

        assertThat(backupManager.onRestoreResult.toListWithTimeout(), `is`(listOf(Success(Unit))))

        coroutineScope.resumeDispatcher()

        verify(mockKeyMapRepository, times(1)).insert(any(), any())
        verify(mockFingerprintMapRepository, times(1)).update(any(), any(), any(), any())
    }

    @Test
    fun `restore keymaps with no db version, assume version is 9 and don't show error message`() =
        coroutineScope.runBlockingTest {
            val fileName = "restore-keymaps-no-db-version.json"

            coroutineScope.pauseDispatcher()

            backupManager.restoreMappings(fileName)

            assertThat(backupManager.onRestoreResult.toListWithTimeout().size, `is`(1))

            coroutineScope.resumeDispatcher()

            verify(mockKeyMapRepository, times(1)).insert(any(), any())
        }

    @Test
    fun `restore a single legacy fingerprint map, only restore a single fingerprint map and a success message`() =
        coroutineScope.runBlockingTest {

            coroutineScope.pauseDispatcher()

            backupManager.restoreMappings("restore-legacy-single-fingerprint-map.json")

            assertThat(
                backupManager.onRestoreResult.toListWithTimeout(),
                `is`(listOf(Success(Unit)))
            )

            coroutineScope.resumeDispatcher()

            verify(mockFingerprintMapRepository, times(1)).update(any())
        }

    @Test
    fun `restore all legacy fingerprint maps, all fingerprint maps should be restored and a success message`() =
        coroutineScope.runBlockingTest {

            val fileName = "restore-all-legacy-fingerprint-maps.json"

            coroutineScope.pauseDispatcher()

            backupManager.restoreMappings(fileName)

            assertThat(
                backupManager.onRestoreResult.toListWithTimeout(),
                `is`(listOf(Success(Unit)))
            )

            coroutineScope.resumeDispatcher()

            verify(mockFingerprintMapRepository, times(1)).update(any(), any(), any(), any())
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

            verify(mockKeyMapRepository, times(1)).insert(any(), any(), any(), any())
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

            verify(mockKeyMapRepository, never()).insert(anyVararg())
        }

    @Test
    fun `restore with legacy fingerprint gesture map db version greater than allowed version, send incompatible backup event`() =
        coroutineScope.runBlockingTest {

            coroutineScope.pauseDispatcher()
            backupManager.restoreMappings("restore-legacy-fingerprint-map-version-too-big.json")

            assertThat(
                backupManager.onRestoreResult.toListWithTimeout(),
                `is`(listOf(Error.BackupVersionTooNew))
            )

            coroutineScope.resumeDispatcher()

            verify(mockFingerprintMapRepository, never()).update(anyVararg())
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
    fun `backup all fingerprint maps, return list of fingerprint maps and app database version`() =
        coroutineScope.runBlockingTest {
            val fingerprintMapsToBackup = listOf(
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_DOWN),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_UP),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_LEFT),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_RIGHT)
            )

            whenever(mockFingerprintMapRepository.fingerprintMapList).then {
                flow { emit(State.Data(fingerprintMapsToBackup)) }
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

            assertThat(
                gson.toJson(rootElement["fingerprint_map_list"]),
                `is`(gson.toJson(fingerprintMapsToBackup))
            )

            assertThat(
                rootElement["keymap_db_version"].asInt,
                `is`(AppDatabase.DATABASE_VERSION)
            )
        }

    @Test
    fun `backup key maps, return list of default key maps, keymap db version should be current database version`() =
        coroutineScope.runBlockingTest {
            val keyMapList = listOf(KeyMapEntity(0), KeyMapEntity(1))
            whenever(mockKeyMapRepository.keyMapList).then { flow { emit(State.Data(keyMapList)) } }

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

    private fun getLegacyFingerprintMapsFromFile(fileName: String): Map<String, String> {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        val legacyGestureIdsToJsonMap = mapOf(
            "swipe_down" to "fingerprint_swipe_down",
            "swipe_up" to "fingerprint_swipe_up",
            "swipe_left" to "fingerprint_swipe_left",
            "swipe_up" to "fingerprint_swipe_up",
        )

        return legacyGestureIdsToJsonMap.map { (gestureId, jsonKey) ->
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

    private fun getFingerprintMapJsonListFromFile(fileName: String): List<String> {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return rootElement["fingerprint_map_list"].asJsonArray.map { gson.toJson(it) }
    }

    private fun getJson(fileName: String): InputStream {
        return this.javaClass.classLoader!!.getResourceAsStream("backup-manager-test/$fileName")
    }
}