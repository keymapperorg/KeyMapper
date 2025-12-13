package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.base.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.popup.ToastAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PerformActionsUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testCoroutineScope = TestScope(testDispatcher)

    private lateinit var useCase: PerformActionsUseCaseImpl
    private lateinit var fakeDevicesAdapter: FakeDevicesAdapter
    private lateinit var mockAccessibilityService: IAccessibilityService
    private lateinit var mockToastAdapter: ToastAdapter
    private lateinit var mockInputEventHub: InputEventHub

    @Before
    fun init() {
        fakeDevicesAdapter = FakeDevicesAdapter()
        mockAccessibilityService = mock()
        mockToastAdapter = mock()
        mockInputEventHub = mock()

        useCase = PerformActionsUseCaseImpl(
            service = mockAccessibilityService,
            inputMethodAdapter = mock(),
            switchImeInterface = mock(),
            fileAdapter = mock(),
            suAdapter = mock {},
            shell = mock(),
            intentAdapter = mock(),
            getActionErrorUseCase = mock(),
            keyMapperImeMessenger = mock(),
            packageManagerAdapter = mock(),
            appShortcutAdapter = mock(),
            toastAdapter = mockToastAdapter,
            devicesAdapter = fakeDevicesAdapter,
            phoneAdapter = mock(),
            audioAdapter = mock(),
            cameraAdapter = mock(),
            displayAdapter = mock(),
            lockScreenAdapter = mock(),
            mediaAdapter = mock(),
            airplaneModeAdapter = mock(),
            networkAdapter = mock(),
            bluetoothAdapter = mock(),
            nfcAdapter = mock(),
            openUrlAdapter = mock(),
            resourceProvider = mock(),
            settingsRepository = mock(),
            soundsManager = mock(),
            notificationReceiverAdapter = mock(),
            ringtoneAdapter = mock(),
            inputEventHub = mockInputEventHub,
            systemBridgeConnectionManager = mock(),
            executeShellCommandUseCase = mock(),
            coroutineScope = testCoroutineScope,
            notificationAdapter = mock(),
            settingsAdapter = mock(),
        )
    }

    /**
     * issue #771
     */
    @Test
    fun `dont show accessibility service not found error for open menu action`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = ActionData.OpenMenu

            whenever(
                mockAccessibilityService.performActionOnNode(
                    any(),
                    any(),
                ),
            ).doReturn(KMError.FailedToFindAccessibilityNode)

            // WHEN
            useCase.perform(action)

            // THEN
            verify(mockToastAdapter, never()).show(anyOrNull())
        }
}
