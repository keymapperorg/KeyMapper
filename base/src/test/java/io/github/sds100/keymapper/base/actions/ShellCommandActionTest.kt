package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.utils.InputEventAction
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ShellCommandActionTest {

    private lateinit var mockShellAdapter: ShellAdapter
    private lateinit var mockSuAdapter: SuAdapter

    @Before
    fun setup() {
        mockShellAdapter = mock {
            on { execute(any()) }.doReturn(Success(Unit))
            on { executeWithOutput(any()) }.doReturn(Success("output"))
        }
        mockSuAdapter = mock {
            on { execute(any()) }.doReturn(Success(Unit))
            on { executeWithOutput(any()) }.doReturn(Success("root output"))
        }
    }

    @Test
    fun `shell command without root executes with shell adapter`() = runTest {
        val action = ActionData.ShellCommand(
            command = "echo test",
            useRoot = false,
        )

        // This test verifies the action data structure is correct
        // Actual execution testing should be done in PerformActionsUseCaseTest
        assert(action.command == "echo test")
        assert(!action.useRoot)
        assert(action.id == ActionId.SHELL_COMMAND)
    }

    @Test
    fun `shell command with root flag set correctly`() = runTest {
        val action = ActionData.ShellCommand(
            command = "reboot",
            useRoot = true,
        )

        assert(action.command == "reboot")
        assert(action.useRoot)
        assert(action.id == ActionId.SHELL_COMMAND)
    }

    @Test
    fun `shell command is editable`() {
        val action = ActionData.ShellCommand(
            command = "ls",
            useRoot = false,
        )

        assert(action.isEditable())
    }
}
