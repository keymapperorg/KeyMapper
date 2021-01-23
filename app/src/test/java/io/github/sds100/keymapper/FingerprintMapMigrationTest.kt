package io.github.sds100.keymapper

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import com.google.gson.Gson
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.util.FakeDataStore
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by sds100 on 22/01/21.
 */

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class FingerprintMapMigrationTest {
    companion object {
        private const val MIGRATION_0_1_TEST_DATA = "{\"action_list\":[{\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":2,\"type\":\"APP\",\"uid\":\"cd19c16a-4835-4b85-92d9-d27afe1a242f\"}],\"constraints\":[],\"constraint_mode\":1,\"extras\":[],\"flags\":1,\"enabled\":true}"
        private const val MIGRATION_0_1_EXPECTED_DATA = "{\"db_version\":1,\"action_list\":[{\"type\":\"APP\",\"data\":\"com.google.android.contacts\",\"extras\":[],\"flags\":0,\"uid\":\"cd19c16a-4835-4b85-92d9-d27afe1a242f\"}],\"constraints\":[],\"constraint_mode\":1,\"extras\":[],\"flags\":3,\"enabled\":true}"
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
        repository = FingerprintMapRepository(dataStore, coroutineScope)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun migrate0to1() = coroutineScope.runBlockingTest {

        dataStore.edit {
            it[SWIPE_DOWN_KEY] = MIGRATION_0_1_TEST_DATA
        }

        repository.fingerprintGestureMapsLiveData.observeForever {
            val json = it[SWIPE_DOWN_KEY.name]!!

            assertThat(Gson().toJson(json), `is`(MIGRATION_0_1_EXPECTED_DATA))
        }

        assertThat(dataStore.data.first()[SWIPE_DOWN_KEY], `is`(MIGRATION_0_1_EXPECTED_DATA))
    }
}