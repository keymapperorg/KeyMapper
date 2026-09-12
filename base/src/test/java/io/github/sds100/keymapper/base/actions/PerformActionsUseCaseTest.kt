package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.base.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.PinchScreenType
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.display.DisplayAdapter
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
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
    private lateinit var mockDisplayAdapter: DisplayAdapter

    @Before
    fun init() {
        fakeDevicesAdapter = FakeDevicesAdapter()
        mockAccessibilityService = mock()
        mockToastAdapter = mock()
        mockInputEventHub = mock()
        mockDisplayAdapter = mock()

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
            displayAdapter = mockDisplayAdapter,
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
            verify(mockToastAdapter, never()).show(any(), any())
        }

    /**
     * issue #2217
     */
    @Test
    fun `scale tap screen action to the current display resolution`() = runTest(testDispatcher) {
        // GIVEN the coordinate was picked on a 1080x2400 display and the display is now 1440x3200.
        whenever(mockDisplayAdapter.size).doReturn(SizeKM(1440, 3200))
        whenever(mockAccessibilityService.tapScreen(any(), any(), any())).doReturn(Success(Unit))

        val action = ActionData.TapScreen(
            x = 540,
            y = 1200,
            description = null,
            screenResolution = SizeKM(1080, 2400),
        )

        // WHEN
        useCase.perform(action)

        // THEN
        verify(mockAccessibilityService).tapScreen(eq(720), eq(1600), any())
    }

    /**
     * issue #2217
     */
    @Test
    fun `dont scale tap screen action created before the resolution was saved`() =
        runTest(testDispatcher) {
            // GIVEN
            whenever(mockDisplayAdapter.size).doReturn(SizeKM(1440, 3200))
            whenever(mockAccessibilityService.tapScreen(any(), any(), any()))
                .doReturn(Success(Unit))

            val action = ActionData.TapScreen(
                x = 540,
                y = 1200,
                description = null,
                screenResolution = null,
            )

            // WHEN
            useCase.perform(action)

            // THEN the coordinate is dispatched exactly as it was saved.
            verify(mockAccessibilityService).tapScreen(eq(540), eq(1200), any())
        }

    /**
     * issue #2217
     */
    @Test
    fun `scale both ends of a swipe screen action to the current display resolution`() =
        runTest(testDispatcher) {
            // GIVEN
            whenever(mockDisplayAdapter.size).doReturn(SizeKM(1440, 3200))
            whenever(
                mockAccessibilityService.swipeScreen(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                ),
            ).doReturn(Success(Unit))

            val action = ActionData.SwipeScreen(
                xStart = 270,
                yStart = 600,
                xEnd = 540,
                yEnd = 1200,
                fingerCount = 1,
                duration = 250,
                description = null,
                screenResolution = SizeKM(1080, 2400),
            )

            // WHEN
            useCase.perform(action)

            // THEN
            verify(mockAccessibilityService).swipeScreen(
                eq(360),
                eq(800),
                eq(720),
                eq(1600),
                eq(1),
                eq(250),
                any(),
            )
        }

    /**
     * issue #2217
     */
    @Test
    fun `scale the centre and the distance of a pinch screen action`() = runTest(testDispatcher) {
        // GIVEN
        whenever(mockDisplayAdapter.size).doReturn(SizeKM(1440, 3200))
        whenever(
            mockAccessibilityService.pinchScreen(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).doReturn(Success(Unit))

        val action = ActionData.PinchScreen(
            x = 540,
            y = 1200,
            distance = 300,
            pinchType = PinchScreenType.PINCH_IN,
            fingerCount = 2,
            duration = 250,
            description = null,
            screenResolution = SizeKM(1080, 2400),
        )

        // WHEN
        useCase.perform(action)

        // THEN
        verify(mockAccessibilityService).pinchScreen(
            eq(720),
            eq(1600),
            eq(400),
            eq(PinchScreenType.PINCH_IN),
            eq(2),
            eq(250),
            any(),
        )
    }

    /**
     * issue #2217
     */
    @Test
    fun `dont scale tap screen action when the display is only rotated`() =
        runTest(testDispatcher) {
            // GIVEN the coordinate was picked on a portrait display and the display is now
            // landscape at the same resolution.
            whenever(mockDisplayAdapter.size).doReturn(SizeKM(2400, 1080))
            whenever(mockAccessibilityService.tapScreen(any(), any(), any()))
                .doReturn(Success(Unit))

            val action = ActionData.TapScreen(
                x = 540,
                y = 1200,
                description = null,
                screenResolution = SizeKM(1080, 2400),
            )

            // WHEN
            useCase.perform(action)

            // THEN
            verify(mockAccessibilityService).tapScreen(eq(540), eq(1200), any())
        }
}
