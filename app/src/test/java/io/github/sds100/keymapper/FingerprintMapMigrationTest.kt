package io.github.sds100.keymapper

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.repository.DefaultFingerprintMapRepository
import io.github.sds100.keymapper.util.JsonTestUtils
import io.github.sds100.keymapper.util.MigrationUtils
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
class FingerprintMapMigrationTest {
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
    fun `migrate 1 to 2`() {
        test(
            arrayOf(getSwipeDownJsonFromFile("migration-10-11-test-data.json")),
            arrayOf(getSwipeDownJsonFromFile("migration-10-11-expected-data.json")),
            1, 2
        )
    }

    @Test
    fun `migrate 0 to 1`() {
        test(
            MIGRATION_0_1_TEST_DATA, MIGRATION_0_1_EXPECTED_DATA,
            0, 1
        )
    }

    private fun getSwipeDownJsonFromFile(fileName: String): String {
        val jsonInputStream = getJson(fileName)
        val json = jsonInputStream.bufferedReader().use { it.readText() }

        val rootElement = parser.parse(json)

        return rootElement["fingerprint_swipe_down"].asJsonObject.let { gson.toJson(it) }
    }

    private fun getJson(fileName: String): InputStream {
        return this.javaClass.classLoader!!.getResourceAsStream("json-migration-test/$fileName")
    }

    private fun test(
        testData: Array<String>,
        expectedData: Array<String>,
        inputVersion: Int,
        outputVersion: Int
    ) = coroutineScope.runBlockingTest {
        testData.forEachIndexed { index, json ->
            val migratedJson = MigrationUtils.migrate(
                gson,
                DefaultFingerprintMapRepository.MIGRATIONS,
                inputVersion,
                json,
                outputVersion
            )

            val expectedElement = parser.parse(expectedData[index])
            val migratedElement = parser.parse(migratedJson)

            JsonTestUtils.compareBothWays(expectedElement, "expected", migratedElement, "migrated")
        }
    }
}