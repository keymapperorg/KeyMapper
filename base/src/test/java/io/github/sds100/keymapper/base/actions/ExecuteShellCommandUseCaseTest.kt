package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Regression tests for issue #2209. Scripts pasted from a computer can have Windows (\r\n) line
 * endings, which a shell fails to parse because \r stops reserved words like "then" from being
 * recognized.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ExecuteShellCommandUseCaseTest {

    private companion object {
        private const val TIMEOUT = 10000L

        private const val WINDOWS_SCRIPT =
            "if true; then\r\n    echo hello\r\nelse\r\n    echo bye\r\nfi"

        private const val UNIX_SCRIPT =
            "if true; then\n    echo hello\nelse\n    echo bye\nfi"
    }

    private lateinit var useCase: ExecuteShellCommandUseCase
    private lateinit var mockShellAdapter: ShellAdapter
    private lateinit var mockSuAdapter: SuAdapter
    private lateinit var mockSystemBridgeConnectionManager: SystemBridgeConnectionManager
    private lateinit var mockSystemBridge: ISystemBridge

    private val shellResult: KMResult<ShellResult> =
        ShellResult(stdout = "", exitCode = 0).success()

    @Before
    fun init() {
        mockShellAdapter = mock()
        mockSuAdapter = mock()
        mockSystemBridge = mock()
        mockSystemBridgeConnectionManager = mock()

        useCase = ExecuteShellCommandUseCase(
            shellAdapter = mockShellAdapter,
            suAdapter = mockSuAdapter,
            systemBridgeConnectionManager = mockSystemBridgeConnectionManager,
        )
    }

    @Test
    fun `standard mode replaces windows line endings`() = runTest {
        whenever(mockShellAdapter.execute(any(), any())).thenReturn(shellResult)

        useCase.execute(WINDOWS_SCRIPT, ShellExecutionMode.STANDARD, TIMEOUT)

        verify(mockShellAdapter).execute(eq(UNIX_SCRIPT), eq(TIMEOUT))
    }

    @Test
    fun `root mode replaces windows line endings`() = runTest {
        whenever(mockSuAdapter.execute(any(), any())).thenReturn(shellResult)

        useCase.execute(WINDOWS_SCRIPT, ShellExecutionMode.ROOT, TIMEOUT)

        verify(mockSuAdapter).execute(eq(UNIX_SCRIPT), eq(TIMEOUT))
    }

    @Test
    fun `adb mode replaces windows line endings`() = runTest {
        stubSystemBridge()

        useCase.execute(WINDOWS_SCRIPT, ShellExecutionMode.ADB, TIMEOUT)

        verify(mockSystemBridge).executeCommand(eq(UNIX_SCRIPT), eq(TIMEOUT))
    }

    @Test
    fun `standard mode streaming replaces windows line endings`() = runTest {
        whenever(mockShellAdapter.executeWithStreamingOutput(any(), any()))
            .thenReturn(flowOf(shellResult))

        useCase.executeWithStreamingOutput(
            WINDOWS_SCRIPT,
            ShellExecutionMode.STANDARD,
            TIMEOUT,
        ).first()

        verify(mockShellAdapter).executeWithStreamingOutput(eq(UNIX_SCRIPT), eq(TIMEOUT))
    }

    @Test
    fun `root mode streaming replaces windows line endings`() = runTest {
        whenever(mockSuAdapter.executeWithStreamingOutput(any(), any()))
            .thenReturn(flowOf(shellResult))

        useCase.executeWithStreamingOutput(
            WINDOWS_SCRIPT,
            ShellExecutionMode.ROOT,
            TIMEOUT,
        ).first()

        verify(mockSuAdapter).executeWithStreamingOutput(eq(UNIX_SCRIPT), eq(TIMEOUT))
    }

    @Test
    fun `adb mode streaming replaces windows line endings`() = runTest {
        stubSystemBridge()

        useCase.executeWithStreamingOutput(
            WINDOWS_SCRIPT,
            ShellExecutionMode.ADB,
            TIMEOUT,
        ).first()

        verify(mockSystemBridge).executeCommand(eq(UNIX_SCRIPT), eq(TIMEOUT))
    }

    @Test
    fun `replace lone carriage returns with new lines`() = runTest {
        whenever(mockShellAdapter.execute(any(), any())).thenReturn(shellResult)

        useCase.execute("echo hello\recho bye", ShellExecutionMode.STANDARD, TIMEOUT)

        verify(mockShellAdapter).execute(eq("echo hello\necho bye"), eq(TIMEOUT))
    }

    @Test
    fun `do not change a command that already has unix line endings`() = runTest {
        whenever(mockShellAdapter.execute(any(), any())).thenReturn(shellResult)

        useCase.execute(UNIX_SCRIPT, ShellExecutionMode.STANDARD, TIMEOUT)

        verify(mockShellAdapter).execute(eq(UNIX_SCRIPT), eq(TIMEOUT))
    }

    private fun stubSystemBridge() {
        whenever(mockSystemBridge.executeCommand(any(), any()))
            .thenReturn(ShellResult(stdout = "", exitCode = 0))

        whenever(mockSystemBridgeConnectionManager.run<ShellResult>(any()))
            .thenAnswer { invocation ->
                val block = invocation.getArgument<(ISystemBridge) -> ShellResult>(0)
                Success(block(mockSystemBridge))
            }
    }
}
