package io.github.sds100.keymapper

import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.backup.BackupManagerImpl
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.FlowUtils.toListWithTimeout
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
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import timber.log.Timber
import java.io.*


/**
 * Created by sds100 on 19/04/2021.
 */

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BackupManagerTest {

    companion object {
        private val REGEX_DATA_JSON_PATH = Regex("backup_temp/.*/data.json")
    }

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    private lateinit var backupManager: BackupManagerImpl
    private lateinit var mockFileAdapter: FileAdapter
    private lateinit var fakePreferenceRepository: PreferenceRepository
    private lateinit var mockKeyMapRepository: KeyMapRepository
    private lateinit var mockFingerprintMapRepository: FingerprintMapRepository
    private lateinit var mockSoundsManager: SoundsManager
    private lateinit var dataJsonOutputStream: PipedOutputStream

    private lateinit var parser: JsonParser
    private lateinit var gson: Gson

    @Before
    fun init() {
        Timber.plant(TestLoggingTree())

        mockKeyMapRepository = mock {
            on { requestBackup } doReturn MutableSharedFlow()
        }

        mockFingerprintMapRepository = mock {
            on { requestBackup } doReturn MutableSharedFlow()
        }

        fakePreferenceRepository = FakePreferenceRepository()

        dataJsonOutputStream = PipedOutputStream()

        mockFileAdapter = mock {
            on { openOutputStream(any()) }.then {
                Success(dataJsonOutputStream)
            }

            on { openInputStream(any()) }.then {
                Success(getJson(it.getArgument(0)))
            }

            on { createPrivateFile(argThat { REGEX_DATA_JSON_PATH.matches(this) }) }.then {
                Success(dataJsonOutputStream)
            }

            on { createPrivateDirectory(any()) }.then {
                Success(Unit)
            }

            on { createZipFile(any(), any()) }.then {
                Success(Unit)
            }
        }

        mockSoundsManager = mock()

        backupManager = BackupManagerImpl(
            coroutineScope,
            fileAdapter = mockFileAdapter,
            keyMapRepository = mockKeyMapRepository,
            preferenceRepository = fakePreferenceRepository,
            fingerprintMapRepository = mockFingerprintMapRepository,
            throwExceptions = true,
            dispatchers = dispatcherProvider,
            soundsManager = mockSoundsManager
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
    fun `backup sound file if there is a key map with a sound action`() = coroutineScope.runBlockingTest {
        //GIVEN
        val soundFileName = "sound.ogg"
        val soundFileRegex = Regex("backup_temp/.*/sounds/$soundFileName")
        val soundFolderRegex = Regex("backup_temp/.*/sounds")

        val action = ActionEntity(
            type = ActionEntity.Type.SOUND, data = soundFileName,
            extra = Extra(ActionEntity.EXTRA_SOUND_FILE_DESCRIPTION, "sound_description")
        )

        val keyMapList = listOf(KeyMapEntity(id = 0, actionList = listOf(action)))

        whenever(mockKeyMapRepository.keyMapList).then { flow { emit(State.Data(keyMapList)) } }

        whenever(mockFileAdapter.createPrivateFile(argThat { soundFileRegex.matches(this) })).then {
            Success(ByteArrayOutputStream())
        }

        whenever(mockSoundsManager.getSound(soundFileName)).then {
            Success(ByteArrayInputStream(byteArrayOf(0)))
        }

        val inputStream =
            PipedInputStream(dataJsonOutputStream) // must become before async call in BackupManager.backup

        //WHEN
        coroutineScope.pauseDispatcher()

        backupManager.backupKeyMaps("", keyMapList.map { it.uid })

        assertThat(
            backupManager.onBackupResult.toListWithTimeout(),
            `is`(listOf(Success(Unit)))
        )

        coroutineScope.resumeDispatcher()

        //THEN
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

        verify(mockFileAdapter, times(1)).createZipFile(destination = any(), files = check { files ->
            assertThat(files.size, `is`(2))
            assertThat(files.any { REGEX_DATA_JSON_PATH.matches(it) }, `is`(true))
            assertThat(files.any { soundFolderRegex.matches(it) }, `is`(true))
        })
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
                PipedInputStream(dataJsonOutputStream) // must become before async call in BackupManager.backup

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

            verify(mockFileAdapter, times(1)).createZipFile(destination = any(), files = check { files ->
                println(files.toString())
                assertThat(files.size, `is`(1))
                assertThat(files.any { REGEX_DATA_JSON_PATH.matches(it) }, `is`(true))
            })
        }

    @Test
    fun `backup key maps, return list of default key maps, keymap db version should be current database version`() =
        coroutineScope.runBlockingTest {
            val keyMapList = listOf(KeyMapEntity(0), KeyMapEntity(1))
            whenever(mockKeyMapRepository.keyMapList).then { flow { emit(State.Data(keyMapList)) } }

            val inputStream =
                PipedInputStream(dataJsonOutputStream) // must become before async call in BackupManager.backup

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

            verify(mockFileAdapter, times(1)).createZipFile(destination = any(), files = check { files ->
                assertThat(files.size, `is`(1))
                assertThat(files.any { REGEX_DATA_JSON_PATH.matches(it) }, `is`(true))
            })
        }

    private fun getJson(fileName: String): InputStream {
        return this.javaClass.classLoader!!.getResourceAsStream("backup-manager-test/$fileName")
    }
}