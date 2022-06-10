package io.github.sds100.keymapper.mappings.keymaps

import android.view.KeyEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.TestDispatcherProvider
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerState
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordedKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.onboarding.FakeOnboardingUseCase
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.FakeResourceProvider
import io.github.sds100.keymapper.util.ui.PopupUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock

/**
 * Created by sds100 on 28/04/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ConfigKeyMapTriggerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope = TestScope(testDispatcher)
    private lateinit var viewModel: ConfigKeyMapTriggerViewModel
    private lateinit var mockConfigKeyMapUseCase: ConfigKeyMapUseCase
    private lateinit var mockRecordTrigger: RecordTriggerUseCase
    private lateinit var fakeOnboarding: FakeOnboardingUseCase
    private lateinit var fakeResourceProvider: FakeResourceProvider

    private lateinit var onRecordKey: MutableSharedFlow<RecordedKey>
    private lateinit var keyMap: MutableStateFlow<KeyMap>

    @Before
    fun init() {
        Dispatchers.setMain(testDispatcher)

        fakeOnboarding = FakeOnboardingUseCase()

        keyMap = MutableStateFlow(KeyMap())
        onRecordKey = MutableSharedFlow()

        mockRecordTrigger = mock {
            on { onRecordKey }.then { onRecordKey }
            on { state }.then { flow<RecordTriggerState> {} }
        }

        mockConfigKeyMapUseCase = mock {
            on { mapping }.then { keyMap.map { State.Data(it) } }
        }

        fakeResourceProvider = FakeResourceProvider()

        viewModel = ConfigKeyMapTriggerViewModel(
            coroutineScope,
            fakeOnboarding,
            mockConfigKeyMapUseCase,
            mockRecordTrigger,
            mock(),
            mock {
                on { invalidateTriggerErrors }.then { flow<Unit> { } }
                on { showDeviceDescriptors }.then { flow<Unit> { } }
            },
            fakeResourceProvider,
            TestDispatcherProvider(testDispatcher)
        )

        coroutineScope.advanceUntilIdle()
    }

    /**
     * issue #602
     */
    @Test
    fun `when create back button trigger key then prompt the user to disable screen pinning`() =
        runTest(testDispatcher) {
            //GIVEN
            fakeResourceProvider.stringResourceMap[R.string.dialog_message_screen_pinning_warning] = "bla"

            //WHEN
            onRecordKey.emit(
                RecordedKey(
                    keyCode = KeyEvent.KEYCODE_BACK,
                    device = TriggerKeyDevice.Internal
                )
            )

            //THEN
            assertThat(viewModel.showPopup.first().ui, `is`(PopupUi.Ok("bla")))
        }
}