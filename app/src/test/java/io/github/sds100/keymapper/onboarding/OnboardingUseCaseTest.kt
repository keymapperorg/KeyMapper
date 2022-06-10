package io.github.sds100.keymapper.onboarding

import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.util.VersionHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock

/**
 * Created by sds100 on 25/04/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class OnboardingUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope =
        createTestCoroutineScope(TestCoroutineDispatcher() + TestCoroutineExceptionHandler() + testDispatcher)

    private lateinit var useCase: OnboardingUseCaseImpl
    private lateinit var fakePreferences: FakePreferenceRepository

    @Before
    fun init() {
        fakePreferences = FakePreferenceRepository()
        useCase = OnboardingUseCaseImpl(
            fakePreferences,
            mock(),
            leanbackAdapter = mock(),
            shizukuAdapter = mock(),
            permissionAdapter = mock(),
            packageManagerAdapter = mock()
        )
    }

    /**
     * #709
     */
    @Test
    fun `Only show fingerprint map feature notification for the first update only`() =
        runTest {
            //show it when updating from a version that didn't support it to a version that does
            //GIVEN
            fakePreferences.set(Keys.approvedFingerprintFeaturePrompt, false)
            fakePreferences.set(Keys.fingerprintGesturesAvailable, true)
            fakePreferences.set(Keys.shownAppIntro, true)

            //WHEN
            fakePreferences.set(
                Keys.lastInstalledVersionCodeHomeScreen,
                VersionHelper.FINGERPRINT_GESTURES_MIN_VERSION - 1
            )
            advanceUntilIdle()

            //THEN
            assertThat(useCase.showFingerprintFeatureNotificationIfAvailable.first(), `is`(true))

            //Don't show it when updating from a version that supports it.
            //GIVEN
            fakePreferences.set(Keys.approvedFingerprintFeaturePrompt, true)
            fakePreferences.set(Keys.fingerprintGesturesAvailable, true)

            //WHEN
            fakePreferences.set(
                Keys.lastInstalledVersionCodeHomeScreen,
                VersionHelper.FINGERPRINT_GESTURES_MIN_VERSION
            )
            advanceUntilIdle()

            //THEN
            assertThat(useCase.showFingerprintFeatureNotificationIfAvailable.first(), `is`(false))

            //Don't show it when opening the app for the first time.
            //GIVEN
            fakePreferences.set(Keys.approvedFingerprintFeaturePrompt, null)
            fakePreferences.set(Keys.fingerprintGesturesAvailable, true)
            fakePreferences.set(Keys.lastInstalledVersionCodeHomeScreen, null)
            fakePreferences.set(Keys.shownAppIntro, null)

            //WHEN
            advanceUntilIdle()

            //THEN
            assertThat(useCase.showFingerprintFeatureNotificationIfAvailable.first(), `is`(false))
        }

    @Test
    fun `update to 2_3_0, no bluetooth devices were chosen in settings, do not show notification to choose devices again`() =
        runTest {
            //GIVEN
            fakePreferences.set(
                stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker"),
                emptySet()
            )
            fakePreferences.set(stringSetPreferencesKey("pref_bluetooth_devices"), emptySet())
            fakePreferences.set(Keys.approvedSetupChosenDevicesAgain, false)
            fakePreferences.set(
                Keys.lastInstalledVersionCodeBackground,
                VersionHelper.VERSION_2_3_0 - 1
            )
            //WHEN

            //THEN
            assertThat(useCase.showSetupChosenDevicesAgainNotification.first(), `is`(false))
        }

    @Test
    fun `update to 2_3_0, bluetooth devices were chosen in settings, show notification to choose devices again`() =
        runTest {
            //GIVEN
            fakePreferences.set(
                stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker"),
                setOf("devices")
            )
            fakePreferences.set(stringSetPreferencesKey("pref_bluetooth_devices"), setOf("devices"))
            fakePreferences.set(Keys.approvedSetupChosenDevicesAgain, false)
            fakePreferences.set(
                Keys.lastInstalledVersionCodeBackground,
                VersionHelper.VERSION_2_3_0 - 1
            )
            //WHEN

            //THEN
            assertThat(useCase.showSetupChosenDevicesAgainNotification.first(), `is`(true))
        }
}