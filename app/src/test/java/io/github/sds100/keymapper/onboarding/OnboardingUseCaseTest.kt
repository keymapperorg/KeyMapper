package io.github.sds100.keymapper.onboarding

import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.system.apps.PackageInfo
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.VersionUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
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
    private lateinit var mockPackageManager: PackageManagerAdapter

    @Before
    fun init() {
        fakePreferences = FakePreferenceRepository()
        mockPackageManager = mock()
        useCase = OnboardingUseCaseImpl(fakePreferences, mockPackageManager, mock())
    }

    @Test
    fun `if gui keyboard is installed then never show gui keyboard prompts in the future`() =
        coroutineScope.runBlockingTest {

            val guiKeyboardPackageInfo = PackageInfo(
                packageName = KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE,
                canBeLaunched = false,
                activities = emptyList()
            )

            whenever(mockPackageManager.installedPackages).then {
                MutableStateFlow(State.Data(listOf(guiKeyboardPackageInfo)))
            }

            useCase.showGuiKeyboardPrompt.launchIn(coroutineScope)

            assertThat(fakePreferences.get(Keys.acknowledgedGuiKeyboard).first(), `is`(true))
            assertThat(useCase.showGuiKeyboardPrompt.first(), `is`(false))
        }

    @Test
    fun `update to 2_3_0, no bluetooth devices were chosen in settings, do not show notification to choose devices again`() =
        coroutineScope.runBlockingTest {
            //GIVEN
            fakePreferences.set(
                stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker"),
                emptySet()
            )
            fakePreferences.set(stringSetPreferencesKey("pref_bluetooth_devices"), emptySet())
            fakePreferences.set(Keys.approvedSetupChosenDevicesAgain, false)
            fakePreferences.set(
                Keys.lastInstalledVersionCodeBackground,
                VersionUtils.VERSION_2_3_0 - 1
            )
            //WHEN

            //THEN
            assertThat(useCase.showSetupChosenDevicesAgainNotification.first(), `is`(false))
        }

    @Test
    fun `update to 2_3_0, bluetooth devices were chosen in settings, show notification to choose devices again`() =
        coroutineScope.runBlockingTest {
            //GIVEN
            fakePreferences.set(
                stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker"),
                setOf("devices")
            )
            fakePreferences.set(stringSetPreferencesKey("pref_bluetooth_devices"), setOf("devices"))
            fakePreferences.set(Keys.approvedSetupChosenDevicesAgain, false)
            fakePreferences.set(
                Keys.lastInstalledVersionCodeBackground,
                VersionUtils.VERSION_2_3_0 - 1
            )
            //WHEN

            //THEN
            assertThat(useCase.showSetupChosenDevicesAgainNotification.first(), `is`(true))
        }
}