package io.github.sds100.keymapper.base.actions

import android.view.KeyEvent
import io.github.sds100.keymapper.base.system.inputmethod.FakeInputMethodAdapter
import io.github.sds100.keymapper.base.utils.TestBuildConfigProvider
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetActionErrorUseCaseTest {

    companion object {
        private val GUI_KEYBOARD_IME_INFO = ImeInfo(
            id = "ime_id",
            packageName = "io.github.sds100.keymapper.inputmethod.latin",
            label = "Key Mapper GUI Keyboard",
            isEnabled = true,
            isChosen = true,
        )

        private val GBOARD_IME_INFO = ImeInfo(
            id = "gboard_id",
            packageName = "com.google.android.inputmethod.latin",
            label = "Gboard",
            isEnabled = true,
            isChosen = false,
        )
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var useCase: GetActionErrorUseCaseImpl

    private lateinit var fakeInputMethodAdapter: FakeInputMethodAdapter
    private lateinit var mockPermissionAdapter: PermissionAdapter

    @Before
    fun init() {
        fakeInputMethodAdapter = FakeInputMethodAdapter()
        mockPermissionAdapter = mock()

        useCase = GetActionErrorUseCaseImpl(
            packageManagerAdapter = mock(),
            inputMethodAdapter = fakeInputMethodAdapter,
            permissionAdapter = mockPermissionAdapter,
            systemFeatureAdapter = mock(),
            cameraAdapter = mock(),
            soundsManager = mock(),
            ringtoneAdapter = mock(),
            buildConfigProvider = TestBuildConfigProvider(),
            systemBridgeConnectionManager = mock(),
            preferenceRepository = mock(),
        )
    }

    private fun setupKeyEventActionTest(chosenIme: ImeInfo) {
        whenever(mockPermissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)).then { true }
        fakeInputMethodAdapter.chosenIme.value = chosenIme
        fakeInputMethodAdapter.inputMethods.value = listOf(GBOARD_IME_INFO, GUI_KEYBOARD_IME_INFO)
    }

    /**
     * #797, #1719 Allow key maps to have actions that will "fix" subsequent actions in the sequence.
     */
    @Test
    fun `do not show error for key event action if the previous action selects the Key Mapper keyboard`() =
        testScope.runTest {
            setupKeyEventActionTest(chosenIme = GBOARD_IME_INFO)

            val actions = listOf(
                ActionData.SwitchKeyboard(
                    imeId = GUI_KEYBOARD_IME_INFO.id,
                    GUI_KEYBOARD_IME_INFO.label,
                ),
                ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN),
            )

            val errors = useCase.actionErrorSnapshot.first().getErrors(actions).values.toList()

            assertThat(errors[0], nullValue())
            assertThat(errors[1], nullValue())
        }

    @Test
    fun `do not show error for text actions if the previous action selects the Key Mapper keyboard`() =
        testScope.runTest {
            setupKeyEventActionTest(chosenIme = GBOARD_IME_INFO)

            val actions = listOf(
                ActionData.SwitchKeyboard(
                    imeId = GUI_KEYBOARD_IME_INFO.id,
                    GUI_KEYBOARD_IME_INFO.label,
                ),
                ActionData.Text("hello"),
            )

            val errors = useCase.actionErrorSnapshot.first().getErrors(actions).values.toList()

            assertThat(errors[0], nullValue())
            assertThat(errors[1], nullValue())
        }

    @Test
    fun `do not show error for key event actions if an action not immediately before selects the Key Mapper keyboard`() =
        testScope.runTest {
            setupKeyEventActionTest(chosenIme = GBOARD_IME_INFO)

            val actions = listOf(
                ActionData.SwitchKeyboard(
                    imeId = GUI_KEYBOARD_IME_INFO.id,
                    GUI_KEYBOARD_IME_INFO.label,
                ),
                ActionData.OpenCamera,
                ActionData.OpenSettings,
                ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN),
            )

            val errors = useCase.actionErrorSnapshot.first().getErrors(actions).values.toList()

            assertThat(errors[0], nullValue())
            assertThat(errors[1], nullValue())
            assertThat(errors[2], nullValue())
            assertThat(errors[3], nullValue())
        }

    @Test
    fun `do not show an error for a key event action if a key mapper keyboard is selected`() =
        testScope.runTest {
            setupKeyEventActionTest(chosenIme = GUI_KEYBOARD_IME_INFO)

            val action = ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)

            val errors = useCase.actionErrorSnapshot.first().getErrors(listOf(action))
            assertThat(errors[action], nullValue())
        }

    @Test
    fun `show an error for a key event action if a key mapper keyboard is not selected`() =
        testScope.runTest {
            setupKeyEventActionTest(chosenIme = GBOARD_IME_INFO)

            val action = ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)

            val errors = useCase.actionErrorSnapshot.first().getErrors(listOf(action))
            assertThat(errors[action], `is`(KMError.NoCompatibleImeChosen))
        }

    @Test
    fun `show an error for a key event action in a list if a key mapper keyboard is not selected`() =
        testScope.runTest {
            setupKeyEventActionTest(chosenIme = GBOARD_IME_INFO)

            val actions = listOf(
                ActionData.OpenCamera,
                ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN),
            )

            val errors = useCase.actionErrorSnapshot.first().getErrors(actions).values.toList()

            assertThat(errors[0], nullValue())
            assertThat(errors[1], `is`(KMError.NoCompatibleImeChosen))
        }

    @Test
    fun `show an error for a key event action if a previous action selects a non-key mapper keyboard`() =
        testScope.runTest {
            setupKeyEventActionTest(chosenIme = GBOARD_IME_INFO)

            val actions = listOf(
                ActionData.SwitchKeyboard(imeId = GBOARD_IME_INFO.id, GBOARD_IME_INFO.label),
                ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN),
            )

            val errors = useCase.actionErrorSnapshot.first().getErrors(actions).values.toList()

            assertThat(errors[0], nullValue())
            assertThat(errors[1], `is`(KMError.NoCompatibleImeChosen))
        }

    @Test
    fun `show an error for a key event action if a previous action selects a non-key mapper keyboard after selecting a key mapper keyboard`() =
        testScope.runTest {
            setupKeyEventActionTest(chosenIme = GBOARD_IME_INFO)

            val actions = listOf(
                ActionData.SwitchKeyboard(
                    imeId = GUI_KEYBOARD_IME_INFO.id,
                    GUI_KEYBOARD_IME_INFO.label,
                ),
                ActionData.SwitchKeyboard(imeId = GBOARD_IME_INFO.id, GBOARD_IME_INFO.label),
                ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN),
            )

            val errors = useCase.actionErrorSnapshot.first().getErrors(actions).values.toList()

            assertThat(errors[0], nullValue())
            assertThat(errors[1], nullValue())
            assertThat(errors[2], `is`(KMError.NoCompatibleImeChosen))
        }
}
