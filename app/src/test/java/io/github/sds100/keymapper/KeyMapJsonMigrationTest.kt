package io.github.sds100.keymapper

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.repositories.RoomKeyMapRepository
import io.github.sds100.keymapper.util.JsonTestUtils
import io.github.sds100.keymapper.data.migration.MigrationUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.InputStream

/**
 * Created by sds100 on 22/01/21.
 */

@ExperimentalCoroutinesApi
class KeymapJsonMigrationTest {
    companion object {
        private val MIGRATION_9_10_TEST_DATA = listOf(
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":2,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":1,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}",
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":6,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":0,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}"
        )
        private val MIGRATION_9_10_EXPECTED_DATA = listOf(
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":0,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":17,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}",
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":4,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":16,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}"
        )
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private lateinit var parser: JsonParser
    private lateinit var gson: Gson

    @Before
    fun init() {
        parser = JsonParser()
        gson = Gson()
    }

    @Test
    fun `migrate 10 to 11`() {
        test(
            getKeymapListJsonFromFile("migration-10-11-test-data.json"),
            getKeymapListJsonFromFile("migration-10-11-expected-data.json"),
            10, 11
        )
    }

    @Test
    fun `migrate 9 to 11`() {
        test(
            getKeymapListJsonFromFile("migration-9-11-expected-data.json"),
            getKeymapListJsonFromFile("migration-9-11-expected-data.json"),
            9,
            11
        )
    }

    @Test
    fun `migrate 9 to 10`() {
        test(MIGRATION_9_10_TEST_DATA, MIGRATION_9_10_EXPECTED_DATA, 9, 10)
    }

    private fun test(
        testData: List<String>,
        expectedData: List<String>,
        inputVersion: Int,
        outputVersion: Int
    ) = coroutineScope.runBlockingTest {
        testData.forEachIndexed { index, json ->
            val migratedJson = MigrationUtils.migrate(
                gson,
                RoomKeyMapRepository.MIGRATIONS,
                inputVersion,
                json,
                outputVersion
            )

            val expectedElement = parser.parse(expectedData[index])
            val migratedElement = parser.parse(migratedJson)

            JsonTestUtils.compareBothWays(expectedElement, "expected", migratedElement, "migrated")
        }
    }

    private fun getKeymapListJsonFromFile(fileName: String): List<String> {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return rootElement["keymap_list"].asJsonArray.map { gson.toJson(it) }
    }

    private fun getJson(fileName: String): InputStream {
        return this.javaClass.classLoader!!.getResourceAsStream("json-migration-test/$fileName")
    }
}