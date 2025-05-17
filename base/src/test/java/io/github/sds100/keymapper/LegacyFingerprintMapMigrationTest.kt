package io.github.sds100.keymapper

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.migration.JsonMigration
import io.github.sds100.keymapper.data.migration.MigrationUtils
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration0To1
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration1To2
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
class LegacyFingerprintMapMigrationTest {
    companion object {
        private val MIGRATION_0_1_TEST_DATA = arrayOf(
            "{\"action_list\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":2,\"type\":\"APP\",\"uid\":\"cd19c16a-4835-4b85-92d9-d27afe1a242f\"}],\"constraints\":[],\"constraint_mode\":1,\"extras\":[],\"flags\":1,\"enabled\":true}",
            "{\"action_list\":[],\"constraints\":[],\"constraint_mode\":1,\"extras\":[],\"flags\":0,\"enabled\":true}",
        )
        private val MIGRATION_0_1_EXPECTED_DATA = arrayOf(
            "{\"db_version\":1,\"action_list\":[{\"type\":\"APP\",\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":0,\"uid\":\"cd19c16a-4835-4b85-92d9-d27afe1a242f\"}],\"constraints\":[],\"constraint_mode\":1,\"extras\":[],\"flags\":3,\"enabled\":true}",
            "{\"db_version\":1,\"action_list\":[],\"constraints\":[],\"constraint_mode\":1,\"extras\":[],\"flags\":0,\"enabled\":true}",
        )

        private val SWIPE_DOWN_KEY = stringPreferencesKey("swipe_down")
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
    fun `migrate 1 to 2`() {
        test(
            listOf(getLegacySwipeDownJsonFromFile("migration-10-11-test-data.json")).toJsonArray(),
            listOf(getLegacySwipeDownJsonFromFile("migration-10-11-expected-data.json")).toJsonArray(),
            1,
            2,
        )
    }

    @Test
    fun `migrate 0 to 1`() {
        val testData = MIGRATION_0_1_TEST_DATA.map {
            parser.parse(it)
        }.toJsonArray()

        val expectedData = MIGRATION_0_1_EXPECTED_DATA.map {
            parser.parse(it)
        }.toJsonArray()

        test(testData, expectedData, 0, 1)
    }

    private fun getLegacySwipeDownJsonFromFile(fileName: String): JsonObject {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return rootElement["fingerprint_swipe_down"].asJsonObject
    }

    private fun getJson(fileName: String): InputStream =
        this.javaClass.classLoader!!.getResourceAsStream("json-migration-test/$fileName")

    private fun test(
        testData: JsonArray,
        expectedData: JsonArray,
        inputVersion: Int,
        outputVersion: Int,
    ) = runTest(testDispatcher) {
        val migrations = listOf(
            JsonMigration(0, 1) { json -> FingerprintMapMigration0To1.migrate(json) },
            JsonMigration(1, 2) { json -> FingerprintMapMigration1To2.migrate(json) },
        )
        testData.forEachIndexed { index, fingerprintMap ->

            val migratedFingerprintMap = MigrationUtils.migrate(
                migrations,
                inputVersion,
                fingerprintMap.asJsonObject,
                outputVersion,
            )

            val expectedElement = expectedData[index]

            JsonTestUtils.compareBothWays(expectedElement, "expected", migratedFingerprintMap, "migrated")
        }
    }
}
