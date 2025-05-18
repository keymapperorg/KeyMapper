package io.github.sds100.keymapper.actions

import android.view.KeyEvent
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.base.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class GetActionFailedUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var useCase: GetActionErrorUseCaseImpl

    private lateinit var mockShizukuAdapter: ShizukuAdapter
    private lateinit var mockInputMethodAdapter: InputMethodAdapter
    private lateinit var mockPermissionAdapter: PermissionAdapter

    @Before
    fun init() {
        mockShizukuAdapter = mock()
        mockInputMethodAdapter = mock()
        mockPermissionAdapter = mock()

        useCase = GetActionErrorUseCaseImpl(
            packageManager = mock(),
            inputMethodAdapter = mockInputMethodAdapter,
            permissionAdapter = mockPermissionAdapter,
            systemFeatureAdapter = mock(),
            cameraAdapter = mock(),
            soundsManager = mock(),
            shizukuAdapter = mockShizukuAdapter,
            ringtoneAdapter = mock(),
        )
    }

    /**
     * #776
     */
    @Test
    fun `don't show Shizuku errors if a compatible ime is selected`() = testScope.runTest {
        // GIVEN
        whenever(mockShizukuAdapter.isInstalled).then { MutableStateFlow(true) }
        whenever(mockInputMethodAdapter.chosenIme).then {
            MutableStateFlow(
                ImeInfo(
                    id = "ime_id",
                    packageName = "io.github.sds100.keymapper.inputmethod.latin",
                    label = "Key Mapper GUI Keyboard",
                    isEnabled = true,
                    isChosen = true,
                ),
            )
        }

        val action = ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)

        // WHEN
        val error = useCase.actionErrorSnapshot.first().getError(action)

        // THEN
        assertThat(error, nullValue())
    }

    /**
     * #776
     */
    @Test
    fun `show Shizuku errors if a compatible ime is not selected and Shizuku is installed`() = testScope.runTest {
        // GIVEN
        whenever(mockShizukuAdapter.isInstalled).then { MutableStateFlow(true) }
        whenever(mockShizukuAdapter.isStarted).then { MutableStateFlow(false) }

        whenever(mockInputMethodAdapter.chosenIme).then {
            MutableStateFlow(
                ImeInfo(
                    id = "ime_id",
                    packageName = "io.gboard",
                    label = "Gboard",
                    isEnabled = true,
                    isChosen = true,
                ),
            )
        }

        val action = ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)
        // WHEN
        val error = useCase.actionErrorSnapshot.first().getError(action)

        // THEN
        assertThat(error, `is`(Error.ShizukuNotStarted))
    }
}
