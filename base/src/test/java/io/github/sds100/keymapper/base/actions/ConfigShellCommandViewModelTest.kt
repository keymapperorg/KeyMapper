package io.github.sds100.keymapper.base.actions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.FakeResourceProvider
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Suppress("UnusedFlow")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ConfigShellCommandViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: ConfigShellCommandViewModel
    private lateinit var mockExecuteShellCommandUseCase: ExecuteShellCommandUseCase
    private lateinit var mockNavigationProvider: NavigationProvider
    private lateinit var mockSystemBridgeConnectionManager: SystemBridgeConnectionManager
    private lateinit var fakePreferenceRepository: FakePreferenceRepository
    private lateinit var fakeResourceProvider: FakeResourceProvider

    private val commandEmptyErrorString = "Command cannot be empty"
    private val descriptionEmptyErrorString = "Description cannot be empty"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeResourceProvider = FakeResourceProvider().apply {
            stringResourceMap[R.string.action_shell_command_command_empty_error] =
                commandEmptyErrorString
            stringResourceMap[R.string.error_cant_be_empty] = descriptionEmptyErrorString
        }

        mockExecuteShellCommandUseCase = mock()
        mockNavigationProvider = mock()
        mockSystemBridgeConnectionManager = mock {
            on { connectionState }.thenReturn(
                MutableStateFlow(
                    SystemBridgeConnectionState.Connected(
                        0L,
                    ),
                ),
            )
        }

        fakePreferenceRepository = FakePreferenceRepository()

        viewModel = ConfigShellCommandViewModel(
            executeShellCommandUseCase = mockExecuteShellCommandUseCase,
            navigationProvider = mockNavigationProvider,
            systemBridgeConnectionManager = mockSystemBridgeConnectionManager,
            preferenceRepository = fakePreferenceRepository,
            resourceProvider = fakeResourceProvider,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when clicking done show error when command is blank`() = runTest {
        viewModel.onCommandChanged("")

        val result = viewModel.onDoneClick()

        assertThat(result, `is`(false))
        assertThat(viewModel.state.commandError, `is`(commandEmptyErrorString))
        assertThat(viewModel.state.descriptionError, `is`(nullValue()))
        verify(mockNavigationProvider, never()).popBackStackWithResult(any())
    }

    @Test
    fun `when clicking done show error when description is blank`() = runTest {
        viewModel.onCommandChanged("echo test")
        viewModel.onDescriptionChanged("")

        val result = viewModel.onDoneClick()

        assertThat(result, `is`(false))
        assertThat(viewModel.state.descriptionError, `is`(descriptionEmptyErrorString))
        assertThat(viewModel.state.commandError, `is`(nullValue()))
        verify(mockNavigationProvider, never()).popBackStackWithResult(any())
    }

    @Test
    fun `when clicking done show error when both command and description are blank`() = runTest {
        viewModel.onCommandChanged("")
        viewModel.onDescriptionChanged("")

        val result = viewModel.onDoneClick()

        assertThat(result, `is`(false))
        assertThat(viewModel.state.commandError, `is`(commandEmptyErrorString))
        assertThat(viewModel.state.descriptionError, `is`(nullValue()))
        verify(mockNavigationProvider, never()).popBackStackWithResult(any())
    }

    @Test
    fun `when clicking done with valid command and description navigate with result`() = runTest {
        viewModel.onCommandChanged("echo test")
        viewModel.onDescriptionChanged("Test command")
        viewModel.onTimeoutChanged(5)

        val result = viewModel.onDoneClick()

        assertThat(result, `is`(true))
        assertThat(viewModel.state.commandError, `is`(nullValue()))
        assertThat(viewModel.state.descriptionError, `is`(nullValue()))
        verify(mockNavigationProvider).popBackStackWithResult(any())
    }

    @Test
    fun `when clicking test show error when command is blank`() = runTest {
        viewModel.onCommandChanged("")

        val result = viewModel.onTestClick()

        assertThat(result, `is`(false))
        assertThat(viewModel.state.commandError, `is`(commandEmptyErrorString))
        assertThat(viewModel.state.isRunning, `is`(false))
        verify(
            mockExecuteShellCommandUseCase,
            never(),
        ).executeWithStreamingOutput(any(), any(), any())
    }

    @Test
    fun `when clicking test with valid command start execution`() = runTest {
        whenever(
            mockExecuteShellCommandUseCase.executeWithStreamingOutput(any(), any(), any()),
        ).thenReturn(flowOf(Success(ShellResult(stdout = "", exitCode = 0))))

        viewModel.onCommandChanged("echo test")

        val result = viewModel.onTestClick()

        advanceUntilIdle()

        verify(
            mockExecuteShellCommandUseCase,
            times(1),
        ).executeWithStreamingOutput(eq("echo test"), eq(ShellExecutionMode.STANDARD), eq(10000))
        assertThat(result, `is`(true))
        assertThat(viewModel.state.commandError, `is`(nullValue()))
        assertThat(viewModel.state.isRunning, `is`(false))
    }

    @Test
    fun `when changing description clear error`() = runTest {
        viewModel.onDescriptionChanged("")
        viewModel.onCommandChanged("ls")
        viewModel.onDoneClick()
        assertThat(viewModel.state.descriptionError, `is`(descriptionEmptyErrorString))

        viewModel.onDescriptionChanged("New description")

        assertThat(viewModel.state.descriptionError, `is`(nullValue()))
        assertThat(viewModel.state.description, `is`("New description"))
    }

    @Test
    fun `when changing description update description`() = runTest {
        viewModel.onDescriptionChanged("Test description")

        assertThat(viewModel.state.description, `is`("Test description"))
        assertThat(viewModel.state.descriptionError, `is`(nullValue()))
    }

    @Test
    fun `when changing command clear error`() = runTest {
        viewModel.onCommandChanged("")
        viewModel.onTestClick()
        assertThat(viewModel.state.commandError, `is`(commandEmptyErrorString))

        viewModel.onCommandChanged("echo test")

        assertThat(viewModel.state.commandError, `is`(nullValue()))
        assertThat(viewModel.state.command, `is`("echo test"))
    }

    @Test
    fun `when changing command update command`() = runTest {
        assertThat(viewModel.state.command, `is`(""))

        viewModel.onCommandChanged("echo hello")

        assertThat(viewModel.state.command, `is`("echo hello"))
        assertThat(viewModel.state.commandError, `is`(nullValue()))
    }

    @Test
    fun `when changing command save script text to preferences`() = runTest {
        val testCommand = "echo test command"

        viewModel.onCommandChanged(testCommand)

        val savedScriptText = fakePreferenceRepository.get(Keys.shellCommandScriptText)
            .first()
        assertThat(savedScriptText, `is`(org.hamcrest.Matchers.notNullValue()))
    }

    @Test
    fun `when clicking done show error when command is whitespace`() = runTest {
        viewModel.onCommandChanged("   ")

        val result = viewModel.onDoneClick()

        assertThat(result, `is`(false))
        assertThat(viewModel.state.commandError, `is`(commandEmptyErrorString))
        verify(mockNavigationProvider, never()).popBackStackWithResult(any())
    }

    @Test
    fun `when clicking test show error when command is whitespace`() = runTest {
        viewModel.onCommandChanged("   ")

        val result = viewModel.onTestClick()

        assertThat(result, `is`(false))
        assertThat(viewModel.state.commandError, `is`(commandEmptyErrorString))
        verify(
            mockExecuteShellCommandUseCase,
            never(),
        ).executeWithStreamingOutput(any(), any(), any())
    }

    @Test
    fun `when clicking done show error when description is whitespace`() = runTest {
        viewModel.onCommandChanged("echo test")
        viewModel.onDescriptionChanged("   ")

        val result = viewModel.onDoneClick()

        assertThat(result, `is`(false))
        assertThat(viewModel.state.descriptionError, `is`(descriptionEmptyErrorString))
        verify(mockNavigationProvider, never()).popBackStackWithResult(any())
    }
}
