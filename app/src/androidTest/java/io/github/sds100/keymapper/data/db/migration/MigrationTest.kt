package io.github.sds100.keymapper.data.db.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.sds100.keymapper.data.db.AppDatabase
import org.hamcrest.Matchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Created by sds100 on 05/06/20.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    companion object {
        private val TEST_DB = "migration_test"

        private val MIGRATION_1_2_TEST_DATA = arrayOf(
            arrayOf(1, "[]", 0, 1, "NULL", "NULL", "NULL"),
            arrayOf(2, "[{\"keys\":[25]}]", 4, 1, "APP", "com.android.chrome", "[]"),
            arrayOf(3, "[{\"keys\":[25,24]}]", 0, 1, "KEY", "24", "[]"),
            arrayOf(4, "[{\"keys\":[25,24]},{\"keys\":[25]}]", 0, 1, "SYSTEM_ACTION", "toggle_flashlight", "[{\"data\":\"option_lens_back\",\"id\":\"extra_flash\"}]"),
            arrayOf(5, "[{\"keys\":[4]}]", 3, 1, "SYSTEM_ACTION", "volume_mute", "[]")
        )

        private val MIGRATION_1_2_EXPECTED_DATA = arrayOf(
            arrayOf(1, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(2, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25}],\"mode\":1}", "[{\"data\":\"com.android.chrome\",\"extras\":[],\"flags\":0,\"type\":\"APP\"}]", "[]", 1, 1, "NULL", 1),
            arrayOf(3, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"24\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 0, "NULL", 1),
            arrayOf(4, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"toggle_flashlight\",\"extras\":[{\"data\":\"option_lens_back\",\"id\":\"extra_flash\"}],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]", "[]", 1, 0, "NULL", 1),
            arrayOf(5, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25}],\"mode\":1}", "[{\"data\":\"toggle_flashlight\",\"extras\":[{\"data\":\"option_lens_back\",\"id\":\"extra_flash\"}],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]", "[]", 1, 0, "NULL", 1),
            arrayOf(6, "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":4}],\"mode\":1}", "[{\"data\":\"volume_mute\",\"extras\":[],\"flags\":1,\"type\":\"SYSTEM_ACTION\"}]", "[]", 1, 0, "NULL", 1)
        )

        private val MIGRATION_2_3_TEST_DATA = arrayOf(
            arrayOf(1, "{\"extras\":[{\"data\":\"610\",\"id\":\"extra_repeat_delay\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":1}", "[{\"data\":\"10\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 1, "NULL", 1),
            arrayOf(2, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(3, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]", "[]", 1, 0, "NULL", 1),
            arrayOf(4, "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"14\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 0, "NULL", 1)
        )

        private val MIGRATION_2_3_EXPECTED_DATA = arrayOf(
            arrayOf(1, "{\"extras\":[{\"data\":\"610\",\"id\":\"extra_repeat_delay\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":1}", "[{\"data\":\"10\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 17, "NULL", 1),
            arrayOf(2, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(3, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]", "[]", 1, 0, "NULL", 1),
            arrayOf(4, "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"14\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 16, "NULL", 1)
        )

        private val MIGRATION_3_4_TEST_DATA = arrayOf(
            arrayOf(1, "{\"extras\":[{\"data\":\"610\",\"id\":\"extra_repeat_delay\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":1}", "[{\"data\":\"10\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 17, "NULL", 1),
            arrayOf(2, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(3, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]", "[]", 1, 0, "NULL", 1),
            arrayOf(4, "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"14\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 16, "NULL", 1)
        )

        private val MIGRATION_3_4_EXPECTED_DATA = arrayOf(
            arrayOf(1, "{\"extras\":[{\"data\":\"610\",\"id\":\"extra_repeat_delay\"}],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25}],\"mode\":2}", "[{\"data\":\"10\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 17, "NULL", 1),
            arrayOf(2, "{\"extras\":[],\"keys\":[],\"mode\":2}", "[]", "[]", 1, 0, "NULL", 1),
            arrayOf(3, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"enable_mobile_data\",\"extras\":[],\"flags\":0,\"type\":\"SYSTEM_ACTION\"}]", "[]", 1, 0, "NULL", 1),
            arrayOf(4, "{\"extras\":[],\"keys\":[{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":25},{\"clickType\":1,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"14\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 16, "NULL", 1)
        )
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate3to4() {
        var db = helper.createDatabase(TEST_DB, 3).apply {

            MIGRATION_3_4_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger, action_list, constraint_list, constraint_mode, flags, folder_name, is_enabled)
                    VALUES (${row.joinToString { "'$it'" }})
                    """)
            }
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4)

        val cursor = db.query("SELECT trigger FROM keymaps")

        MIGRATION_3_4_EXPECTED_DATA.forEachIndexed { row, expectedData ->
            val expectedTrigger = expectedData[1]

            cursor.moveToNext()
            val triggerColumnIndex = cursor.getColumnIndex("trigger")
            val actualTrigger = cursor.getString(triggerColumnIndex)

            assertThat("trigger at row $row", actualTrigger, `is`(expectedTrigger))
        }
    }

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate2to3() {
        var db = helper.createDatabase(TEST_DB, 2).apply {

            MIGRATION_2_3_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger, action_list, constraint_list, constraint_mode, flags, folder_name, is_enabled)
                    VALUES (${row.joinToString { "'$it'" }})
                    """)
            }
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3)

        val cursor = db.query("SELECT flags FROM keymaps")

        MIGRATION_2_3_EXPECTED_DATA.forEachIndexed { row, expectedData ->
            val expectedFlags = expectedData[5]

            cursor.moveToNext()
            val flagsColumnIndex = cursor.getColumnIndex("flags")
            val actualFlags = cursor.getInt(flagsColumnIndex)

            assertThat("flags at row $row", actualFlags, `is`(expectedFlags))
        }
    }

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @Test
    @Throws(IOException::class)
    fun migrate1to2() {

        var db = helper.createDatabase(TEST_DB, 1).apply {

            MIGRATION_1_2_TEST_DATA.forEach { row ->

                execSQL(
                    """
                    INSERT INTO keymaps (id, trigger_list, flags, is_enabled, action_type, action_data, action_extras)
                    VALUES (${row.joinToString { "'$it'" }})
                    """)
            }
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT * FROM keymaps")

        assertThat("Check the logcat", cursor.count, `is`(MIGRATION_1_2_EXPECTED_DATA.size))

        while (cursor.moveToNext()) {
            val row = cursor.position
            val expectedColumnValues: Array<out Any> = MIGRATION_1_2_EXPECTED_DATA[row]

            //id
            assertThat("id at row $row", cursor.getInt(0), `is`(expectedColumnValues[0] as Int))

            //trigger
            assertThat("trigger at row $row", cursor.getString(1), `is`(expectedColumnValues[1] as String))

            //action list
            assertThat("action list at row $row", cursor.getString(2), `is`(expectedColumnValues[2] as String))

            //constraint list
            assertThat("constraint list at row $row", cursor.getString(3), `is`(expectedColumnValues[3] as String))

            //constraint mode
            assertThat("constraint mode at row $row", cursor.getInt(4), `is`(expectedColumnValues[4] as Int))

            //flags
            assertThat("flags at row $row", cursor.getInt(5), `is`(expectedColumnValues[5] as Int))

            //folder name
            assertThat("folder name at row $row", cursor.getString(6), `is`(expectedColumnValues[6] as String))

            //is enabled
            assertThat("isEnabled at row $row", cursor.getInt(7), `is`(expectedColumnValues[7] as Int))
        }
    }
}