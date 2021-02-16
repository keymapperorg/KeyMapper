package io.github.sds100.keymapper

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.repository.DefaultFingerprintMapRepository
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.util.FakeDataStore
import io.github.sds100.keymapper.util.JsonTestUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
        private val SWIPE_DOWN_KEY = preferencesKey<String>("swipe_down")
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)
    private lateinit var repository: FingerprintMapRepository
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun init() {
        Dispatchers.setMain(testDispatcher)
        dataStore = FakeDataStore()
        repository = DefaultFingerprintMapRepository(dataStore, coroutineScope)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `migrate 0 to 1 after update`() {
        testUpdate(MIGRATION_0_1_TEST_DATA, MIGRATION_0_1_EXPECTED_DATA)
    }

    @Test
    fun `migrate 0 to 1 after restore`() {
        testRestore(MIGRATION_0_1_TEST_DATA, MIGRATION_0_1_EXPECTED_DATA)
    }

    private fun testUpdate(testData: Array<String>, expectedData: Array<String>) {
        coroutineScope.runBlockingTest {
            testData.forEachIndexed { index, data ->
                println("test update at index $index")
                dataStore.edit {
                    it[SWIPE_DOWN_KEY] = data
                }

                //must come before getting value from live data test wrapper
                advanceUntilIdle()

                testOutput(expectedData[index])
            }
        }
    }

    private fun testRestore(testData: Array<String>, expectedData: Array<String>) {
        coroutineScope.runBlockingTest {

            testData.forEachIndexed { index, data ->
                println("test restore at index $index")

                dataStore.edit {
                    it[SWIPE_DOWN_KEY] = data
                }
                repository.restore(SWIPE_DOWN_KEY.name, data)

                //must come before getting value from live data test wrapper
                advanceUntilIdle()

                testOutput(expectedData[index])
            }
        }
    }

    private suspend fun testOutput(expectedData: String) {
        val flowJson = repository.fingerprintGestureMaps
            .mapLatest {
                val map = it[SWIPE_DOWN_KEY.name]!!

                Gson().toJson(map)
            }.first()

        val dataStoreJson = dataStore.data.first()[SWIPE_DOWN_KEY]

        val jsonParser = JsonParser()
        val flowRootObject = jsonParser.parse(flowJson).asJsonObject
        val dataStoreRootObject = jsonParser.parse(dataStoreJson).asJsonObject
        val expectedDataRootObject = jsonParser.parse(expectedData).asJsonObject

//        println("data-store json: $dataStoreJson")
        JsonTestUtils.compareBothWays(
            expectedDataRootObject,
            "expected",
            dataStoreRootObject,
            "data-store"
        )

//        println("flow json: $flowJson")
        JsonTestUtils.compareBothWays(expectedDataRootObject, "expected", flowRootObject, "flow")
    }

}