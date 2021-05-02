package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.mappings.FakeMapping
import io.github.sds100.keymapper.onboarding.FakeOnboardingUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.util.FlowUtils.toListWithTimeout
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.onUserResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Created by sds100 on 28/04/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ConfigActionsViewModelTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)
    private lateinit var viewModel: ConfigActionsViewModel<FakeAction, FakeMapping>
    private lateinit var fakeOnboarding: FakeOnboardingUseCase

    @Before
    fun init() {
        fakeOnboarding = FakeOnboardingUseCase()
        viewModel = ConfigActionsViewModel(
            coroutineScope,
            displayActionUseCase = mock {
                on { invalidateActionErrors }.then { flow<Unit> { } }
                on { showDeviceDescriptors }.then { flow<Unit> { } }
            },
            testAction = mock(),
            config = mock {
                on { mapping }.then { flow { emit(State.Loading) } }
            },
            uiHelper = mock(),
            onboardingUseCase = fakeOnboarding,
            resourceProvider = mock()
        )
    }

    /**
     * #645
     */
    @Test
    fun `create text action then show prompt to install Key Mapper GUI Keyboard`() =
        coroutineScope.runBlockingTest {
            viewModel.addAction(TextAction("bla"))

            assertThat(viewModel.showPopup.first().ui, `is`(PopupUi.InstallGuiKeyboard))
        }

    /**
     * #645
     */
    @Test
    fun `create key event action then show prompt to install Key Mapper GUI Keyboard`() =
        coroutineScope.runBlockingTest {
            viewModel.addAction(KeyEventAction(3))

            assertThat(viewModel.showPopup.first().ui, `is`(PopupUi.InstallGuiKeyboard))
        }

    /**
     * #645
     */
    @Test
    fun `create key event action and acknowledged gui keyboard then do not show prompt to install Key Mapper GUI Keyboard`() =
        coroutineScope.runBlockingTest {
            fakeOnboarding.showGuiKeyboardPrompt.value = false

            viewModel.addAction(KeyEventAction(3))

            assertThat(viewModel.showPopup.toListWithTimeout(), empty())

            viewModel.onUserResponse("install_gui_keyboard", DialogResponse.POSITIVE)
        }
}