package io.github.sds100.keymapper.onboarding

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.util.VersionUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Created by sds100 on 25/04/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class OnboardingUseCaseTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private lateinit var useCase: OnboardingUseCaseImpl
    private lateinit var fakePreferences: FakePreferenceRepository

    @Before
    fun init() {
       fakePreferences = FakePreferenceRepository()
        useCase = OnboardingUseCaseImpl(fakePreferences,mock())
    }

    @Test
    fun `update to 2_3_0, no bluetooth devices were chosen in settings, do not show notification to choose devices again`() =
        coroutineScope.runBlockingTest {
            //GIVEN
            fakePreferences.set(stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker"), emptySet())
            fakePreferences.set(stringSetPreferencesKey("pref_bluetooth_devices"), emptySet())
            fakePreferences.set(Keys.approvedSetupChosenDevicesAgain, false)
            fakePreferences.set(Keys.lastInstalledVersionCodeBackground, VersionUtils.VERSION_2_3_0 - 1)
            //WHEN

            //THEN
            assertThat(useCase.showSetupChosenDevicesAgainNotification.first(), `is`(false))
        }

    @Test
    fun `update to 2_3_0, bluetooth devices were chosen in settings, show notification to choose devices again`() =
        coroutineScope.runBlockingTest {
            //GIVEN
            fakePreferences.set(stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker"), setOf("devices"))
            fakePreferences.set(stringSetPreferencesKey("pref_bluetooth_devices"), setOf("devices"))
            fakePreferences.set(Keys.approvedSetupChosenDevicesAgain, false)
            fakePreferences.set(Keys.lastInstalledVersionCodeBackground, VersionUtils.VERSION_2_3_0 - 1)
            //WHEN

            //THEN
            assertThat(useCase.showSetupChosenDevicesAgainNotification.first(), `is`(true))
        }
}