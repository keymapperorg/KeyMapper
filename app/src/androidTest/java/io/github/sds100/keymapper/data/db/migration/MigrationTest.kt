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

        private val Migration_1_2_TEST_DATA = listOf(
            arrayOf(1, "[]", 0, 1, "NULL", "NULL", "NULL")
                to arrayOf(1, "{\"extras\":[],\"keys\":[],\"mode\":1}", "[]", "[]", 1, 0, "NULL", 1),

            arrayOf(2, "[{\"keys\":[25]}]", 4, 1, "APP", "com.android.chrome", "[]")
                to arrayOf(2, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25}],\"mode\":1}", "[{\"data\":\"com.android.chrome\",\"extras\":[],\"flags\":0,\"type\":\"APP\"}]", "[]", 1, 1, "NULL", 1),

            arrayOf(3, "[{\"keys\":[25,24]}]", 0, 1, "KEY", "24", "[]")
                to arrayOf(3, "{\"extras\":[],\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":25},{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.ANY_DEVICE\",\"keyCode\":24}],\"mode\":0}", "[{\"data\":\"24\",\"extras\":[],\"flags\":0,\"type\":\"KEY_EVENT\"}]", "[]", 1, 0, "NULL", 1)
        )
        //TODO add rest of test rows and row with multiple triggers
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
    fun migrate1to2() {

        var db = helper.createDatabase(TEST_DB, 1).apply {

            Migration_1_2_TEST_DATA.forEach { pair ->
                val row = pair.first

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

        while (cursor.moveToNext()) {
            val row = cursor.position
            val expectedColumnValues: Array<out Any> = Migration_1_2_TEST_DATA[row].second

            //id
            assertThat(cursor.getInt(0), `is`(expectedColumnValues[0] as Int))

            //trigger
            assertThat(cursor.getString(1), `is`(expectedColumnValues[1] as String))

            //action list
            assertThat(cursor.getString(2), `is`(expectedColumnValues[2] as String))

            //constraint list
            assertThat(cursor.getString(3), `is`(expectedColumnValues[3] as String))
        }
    }
}