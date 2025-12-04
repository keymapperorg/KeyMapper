package io.github.sds100.keymapper.base.promode

import io.github.sds100.keymapper.base.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.base.utils.TestBuildConfigProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.Clock
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
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

    private lateinit var mockClock: Clock
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

        mockClock = mock()

        testBuildConfig = TestBuildConfigProvider(sdkInt = 30)

        systemBridgeAutoStarter = SystemBridgeAutoStarter(
            coroutineScope = testCoroutineScope,
            clock = mockClock,
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

    private fun setAutoStartBootState() {
        whenever(mockClock.elapsedRealtime()).thenReturn(5000L)
        fakePreferences.set(Keys.isProModeAutoStartBootEnabled, true)
        fakePreferences.set(Keys.isCleanShutdown, true)
    }

    @Test
    fun `auto start within 60 seconds of booting`() = runTest(testDispatcher) {
        setAutoStartBootState()

        whenever(mockSetupController.isAdbPaired()).thenReturn(true)
        isWifiConnectedFlow.value = true
        writeSecureSettingsGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockSetupController).autoStartWithAdb()
    }

    @Test
    fun `auto start 5 minutes after booting`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(300_000L)
        fakePreferences.set(Keys.isProModeAutoStartBootEnabled, true)
        fakePreferences.set(Keys.isCleanShutdown, true)
        whenever(mockSetupController.isAdbPaired()).thenReturn(true)
        isWifiConnectedFlow.value = true
        writeSecureSettingsGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockSetupController).autoStartWithAdb()
    }

    @Test
    fun `do not auto start with ADB if WRITE_SECURE_SETTINGS is denied`() =
        runTest(testDispatcher) {
            setAutoStartBootState()
            writeSecureSettingsGrantedFlow.value = false
            isWifiConnectedFlow.value = false

            systemBridgeAutoStarter.init()
            advanceUntilIdle()

            verify(mockSetupController, never()).autoStartWithAdb()
        }

    @Test
    fun `do not auto start with ADB if wifi is disconnected`() = runTest(testDispatcher) {
        setAutoStartBootState()
        writeSecureSettingsGrantedFlow.value = true
        isWifiConnectedFlow.value = false

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `auto start on boot with shizuku on Android 10`() = runTest(testDispatcher) {
        setAutoStartBootState()

        testBuildConfig.sdkInt = 29
        isRootGrantedFlow.value = false
        shizukuIsStartedFlow.value = true
        shizukuPermissionGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithShizuku()
    }

    @Test
    fun `auto start on boot with root on Android 10`() = runTest(testDispatcher) {
        setAutoStartBootState()

        testBuildConfig.sdkInt = 29
        isRootGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
    }

    @Test
    fun `auto start from pre version 4 when rooted and it was never used before`() =
        runTest(testDispatcher) {
            whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
            isRootGrantedFlow.value = true
            fakePreferences.set(Keys.isSystemBridgeUsed, null)

            systemBridgeAutoStarter.init()
            advanceUntilIdle()

            verify(mockConnectionManager).startWithRoot()
            assertTrue(fakePreferences.get(Keys.handledRootToProModeUpgrade).first() == true)
        }

    @Test
    fun `prioritize auto starting with root if shizuku also started`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
        isRootGrantedFlow.value = true
        shizukuIsStartedFlow.value = true
        shizukuPermissionGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
    }

    @Test
    fun `do not auto start from pre version 4 when shizuku permission but not started`() =
        runTest(testDispatcher) {
            whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
            shizukuIsStartedFlow.value = false
            shizukuPermissionGrantedFlow.value = true

            systemBridgeAutoStarter.init()
            advanceUntilIdle()

            verify(mockConnectionManager, never()).startWithShizuku()
        }

    @Test
    fun `auto start with root`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
        isRootGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeEmergencyKilled, false)

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
    }

    @Test
    fun `do not auto start when already connected`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
        isRootGrantedFlow.value = true
        connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 1000)

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `do not auto start when emergency killed`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
        isRootGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeEmergencyKilled, true)

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `auto start when disconnected unexpectedly`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
        val disconnectedState = SystemBridgeConnectionState.Disconnected(
            time = 1000,
            isStoppedByUser = false,
        )
        connectionStateFlow.value = disconnectedState
        isRootGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeEmergencyKilled, false)

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
    }

    @Test
    fun `auto start with root when rooted`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
        isRootGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
    }

    @Test
    fun `auto start on boot with shizuku when shizuku is available`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(50000L)
        fakePreferences.set(Keys.isProModeAutoStartBootEnabled, true)
        fakePreferences.set(Keys.isCleanShutdown, true)
        shizukuIsStartedFlow.value = true
        shizukuPermissionGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithShizuku()
    }

    @Test
    fun `wait a minute when auto starting on boot with shizuku`() = runTest(testDispatcher) {
        setAutoStartBootState()
        // Shizuku is initially disconnected on boot but shizuku permission is granted
        shizukuIsStartedFlow.value = false
        shizukuPermissionGrantedFlow.value = true

        inOrder(mockConnectionManager) {
            systemBridgeAutoStarter.init()
            advanceTimeBy(50000)
            verify(mockConnectionManager, never()).startWithShizuku()

            // Shizuku then connects
            shizukuIsStartedFlow.value = true

            advanceTimeBy(20000)
            verify(mockConnectionManager).startWithShizuku()
        }
    }

    @Test
    fun `wait a minute when auto starting on boot with ADB`() = runTest(testDispatcher) {
        setAutoStartBootState()

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
    fun `do not auto start on boot when clean shutdown is false`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(5000L)
        fakePreferences.set(Keys.isProModeAutoStartBootEnabled, true)
        fakePreferences.set(Keys.isCleanShutdown, false)
        isRootGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `do not auto start on boot when boot auto start is disabled`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(5000L)
        fakePreferences.set(Keys.isProModeAutoStartBootEnabled, false)
        fakePreferences.set(Keys.isCleanShutdown, true)
        isRootGrantedFlow.value = true

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `do not auto start on boot when already connected`() = runTest(testDispatcher) {
        setAutoStartBootState()
        val connectedState = SystemBridgeConnectionState.Connected(time = 1000)
        connectionStateFlow.value = connectedState

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager, never()).startWithRoot()
        verify(mockConnectionManager, never()).startWithShizuku()
        verify(mockSetupController, never()).autoStartWithAdb()
    }

    @Test
    fun `show notification when auto starting on boot`() = runTest(testDispatcher) {
        setAutoStartBootState()
        whenever(mockSetupController.isAdbPaired()).thenReturn(true)
        isWifiConnectedFlow.value = true
        writeSecureSettingsGrantedFlow.value = true

        inOrder(mockNotificationAdapter) {
            systemBridgeAutoStarter.init()
            advanceTimeBy(70000)

            // Show the first notification that it is auto starting
            verify(mockNotificationAdapter).showNotification(any())

            // Set the state as connected within the timeout
            connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 70000)

            advanceUntilIdle()
            // Do not show another notification after the timeout
            verify(mockNotificationAdapter, never()).showNotification(any())
        }
    }

    @Test
    fun `show notifications when auto restarting after killed`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
        isRootGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeEmergencyKilled, false)
        fakePreferences.set(Keys.handledRootToProModeUpgrade, true)
        connectionStateFlow.value =
            SystemBridgeConnectionState.Disconnected(time = 800_000L, isStoppedByUser = false)

        inOrder(mockNotificationAdapter) {
            systemBridgeAutoStarter.init()
            advanceTimeBy(1000)

            // Show the first notification that it is auto starting
            verify(mockNotificationAdapter).showNotification(any())

            // Set the state as connected within the timeout
            connectionStateFlow.value = SystemBridgeConnectionState.Connected(time = 1_001_000L)

            advanceUntilIdle()
            // Do not show another notification after the timeout
            verify(mockNotificationAdapter, never()).showNotification(any())
        }
    }

    @Test
    fun `show failed notification when connection times out`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(70000L)
        isRootGrantedFlow.value = true
        fakePreferences.set(Keys.isSystemBridgeEmergencyKilled, false)
        connectionStateFlow.value =
            SystemBridgeConnectionState.Disconnected(time = 1000, isStoppedByUser = false)

        systemBridgeAutoStarter.init()
        advanceUntilIdle()

        verify(mockConnectionManager).startWithRoot()
        verify(mockNotificationAdapter, atLeast(1)).showNotification(any())
    }

    @Test
    fun `do not auto start when connected`() = runTest(testDispatcher) {
        whenever(mockClock.elapsedRealtime()).thenReturn(1_000_000L)
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
        setAutoStartBootState()
        whenever(mockClock.elapsedRealtime()).thenReturn(1000L)
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
    fun `do not auto restart system bridge within 30 seconds of the last auto start`() =
        runTest(testDispatcher) {
            setAutoStartBootState()
            isRootGrantedFlow.value = true

            inOrder(mockConnectionManager) {
                systemBridgeAutoStarter.init()
                advanceUntilIdle()
                // Try auto starting on boot
                verify(mockConnectionManager).startWithRoot()

                connectionStateFlow.value = SystemBridgeConnectionState.Disconnected(
                    time = 1000,
                    isStoppedByUser = true, // It is killed unexpectedly
                )
                advanceUntilIdle()
                // Auto starting fails and so it tries to start again
                verify(mockConnectionManager, never()).startWithRoot()
            }
        }

    @Test
    fun `auto restart system bridge 30 seconds after the last auto start`() =
        runTest(testDispatcher) {
            setAutoStartBootState()
            isRootGrantedFlow.value = true

            inOrder(mockConnectionManager) {
                systemBridgeAutoStarter.init()
                advanceUntilIdle()
                // Try auto starting on boot
                verify(mockConnectionManager).startWithRoot()

                advanceTimeBy(30000)
                connectionStateFlow.value = SystemBridgeConnectionState.Disconnected(
                    time = 61000,
                    isStoppedByUser = false, // It is killed unexpectedly
                )
                advanceUntilIdle()
                // Auto starting fails and so it tries to start again
                verify(mockConnectionManager).startWithRoot()
            }
        }

    @Test
    fun `auto restart when shizuku connects`() = runTest(testDispatcher) {
        shizukuIsStartedFlow.value = true
        shizukuPermissionGrantedFlow.value = true

        // If it dies, then it will wait for shizuku to connect
        systemBridgeAutoStarter.init()
        advanceTimeBy(1_000_000)
    }

    @Test
    fun `auto start when shizuku is connected more than 5 mins after booting`() =
        runTest(testDispatcher) {
            TODO()
        }

    @Test
    fun `do not auto start when shizuku is connected if system bridge was stopped by the user`() =
        runTest(testDispatcher) {
            TODO()
        }

    @Test
    fun `auto restart when wifi is connected`() = runTest(testDispatcher) {
        // If it dies, then it will wait for a wifi connection
        TODO()
    }

    @Test
    fun `auto start when wifi is connected more than 5 mins after booting`() =
        runTest(testDispatcher) {
            TODO()
        }

    @Test
    fun `do not auto start on launch if it was never used before`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeUsed, null)
    }

    @Test
    fun `do not auto restart if it was never used before`() = runTest(testDispatcher) {
        fakePreferences.set(Keys.isSystemBridgeUsed, null)
    }
}
