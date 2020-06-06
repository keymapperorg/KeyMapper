package io.github.sds100.keymapper.data.db.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.sds100.keymapper.data.db.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Created by sds100 on 05/06/20.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration_test"

    @Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1to2() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("")

            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)
    }
}