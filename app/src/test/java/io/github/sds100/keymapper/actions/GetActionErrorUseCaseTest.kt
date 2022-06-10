package io.github.sds100.keymapper.actions

import android.view.KeyEvent
import io.github.sds100.keymapper.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.util.Error
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

/**
 * Created by sds100 on 01/05/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetActionErrorUseCaseTest {

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
            shizukuAdapter = mockShizukuAdapter
        )
    }

    /**
     * #776
     */
    @Test
    fun `dont show Shizuku errors if a compatible ime is selected`() = runTest {
        //GIVEN
        whenever(mockShizukuAdapter.isInstalled).then { MutableStateFlow(true) }
        whenever(mockInputMethodAdapter.chosenIme).then {
            MutableStateFlow(
                ImeInfo(
                    id = "ime_id",
                    packageName = "io.github.sds100.keymapper.inputmethod.latin",
                    label = "Key Mapper GUI Keyboard",
                    isEnabled = true,
                    isChosen = true
                )
            )
        }

        val action = ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)

        //WHEN
        val error = useCase.getError(action)

        //THEN
        assertThat(error, nullValue())
    }

    /**
     * #776
     */
    @Test
    fun `show Shizuku errors if a compatible ime is not selected and Shizuku is installed`() = runTest {
        //GIVEN
        whenever(mockShizukuAdapter.isInstalled).then { MutableStateFlow(true) }
        whenever(mockShizukuAdapter.isStarted).then { MutableStateFlow(false) }

        whenever(mockInputMethodAdapter.chosenIme).then {
            MutableStateFlow(
                ImeInfo(
                    id = "ime_id",
                    packageName = "io.gboard",
                    label = "Gboard",
                    isEnabled = true,
                    isChosen = true
                )
            )
        }

        val action = ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)
        //WHEN
        val error = useCase.getError(action)

        //THEN
        assertThat(error, `is`(Error.ShizukuNotStarted))
    }
}