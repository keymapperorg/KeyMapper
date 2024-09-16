package io.github.sds100.keymapper

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.db.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/**
 * Created by sds100 on 05/06/20.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    companion object {
        private const val TEST_DB_NAME = "migration_test"

        private val MIGRATION_1_2_TEST_DATA = arrayOf(
            arrayOf(1, "[]", 0, 1, "NULL", "NULL", "NULL"),
            arrayOf(2, "[{\"keys\":[25]}]", 4, 1, "APP", "com.android.chrome", "[]"),
            arrayOf(3, "[{\"keys\":[25,24]}]", 0, 1, "KEY", "24", "[]"),
            arrayOf(4, "[{\"keys\":[25,24]}]", 0, 1, "KEYCODE", "24", "[]"),
            arrayOf(
                5,
                "[{\"keys\":[25,24]},{\"keys\":[25]}]",
                0,
                1,
                "SYSTEM_ACTION",
                "toggle_flashlight",
                "[{\"data\":\"option_lens_back\",\"id\":\"extra_flash\"}]",
            ),
            arrayOf(6, "[{\"keys\":[4]}]", 3, 1, "SYSTEM_ACTION", "volume_mute", "[]"),
        )

        private val MIGRATION_1_2_EXPECTED_DATA = arrayOf(
            arrayOf(1, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(
                2,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25}],\"mode\":1}",
                "[{\"data\":\"com.android.chrome\",\"extras\":[],\"flags\":0,\"type\":\"APP\"}]",
                "[]",
                1,
                1,
                "NULL",
                1,
            ),
            arrayOf(
                3,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"24\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                4,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"24\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                5,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"toggle_flashlight\",\"extras\":[{\"data\":\"option_lens_back\",\"id\":\"extra_flash\"}],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                6,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25}],\"mode\":1}",
                "[{\"data\":\"toggle_flashlight\",\"extras\":[{\"data\":\"option_lens_back\",\"id\":\"extra_flash\"}],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                7,
                "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":4}],\"mode\":1}",
                "[{\"data\":\"volume_mute\",\"extras\":[],\"flags\":1,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_2_3_TEST_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[{\"data\":\"610\",\"id\":\"extra_repeat_delay\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":1}",
                "[{\"data\":\"10\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                1,
                "NULL",
                1,
            ),
            arrayOf(2, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(
                3,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                4,
                "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"14\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_2_3_EXPECTED_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[{\"data\":\"610\",\"id\":\"extra_repeat_delay\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":1}",
                "[{\"data\":\"10\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                17,
                "NULL",
                1,
            ),
            arrayOf(2, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(
                3,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                4,
                "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"14\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                16,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_3_4_TEST_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[{\"data\":\"610\",\"id\":\"extra_repeat_delay\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":1}",
                "[{\"data\":\"10\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                17,
                "NULL",
                1,
            ),
            arrayOf(2, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(
                3,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                4,
                "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"14\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                16,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_3_4_EXPECTED_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[{\"data\":\"610\",\"id\":\"extra_repeat_delay\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":2}",
                "[{\"data\":\"10\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                17,
                "NULL",
                1,
            ),
            arrayOf(2, "{\"extras\":[],\"keys\":[],\"mode\":2}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(
                3,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                4,
                "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"14\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                16,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_4_5_TEST_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"volume_up\",\"extras\":[],\"flags\":1,\"type\":\"SYSTEM_ACTION\"},{\"data\":\"com.android.settings\",\"extras\":[],\"flags\":0,\"type\":\"APP\"}]",
                "[]",
                1,
                16,
                "NULL",
                1,
            ),
            arrayOf(
                2,
                "{\"extras\":[{\"data\":\"5000\",\"id\":\"extra_hold_down_until_repeat_delay\"},{\"data\":\"575\",\"id\":\"extra_repeat_delay\"},{\"data\":\"365\",\"id\":\"extra_vibration_duration\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":2}",
                "[{\"data\":\"7\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                19,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_4_5_EXPECTED_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}",
                "[{\"data\":\"volume_up\",\"extras\":[],\"flags\":5,\"type\":\"SYSTEM_ACTION\"},{\"data\":\"com.android.settings\",\"extras\":[],\"flags\":4,\"type\":\"APP\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
            arrayOf(
                2,
                "{\"extras\":[{\"data\":\"365\",\"id\":\"extra_vibration_duration\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":2}",
                "[{\"data\":\"7\",\"extras\":[{\"data\":\"5000\",\"id\":\"extra_hold_down_until_repeat_delay\"},{\"data\":\"575\",\"id\":\"extra_repeat_delay\"}],\"flags\":6,\"type\":\"KEY_EVENT\"}]",
                "[]",
                1,
                1,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_5_6_TEST_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[{\"data\":\"2930\",\"id\":\"extra_sequence_trigger_timeout\"},{\"data\":\"1840\",\"id\":\"extra_long_press_delay\"},{\"data\":\"3580\",\"id\":\"extra_double_press_timeout\"},{\"data\":\"390\",\"id\":\"extra_vibration_duration\"}],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":2,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":1}",
                "[{\"data\":\"volume_up\",\"extras\":[],\"flags\":1,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                5,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_5_6_EXPECTED_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[{\"data\":\"2930\",\"id\":\"extra_sequence_trigger_timeout\"},{\"data\":\"1840\",\"id\":\"extra_long_press_delay\"},{\"data\":\"3580\",\"id\":\"extra_double_press_timeout\"},{\"data\":\"390\",\"id\":\"extra_vibration_duration\"}],\"flags\":5,\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":2,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":1}",
                "[{\"data\":\"volume_up\",\"extras\":[],\"flags\":1,\"type\":\"SYSTEM_ACTION\"}]",
                "[]",
                1,
                0,
                "NULL",
                1,
            ),
        )

        private val MIGRATION_9_10_TEST_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[],\"flags\":0,\"keys\":[],\"mode\":2}",
                "[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":2,\"type\":\"APP\",\"uid\":\"dc2d8c69-aaa5-4471-b981-17cba7677c1a\"}]",
                "[]",
                1,
                0,
                "",
                1,
                "d314e9e8-fac9-43e7-b540-0b9c0bfb4238",
            ),
            arrayOf(
                2,
                "{\"extras\":[],\"flags\":1,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25,\"uid\":\"9d5d6f0b-1b9a-44ba-9406-1caaacea05de\"}],\"mode\":2}",
                "[{\"data\":\"com.discord\",\"extras\":[],\"flags\":2,\"type\":\"APP\",\"uid\":\"86c78374-59ce-4050-94a4-299f4778658f\"}]",
                "[]",
                1,
                0,
                "",
                1,
                "b854ece7-2f0e-45c4-9cf3-bb5aa4fad288",
            ),
            arrayOf(
                3,
                "{\"extras\":[],\"flags\":0,\"keys\":[],\"mode\":2}",
                "[{\"data\":\"com.google.android.vr.home\",\"extras\":[],\"flags\":2,\"type\":\"APP\",\"uid\":\"ca5b18ee-2673-4443-b2a7-cedef2c455b2\"},{\"data\":\"com.google.android.calendar\",\"extras\":[],\"flags\":0,\"type\":\"APP\",\"uid\":\"d274a5a8-f48e-4fbf-acbd-ed3c4b4ff377\"},{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":2,\"type\":\"SYSTEM_ACTION\",\"uid\":\"f6b2afc5-4265-403d-8a0f-4eacf245286f\"}]",
                "[]",
                1,
                0,
                "",
                1,
                "75ab7552-c175-4df4-9f50-1a9b86e717cc",
            ),
        )

        private val MIGRATION_9_10_EXPECTED_DATA = arrayOf(
            arrayOf(
                1,
                "{\"extras\":[],\"flags\":16,\"keys\":[],\"mode\":2}",
                "[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":0,\"type\":\"APP\",\"uid\":\"dc2d8c69-aaa5-4471-b981-17cba7677c1a\"}]",
                "[]",
                1,
                0,
                "",
                1,
                "d314e9e8-fac9-43e7-b540-0b9c0bfb4238",
            ),
            arrayOf(
                2,
                "{\"extras\":[],\"flags\":17,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25,\"uid\":\"9d5d6f0b-1b9a-44ba-9406-1caaacea05de\"}],\"mode\":2}",
                "[{\"data\":\"com.discord\",\"extras\":[],\"flags\":0,\"type\":\"APP\",\"uid\":\"86c78374-59ce-4050-94a4-299f4778658f\"}]",
                "[]",
                1,
                0,
                "",
                1,
                "b854ece7-2f0e-45c4-9cf3-bb5aa4fad288",
            ),
            arrayOf(
                3,
                "{\"extras\":[],\"flags\":16,\"keys\":[],\"mode\":2}",
                "[{\"data\":\"com.google.android.vr.home\",\"extras\":[],\"flags\":0,\"type\":\"APP\",\"uid\":\"ca5b18ee-2673-4443-b2a7-cedef2c455b2\"},{\"data\":\"com.google.android.calendar\",\"extras\":[],\"flags\":0,\"type\":\"APP\",\"uid\":\"d274a5a8-f48e-4fbf-acbd-ed3c4b4ff377\"},{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\",\"uid\":\"f6b2afc5-4265-403d-8a0f-4eacf245286f\"}]",
                "[]",
                1,
                0,
                "",
                1,
                "75ab7552-c175-4df4-9f50-1a9b86e717cc",
            ),
        )
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val coroutineScope = MainScope()

    private lateinit var jsonParser: JsonParser
    private lateinit var gson: Gson

    @Before
    fun init() {
        jsonParser = JsonParser()
        gson = Gson()
    }

    /**
     * issue #612
     */
    @Test
    @Throws(IOException::class)
    fun migrate11to12() {
        val legacyFingerprintMapsDataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = coroutineScope,
            produceFile = { File.createTempFile("test", ".preferences_pb") },
        )

        val testDataFileName = "migration-11-12-test-data.json"
        val expectedDataFileName = "migration-11-12-expected-data.json"
        val testDataJson = getJsonFileText(testDataFileName)

        runBlocking {
            val rootElement = jsonParser.parse(testDataJson)

            legacyFingerprintMapsDataStore.edit { preferences ->
                preferences.putAll(
                    stringPreferencesKey("swipe_down") to gson.toJson(rootElement["fingerprint_swipe_down"]),
                    stringPreferencesKey("swipe_up") to gson.toJson(rootElement["fingerprint_swipe_up"]),
                    stringPreferencesKey("swipe_left") to gson.toJson(rootElement["fingerprint_swipe_left"]),
                    stringPreferencesKey("swipe_right") to gson.toJson(rootElement["fingerprint_swipe_right"]),
                )
            }
        }

        val fromVersion = 11
        val toVersion = 12
        val migration = AppDatabase.RoomMigration11To12(legacyFingerprintMapsDataStore)
        val keyMapColumnNameToJsonNameMap = mapOf(
            "id" to "id",
            "trigger" to "trigger",
            "action_list" to "actionList",
            "constraint_list" to "constraintList",
            "constraint_mode" to "constraintMode",
            "flags" to "flags",
            "folder_name" to "folderName",
            "is_enabled" to "isEnabled",
            "uid" to "uid",
        )
        val fingerprintMapColumnNameToJsonNameMap = mapOf(
            "id" to "id",
            "action_list" to "action_list",
            "constraint_list" to "constraints",
            "constraint_mode" to "constraint_mode",
            "extras" to "extras",
            "flags" to "flags",
            "is_enabled" to "enabled",
        )

        // do this without using the test() method because fingerprint maps are stored differently in test file

        helper.createDatabase(TEST_DB_NAME, fromVersion).apply {
            val keyMapJsonArray = getKeyMapListJsonFromFile(testDataFileName)
            insertMappingListJsonIntoDatabase(
                this,
                keyMapJsonArray,
                keyMapColumnNameToJsonNameMap,
                "keymaps",
            )

            val deviceInfoJsonArray = jsonParser.parse(testDataJson)["device_info"].asJsonArray

            deviceInfoJsonArray.forEach { element ->

                val descriptor = element["descriptor"].asString
                val name = element["name"].asString

                this.execSQL(
                    """
                    INSERT INTO deviceinfo (descriptor, name) VALUES ('$descriptor', '$name')
                    """,
                )
            }
            // dont insert test data fingerprintmaps into database because they weren't in version 11
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, toVersion, true, migration)

        val expectedKeyMapJsonList =
            getKeyMapListJsonFromFile(expectedDataFileName).map { element ->

                val values = keyMapColumnNameToJsonNameMap.values.map { key ->
                    element.convertValueToJson(key).convertJsonValueToSqlValue()
                }

                values.toTypedArray()
            }

        testColumnsMatch(db, tableName = "keymaps", expectedKeyMapJsonList.toTypedArray())

        val expectedFingerprintMapJsonList =
            getFingerprintMapListJsonFromFile(expectedDataFileName).map { element ->
                val values = fingerprintMapColumnNameToJsonNameMap.values.map { key ->
                    element.convertValueToJson(key).convertJsonValueToSqlValue()
                }

                values.toTypedArray()
            }

        testColumnsMatch(
            db,
            tableName = "fingerprintmaps",
            expectedFingerprintMapJsonList.toTypedArray(),
        )
    }

    /**
     * issue #621
     */
    @Test
    @Throws(IOException::class)
    fun migrate10to11() {
        test(
            fromVersion = 10,
            toVersion = 11,
            migration = AppDatabase.MIGRATION_10_11,
            testDataFileName = "migration-10-11-test-data.json",
            expectedDataFileName = "migration-10-11-expected-data.json",
            keyMapColumnNameToJsonNameMap = mapOf(
                "id" to "id",
                "trigger" to "trigger",
                "action_list" to "actionList",
                "constraint_list" to "constraintList",
                "constraint_mode" to "constraintMode",
                "flags" to "flags",
                "folder_name" to "folderName",
                "is_enabled" to "isEnabled",
                "uid" to "uid",
            ),
        )
    }

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate9to10() {
        var db = helper.createDatabase(TEST_DB_NAME, 9).apply {
            MIGRATION_9_10_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger, action_list, constraint_list, constraint_mode, flags, folder_name, is_enabled, uid)
                    VALUES (${row.joinToString { "'$it'" }})
                    """,
                )
            }
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 10, true, AppDatabase.MIGRATION_9_10)

        testColumnsMatch(db, tableName = "keymaps", MIGRATION_9_10_EXPECTED_DATA)
    }

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate5to6() {
        var db = helper.createDatabase(TEST_DB_NAME, 5).apply {
            MIGRATION_5_6_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger, action_list, constraint_list, constraint_mode, flags, folder_name, is_enabled)
                    VALUES (${row.joinToString { "'$it'" }})
                    """,
                )
            }
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 6, true, AppDatabase.MIGRATION_5_6)

        testColumnsMatch(db, tableName = "keymaps", MIGRATION_5_6_EXPECTED_DATA)
    }

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate4to5() {
        var db = helper.createDatabase(TEST_DB_NAME, 4).apply {
            MIGRATION_4_5_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger, action_list, constraint_list, constraint_mode, flags, folder_name, is_enabled)
                    VALUES (${row.joinToString { "'$it'" }})
                    """,
                )
            }
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 5, true, AppDatabase.MIGRATION_4_5)

        testColumnsMatch(db, tableName = "keymaps", MIGRATION_4_5_EXPECTED_DATA)
    }

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate3to4() {
        var db = helper.createDatabase(TEST_DB_NAME, 3).apply {
            MIGRATION_3_4_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger, action_list, constraint_list, constraint_mode, flags, folder_name, is_enabled)
                    VALUES (${row.joinToString { "'$it'" }})
                    """,
                )
            }
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 4, true, AppDatabase.MIGRATION_3_4)

        testColumnsMatch(db, tableName = "keymaps", MIGRATION_3_4_EXPECTED_DATA)
    }

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate2to3() {
        var db = helper.createDatabase(TEST_DB_NAME, 2).apply {
            MIGRATION_2_3_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger, action_list, constraint_list, constraint_mode, flags, folder_name, is_enabled)
                    VALUES (${row.joinToString { "'$it'" }})
                    """,
                )
            }
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, AppDatabase.MIGRATION_2_3)

        testColumnsMatch(db, tableName = "keymaps", MIGRATION_2_3_EXPECTED_DATA)
    }

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate1to2() {
        var db = helper.createDatabase(TEST_DB_NAME, 1).apply {
            MIGRATION_1_2_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger_list, flags, is_enabled, action_type, action_data, action_extras)
                    VALUES (${row.joinToString { "'$it'" }})
                    """,
                )
            }

            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, AppDatabase.MIGRATION_1_2)
        testColumnsMatch(db, tableName = "keymaps", MIGRATION_1_2_EXPECTED_DATA)
    }

    private fun test(
        fromVersion: Int,
        toVersion: Int,
        migration: Migration,
        testDataFileName: String,
        expectedDataFileName: String,
        keyMapColumnNameToJsonNameMap: Map<String, String>,
        fingerprintMapColumnNameToJsonNameMap: Map<String, String>? = null,
    ) {
        helper.createDatabase(TEST_DB_NAME, fromVersion).apply {
            val keyMapJsonArray = getKeyMapListJsonFromFile(testDataFileName)
            insertMappingListJsonIntoDatabase(
                this,
                keyMapJsonArray,
                keyMapColumnNameToJsonNameMap,
                "keymaps",
            )

            if (fingerprintMapColumnNameToJsonNameMap != null) {
                val fingerprintMapJsonArray = getFingerprintMapListJsonFromFile(testDataFileName)
                insertMappingListJsonIntoDatabase(
                    this,
                    fingerprintMapJsonArray,
                    fingerprintMapColumnNameToJsonNameMap,
                    "fingerprintmaps",
                )
            }
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, toVersion, true, migration)

        val expectedKeyMapJsonList =
            getKeyMapListJsonFromFile(expectedDataFileName).map { element ->

                val values = keyMapColumnNameToJsonNameMap.values.map { key ->
                    element.convertValueToJson(key).convertJsonValueToSqlValue()
                }

                values.toTypedArray()
            }

        testColumnsMatch(db, tableName = "keymaps", expectedKeyMapJsonList.toTypedArray())

        if (fingerprintMapColumnNameToJsonNameMap != null) {
            val expectedFingerprintMapJsonList =
                getFingerprintMapListJsonFromFile(expectedDataFileName).map { element ->
                    val values = fingerprintMapColumnNameToJsonNameMap.values.map { key ->
                        element.convertValueToJson(key).convertJsonValueToSqlValue()
                    }

                    values.toTypedArray()
                }

            testColumnsMatch(
                db,
                tableName = "fingerprintmaps",
                expectedFingerprintMapJsonList.toTypedArray(),
            )
        }
    }

    private fun insertMappingListJsonIntoDatabase(
        database: SupportSQLiteDatabase,
        mappingListJson: JsonArray,
        columnNameToJsonMap: Map<String, String>,
        tableName: String,
    ) {
        mappingListJson.forEach { element ->
            val jsonElementNames = columnNameToJsonMap.values

            val values = jsonElementNames.map { key ->
                element.convertValueToJson(key).convertJsonValueToSqlValue()
            }

            database.execSQL(
                """
                    INSERT INTO $tableName (${columnNameToJsonMap.keys.joinToString()})
                    VALUES (${values.joinToString { "'$it'" }})
                    """,
            )
        }
    }

    private fun String?.convertJsonValueToSqlValue() =
        when {
            this == null -> "NULL"
            this == "true" -> 1
            this == "false" -> 0
            this.toIntOrNull() != null -> this.toInt()
            else -> this
        }

    private fun JsonElement.convertValueToJson(key: String) = try {
        gson.toJson(this[key])
    } catch (e: NoSuchElementException) {
        null
    }

    private fun getKeyMapListJsonFromFile(fileName: String): JsonArray {
        val json = getJsonFileText(fileName)

        val rootElement = jsonParser.parse(json)

        return rootElement["keymap_list"].asJsonArray
    }

    private fun getFingerprintMapListJsonFromFile(fileName: String): JsonArray {
        val json = getJsonFileText(fileName)

        val rootElement = jsonParser.parse(json)

        return rootElement["fingerprint_map_list"].asJsonArray
    }

    private fun getJsonFileText(fileName: String): String {
        val inputStream =
            this.javaClass.classLoader!!.getResourceAsStream("json-migration-test/$fileName")
        return inputStream.bufferedReader().use { it.readText() }
    }

    private fun testColumnsMatch(
        db: SupportSQLiteDatabase,
        tableName: String,
        expectedData: Array<out Array<out Any>>,
    ) {
        val cursor = db.query("SELECT * FROM $tableName")

        ViewMatchers.assertThat("Check the logcat", cursor.count, Matchers.`is`(expectedData.size))

        while (cursor.moveToNext()) {
            val row = cursor.position
            val expectedColumnValues: Array<out Any> = expectedData[row]

            cursor.columnNames.forEachIndexed { columnIndex, columnName ->

                val expectedColumnValue = expectedColumnValues[columnIndex]

                val columnValue: Any = when (expectedColumnValue) {
                    is Int -> cursor.getInt(columnIndex)
                    is String -> cursor.getString(columnIndex)
                    else -> throw Exception("Don't know how to get this type ${expectedColumnValue::class.simpleName} from cursor")
                }

                when (expectedColumnValue) {
                    is Int -> ViewMatchers.assertThat(
                        "$columnName at row $row doesn't match",
                        columnValue,
                        Matchers.`is`(expectedColumnValue),
                    )
                    is String -> {
                        try {
                            JsonTestUtils.compareBothWays(
                                element = jsonParser.parse(expectedColumnValue),
                                elementName = "expected $columnName at row $row",
                                other = jsonParser.parse(columnValue as String),
                                otherName = "migrated $columnName at row $row",
                            )
                        } catch (e: JsonParseException) {
                            ViewMatchers.assertThat(
                                "$columnName at row $row doesn't match",
                                columnValue,
                                Matchers.`is`(expectedColumnValue),
                            )
                        }
                    }
                }
            }
        }
    }
}
