package io.github.sds100.keymapper

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.migration.JsonMigration
import io.github.sds100.keymapper.data.migration.Migration10To11
import io.github.sds100.keymapper.data.migration.Migration11To12
import io.github.sds100.keymapper.data.migration.Migration9To10
import io.github.sds100.keymapper.data.migration.MigrationUtils
import io.github.sds100.keymapper.base.util.JsonTestUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.InputStream

@ExperimentalCoroutinesApi
class KeyMapJsonMigrationTest {
    companion object {
        private val MIGRATION_9_10_TEST_DATA = listOf(
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":2,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":1,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}",
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":6,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":0,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}",
        )
        private val MIGRATION_9_10_EXPECTED_DATA = listOf(
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":0,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":17,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}",
            "{\"actionList\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":4,\"type\":\"APP\"}],\"constraintList\":[],\"constraintMode\":1,\"flags\":0,\"id\":0,\"isEnabled\":true,\"trigger\":{\"extras\":[],\"flags\":16,\"keys\":[{\"clickType\":0,\"deviceId\":\"io.github.sds100.keymapper.THIS_DEVICE\",\"flags\":0,\"keyCode\":25}],\"mode\":2}}",
        )
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var parser: JsonParser
    private lateinit var gson: Gson

    @Before
    fun init() {
        parser = JsonParser()
        gson = Gson()
    }

    @Test
    fun `migrate 11 to 12`() {
        val testData = getKeymapListJsonFromFile("migration-11-12-test-data.json")
        val expectedData = getKeymapListJsonFromFile("migration-11-12-expected-data.json")

        val deviceInfoList = getDeviceInfoListJsonFromFile("migration-11-12-test-data.json")

        testData.forEachIndexed { index, keyMap ->
            val migratedKeyMap = Migration11To12.migrateKeyMap(keyMap.asJsonObject, deviceInfoList)
            JsonTestUtils.compareBothWays(
                expectedData[index],
                "expected",
                migratedKeyMap,
                "migrated",
            )
        }
    }

    @Test
    fun `migrate 10 to 11`() {
        test(
            getKeymapListJsonFromFile("migration-10-11-test-data.json"),
            getKeymapListJsonFromFile("migration-10-11-expected-data.json"),
            10,
            11,
        )
    }

    @Test
    fun `migrate 9 to 11`() {
        test(
            getKeymapListJsonFromFile("migration-9-11-expected-data.json"),
            getKeymapListJsonFromFile("migration-9-11-expected-data.json"),
            9,
            11,
        )
    }

    @Test
    fun `migrate 9 to 10`() {
        val testData = MIGRATION_9_10_TEST_DATA.map {
            parser.parse(it)
        }.toJsonArray()

        val expectedData = MIGRATION_9_10_EXPECTED_DATA.map {
            parser.parse(it)
        }.toJsonArray()

        test(testData, expectedData, 9, 10)
    }

    private fun test(
        testData: JsonArray,
        expectedData: JsonArray,
        inputVersion: Int,
        outputVersion: Int,
    ) = runTest(testDispatcher) {
        val migrations = listOf(
            JsonMigration(9, 10) { json -> Migration9To10.migrateJson(json) },
            JsonMigration(10, 11) { json -> Migration10To11.migrateJson(json) },
        )

        testData.forEachIndexed { index, testKeyMap ->

            val migratedKeyMap = MigrationUtils.migrate(
                migrations,
                inputVersion,
                testKeyMap.asJsonObject,
                outputVersion,
            )

            val expectedKeyMap = expectedData[index]

            JsonTestUtils.compareBothWays(expectedKeyMap, "expected", migratedKeyMap, "migrated")
        }
    }

    private fun getKeymapListJsonFromFile(fileName: String): JsonArray {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return rootElement["keymap_list"].asJsonArray
    }

    private fun getDeviceInfoListJsonFromFile(fileName: String): JsonArray {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return rootElement["device_info"].asJsonArray
    }

    private fun getJson(fileName: String): InputStream =
        this.javaClass.classLoader!!.getResourceAsStream("json-migration-test/$fileName")
}
