import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.db.migration.JsonMigration
import io.github.sds100.keymapper.data.db.migration.keymaps.Migration_9_10
import util.JsonTestUtils
import io.github.sds100.keymapper.util.MigrationUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test

/**
 * Created by sds100 on 22/01/21.
 */

@ExperimentalCoroutinesApi
class KeymapJsonMigrationTest {
    companion object {
        private val MIGRATION_0_1_TEST_DATA = arrayOf(
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":2,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":1,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}",
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":6,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":0,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}"
        )
        private val MIGRATION_0_1_EXPECTED_DATA = arrayOf(
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":0,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":17,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}",
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":4,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":16,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}"
        )

        private val MIGRATIONS = listOf(
            JsonMigration(9, 10) { gson, json -> Migration_9_10.migrateJson(gson, json) }
        )
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    @Test
    fun `migrate 9 to 10`() {
        test(MIGRATION_0_1_TEST_DATA, MIGRATION_0_1_EXPECTED_DATA, 9, 10)
    }

    private fun test(
        testData: Array<String>,
        expectedData: Array<String>,
        inputVersion: Int,
        outputVersion: Int
    ) = coroutineScope.runBlockingTest {
        val gson = Gson()
        val parser = JsonParser()

        testData.forEachIndexed { index, json ->
            val migratedJson = MigrationUtils.migrate(
                gson,
                MIGRATIONS,
                inputVersion,
                json,
                outputVersion
            )

            val expectedElement = parser.parse(expectedData[index])
            val migratedElement = parser.parse(migratedJson)

            compare(expectedElement, "expected", migratedElement, "migrated")
        }
    }

    private fun compare(element: JsonElement, elementName: String, other: JsonElement, otherName: String) {
        JsonTestUtils.compare("", element, elementName, other, otherName)
        JsonTestUtils.compare("", other, elementName, element, elementName)
    }
}