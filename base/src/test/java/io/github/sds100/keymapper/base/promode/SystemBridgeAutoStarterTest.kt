package io.github.sds100.keymapper.base.promode

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.base.utils.TestBuildConfigProvider
import io.github.sds100.keymapper.base.utils.TestScopeClock
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SystemBridgeAutoStarterTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testCoroutineScope = TestScope(testDispatcher)

    private lateinit var systemBridgeAutoStarter: SystemBridgeAutoStarter

    private lateinit var testScopeClock: TestScopeClock
    private lateinit var mockSuAdapter: SuAdapter
    private lateinit var mockShizukuAdapter: ShizukuAdapter
    private lateinit var mockConnectionManager: SystemBridgeConnectionManager
    private lateinit var mockSetupController: SystemBridgeSetupController
    private lateinit var fakePreferences: FakePreferenceRepository
    private lateinit var mockNetworkAdapter: NetworkAdapter
    private lateinit var mockPermissionAdapter: PermissionAdapter
    private lateinit var mockNotificationAdapter: NotificationAdapter
    private lateinit var mockResourceProvider: ResourceProvider
    private lateinit var testBuildConfig: TestBuildConfigProvider

    private val isRootGrantedFlow = MutableStateFlow(false)
    private val shizukuIsStartedFlow = MutableStateFlow(false)
    private val connectionStateFlow = MutableStateFlow<SystemBridgeConnectionState>(
        SystemBridgeConnectionState.Disconnected(time = 0, isStoppedByUser = false),
    )
    private val isWifiConnectedFlow = MutableStateFlow(false)
    private val writeSecureSettingsGrantedFlow = MutableStateFlow(false)
    private val shizukuPermissionGrantedFlow = MutableStateFlow(false)

    @Before
    fun init() {
        mockSuAdapter = mock {
            on { isRootGranted } doReturn isRootGrantedFlow
        }

        mockShizukuAdapter = mock {
            on { isStarted } doReturn shizukuIsStartedFlow
        }

        mockConnectionManager = mock {
            on { connectionState } doReturn connectionStateFlow
        }

        mockSetupController = mock()
        runBlocking {
            whenever(mockSetupController.isAdbPaired()).doReturn(false)
        }

        fakePreferences = FakePreferenceRepository()

        mockNetworkAdapter = mock {
            on { isWifiConnected } doReturn isWifiConnectedFlow
        }

        mockPermissionAdapter = mock {
            on { isGrantedFlow(Permission.WRITE_SECURE_SETTINGS) } doReturn
                writeSecureSettingsGrantedFlow
            on { isGrantedFlow(Permission.SHIZUKU) } doReturn shizukuPermissionGrantedFlow
        }

        mockNotificationAdapter = mock()

        mockResourceProvider = mock {
            on { getString(any()) } doReturn "test_string"
        }

        testScopeClock = TestScopeClock(testCoroutineScope)

        testBuildConfig = TestBuildConfigProvider(sdkInt = 30)

        systemBridgeAutoStarter = SystemBridgeAutoStarter(
            coroutineScope = testCoroutineScope,
            clock = testScopeClock,
            suAdapter = mockSuAdapter,
            shizukuAdapter = mockShizukuAdapter,
            connectionManager = mockConnectionManager,
            setupController = mockSetupController,
            preferences = fakePreferences,
            networkAdapter = mockNetworkAdapter,
            permissionAdapter = mockPermissionAdapter,
            notificationAdapter = mockNotificationAdapter,
            resourceProvider = mockResourceProvider,
            buildConfig = testBuildConfig,
        )
    }

    @Test
    fun `auto start within 60 seconds of booting`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        whenever(mockSetupController.isAdbPaired()).thenReturn(true)
        isWifiConnectedFlow.value = true
        writeSecureSettingsGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockSetupController).autoStartWithAdb()
    }

    @Test
    fun `auto start 5 minutes after booting`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        whenever(mockSetupController.isAdbPaired()).thenReturn(true)
        isWifiConnectedFlow.value = true
        writeSecureSettingsGrantedFlow.value = true

        advanceTimeBy(310_000)
        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockSetupController).autoStartWithAdb()
    }

    @Test
    fun `do not auto start with ADB if WRITE_SECURE_SETTINGS is denied`() =
        runTest(testDispatcher) {
            advanceTimeBy(5000L)
            fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)

            writeSecureSettingsGrantedFlow.value = false
            isWifiConnectedFlow.value = false

            systemBridgeAutoStarter.init()
            advanceUntilIdle()

            verify(mockSetupController, never()).autoStartWithAdb()
        }

    @Test
    fun `do not auto start with ADB if wifi is disconnected`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        writeSecureSettingsGrantedFlow.value = true
        isWifiConnectedFlow.value = false

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `auto start with shizuku on Android 10`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        testBuildConfig.sdkInt = 29
        isRootGrantedFlow.value = false
        shizukuIsStartedFlow.value = true
        shizukuPermissionGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithShizuku()
    }

    @Test
    fun `auto start with root on Android 9`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        testBuildConfig.sdkInt = 28
        isRootGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
    }

    @Test
    fun `auto start from pre version 4 when rooted and it was never used before`() =
        runTest(testDispatcher) {
            isRootGrantedFlow.value = true
            fakePreferences.set(Keys.isSystemBridgeUsed, null)
            fakePreferences.set(Keys.hasRootPermissionLegacy, true)

            systemBridgeAutoStarter.init()
            advanceUntilIdle()

            verify(mockConnectionManager).startWithRoot()
            assertThat(fakePreferences.get(Keys.handledRootToProModeUpgrade).first(), `is`(true))
        }

    @Test
    fun `prioritize auto starting with root if shizuku also started`() = runTest(testDispatcher) {
        advanceTimeBy(1_000_000L)
        isRootGrantedFlow.value = true
        shizukuIsStartedFlow.value = true
        shizukuPermissionGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
    }

    @Test
    fun `do not auto start from pre version 4 when shizuku permission granted but not started`() =
        runTest(testDispatcher) {
            advanceTimeBy(1_000_000L)
            shizukuIsStartedFlow.value = false
            shizukuPermissionGrantedFlow.value = true

            systemBridgeAutoStarter.init()
            advanceUntilIdle()

            verify(mockConnectionManager, never()).startWithShizuku()
        }

    @Test
    fun `auto start with root`() = runTest(testDispatcher) {
        advanceTimeBy(1_000_000L)
        isRootGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeEmergencyKilled, false)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
    }

    @Test
    fun `do not auto start when emergency killed`() = runTest(testDispatcher) {
        isRootGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeEmergencyKilled, true)

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `auto start with shizuku when shizuku is available`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)
        shizukuIsStartedFlow.value = true
        shizukuPermissionGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithShizuku()
    }

    @Test
    fun `auto start with Shizuku if Shizuku takes a minute to connect`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        // Initially Shizuku is disconnected, and is connected 50 seconds after booting
        shizukuIsStartedFlow.value = false
        shizukuPermissionGrantedFlow.value = true

        inOrder(mockConnectionManager) {
            systemBridgeAutoStarter.init()
            advanceTimeBy(50000)
            verify(mockConnectionManager, never()).startWithShizuku()

            // Shizuku starts
            shizukuIsStartedFlow.value = true

            advanceTimeBy(20000)
            verify(mockConnectionManager).startWithShizuku()
        }
    }

    @Test
    fun `auto start with ADB if WiFi takes a minute to connect`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        whenever(mockSetupController.isAdbPaired()).thenReturn(true)
        writeSecureSettingsGrantedFlow.value = true

        // Initially the wifi is disconnected, and is connected 50 seconds after booting
        isWifiConnectedFlow.value = false

        inOrder(mockSetupController) {
            systemBridgeAutoStarter.init()
            advanceTimeBy(50000)
            verify(mockSetupController, never()).autoStartWithAdb()

            // Connect to Wi-Fi network
            isWifiConnectedFlow.value = true

            advanceTimeBy(20000)
            verify(mockSetupController).autoStartWithAdb()
        }
    }

    @Test
    fun `do not auto start when auto start is disabled`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, false)
        isRootGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `do not auto start when already connected`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        systemBridgeAutoStarter.init()
        advanceTimeBy(5000)

        // The system bridge connects during init()
        connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 1000)

        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `show notification when auto starting`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)

        whenever(mockSetupController.isAdbPaired()).thenReturn(true)
        isWifiConnectedFlow.value = true
        writeSecureSettingsGrantedFlow.value = true

        inOrder(mockNotificationAdapter) {
            systemBridgeAutoStarter.init()
            advanceTimeBy(10000)

            // Show the notification that it is auto starting
            verify(mockNotificationAdapter).showNotification(any())

            // Set the state as connected within the timeout
            connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 10000)
            advanceUntilIdle()
            // Do not show another notification after the timeout
            verify(mockNotificationAdapter, never()).showNotification(any())
        }
    }

    @Test
    fun `show failed notification when connection times out`() = runTest(testDispatcher) {
        isRootGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeEmergencyKilled, false)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)
        connectionStateFlow.value =
            SystemBridgeConnectionState.Disconnected(time = 0, isStoppedByUser = false)

        systemBridgeAutoStarter.init()
        // The system bridge remains disconnected
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
        verify(mockNotificationAdapter, atLeast(1)).showNotification(any())
    }

    @Test
    fun `do not auto start when connected`() = runTest(testDispatcher) {
        advanceTimeBy(1_000_000L)
        val connectedState = SystemBridgeConnectionState.Connected(time = 1000)
        connectionStateFlow.value = connectedState
        isRootGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `do not auto restart when disconnected by the user`() = runTest(testDispatcher) {
        advanceTimeBy(5000L)
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)
        isRootGrantedFlow.value = true

        inOrder(mockConnectionManager, mockSetupController) {
            systemBridgeAutoStarter.init()
            advanceTimeBy(100_000)
            verify(mockConnectionManager).startWithRoot()

            // Disconnect the system bridge. Expected is true
            connectionStateFlow.value = SystemBridgeConnectionState.Disconnected(
                time = 1_000_000L,
                isStoppedByUser = true,
            )

            advanceUntilIdle()

            verify(mockConnectionManager, never()).startWithRoot()
            verify(mockConnectionManager, never()).startWithShizuku()
            verify(mockSetupController, never()).autoStartWithAdb()
        }
    }

    @Test
    fun `do not auto restart within 5 minutes of the last auto start`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
        fakePreferences.set(Keys.isSystemBridgeUsed, true)
        isRootGrantedFlow.value = true

        inOrder(mockConnectionManager) {
            systemBridgeAutoStarter.init()
            advanceTimeBy(6000)
            // Try auto starting when the process launches
            verify(mockConnectionManager).startWithRoot()
            connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 6000)

            advanceUntilIdle()
            // It is killed unexpectedly straight after auto starting
            connectionStateFlow.value = SystemBridgeConnectionState.Disconnected(
                time = 7000,
                isStoppedByUser = true,
            )

            advanceUntilIdle()
            // Auto starting fails and so it does not try starting again
            verify(mockConnectionManager, never()).startWithRoot()
        }
    }

    @Test
    fun `show killed and not restarting notification if system bridge dies less than 30 seconds after auto starting`() =
        runTest(testDispatcher) {
            fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
            fakePreferences.set(Keys.isSystemBridgeUsed, true)

            whenever(
                mockResourceProvider.getString(R.string.system_bridge_died_notification_title),
            ).thenReturn("died")

            whenever(mockSetupController.isAdbPaired()).thenReturn(true)
            isWifiConnectedFlow.value = true
            writeSecureSettingsGrantedFlow.value = true

            inOrder(mockNotificationAdapter) {
                systemBridgeAutoStarter.init()
                advanceTimeBy(10000)

                // Show the notification that it is auto starting
                verify(mockNotificationAdapter).showNotification(any())
                connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 10000)

                // Set the state as connected within the timeout
                connectionStateFlow.value =
                    SystemBridgeConnectionState.Disconnected(time = 11000, isStoppedByUser = false)
                advanceUntilIdle()
                // Show notification
                val argument = argumentCaptor<NotificationModel>()
                verify(mockNotificationAdapter).showNotification(argument.capture())

                assertThat(argument.firstValue.title, `is`("died"))
            }
        }

    @Test
    fun `auto restart when shizuku connects after the system bridge dies`() =
        runTest(testDispatcher) {
            fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
            fakePreferences.set(Keys.isSystemBridgeUsed, true)

            // Shizuku is not started initially
            shizukuIsStartedFlow.value = false
            shizukuPermissionGrantedFlow.value = true

            // System bridge was already running when Key Mapper process launched
            connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 0)

            inOrder(mockConnectionManager) {
                systemBridgeAutoStarter.init()
                advanceUntilIdle()

                // If it dies, then it will wait for shizuku to connect
                connectionStateFlow.value =
                    SystemBridgeConnectionState.Disconnected(time = 3000, isStoppedByUser = false)

                advanceTimeBy(2000)
                verify(mockConnectionManager, never()).startWithShizuku()

                shizukuIsStartedFlow.value = true
                advanceUntilIdle()

                verify(mockConnectionManager).startWithShizuku()
            }
        }

    @Test
    fun `do not auto start when shizuku is connected if system bridge was stopped by the user`() =
        runTest(testDispatcher) {
            fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
            fakePreferences.set(Keys.isSystemBridgeUsed, true)

            // Shizuku is not started initially
            shizukuIsStartedFlow.value = false
            shizukuPermissionGrantedFlow.value = true

            // System bridge was already running when Key Mapper process launched
            connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 0)

            inOrder(mockConnectionManager) {
                systemBridgeAutoStarter.init()
                advanceUntilIdle()

                // The user stopped the system bridge
                connectionStateFlow.value =
                    SystemBridgeConnectionState.Disconnected(time = 3000, isStoppedByUser = true)

                advanceTimeBy(2000)
                verify(mockConnectionManager, never()).startWithShizuku()

                shizukuIsStartedFlow.value = true
                advanceUntilIdle()

                // Do not start it when shizuku connects
                verify(mockConnectionManager, never()).startWithShizuku()
            }
        }

    @Test
    fun `auto restart when wifi is connected after the system bridge dies`() =
        runTest(testDispatcher) {
            fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
            fakePreferences.set(Keys.isSystemBridgeUsed, true)

            // WiFi is not connected initially
            isWifiConnectedFlow.value = false
            writeSecureSettingsGrantedFlow.value = true
            whenever(mockSetupController.isAdbPaired()).thenReturn(true)

            // System bridge was already running when Key Mapper process launched
            connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 0)

            inOrder(mockSetupController) {
                systemBridgeAutoStarter.init()
                advanceUntilIdle()

                // If it dies, then it will wait for wifi to connect
                connectionStateFlow.value =
                    SystemBridgeConnectionState.Disconnected(time = 2000, isStoppedByUser = false)

                advanceTimeBy(2000)
                verify(mockSetupController, never()).autoStartWithAdb()

                isWifiConnectedFlow.value = true
                advanceUntilIdle()

                verify(mockSetupController).autoStartWithAdb()
            }
        }

    @Test
    fun `do not auto start with Shizuku on launch if it was never used before`() =
        runTest(testDispatcher) {
            fakePreferences.set(Keys.isSystemBridgeUsed, null)
            shizukuIsStartedFlow.value = true
            shizukuPermissionGrantedFlow.value = true

            systemBridgeAutoStarter.init()
            advanceUntilIdle()

            verify(mockConnectionManager, never()).startWithRoot()
            verify(mockConnectionManager, never()).startWithShizuku()
            verify(mockSetupController, never()).autoStartWithAdb()
        }

    @Test
    fun `do not auto restart if keep alive is disabled while it is connected`() =
        runTest(testDispatcher) {
            fakePreferences.set(Keys.isSystemBridgeUsed, true)
            fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, true)
            isRootGrantedFlow.value = true

            inOrder(mockConnectionManager) {
                systemBridgeAutoStarter.init()
                advanceTimeBy(6000)

                connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 6000)
                verify(mockConnectionManager).startWithRoot()

                // The user disabled keep alive while system bridge is running
                fakePreferences.set(Keys.isSystemBridgeKeepAliveEnabled, false)
                // The system bridge dies
                connectionStateFlow.value =
                    SystemBridgeConnectionState.Disconnected(time = 7000, isStoppedByUser = false)

                advanceTimeBy(2000)

                // The system bridge should not be auto restarted.
                verify(mockConnectionManager, never()).startWithRoot()
            }
        }
}
