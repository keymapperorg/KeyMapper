package io.github.sds100.keymapper.base.promode

import io.github.sds100.keymapper.base.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SystemBridgeSetupUseCaseTest {

    private lateinit var useCase: SystemBridgeSetupUseCaseImpl
    private lateinit var fakePreferences: FakePreferenceRepository
    private lateinit var mockSuAdapter: SuAdapter
    private lateinit var mockSystemBridgeSetupController: SystemBridgeSetupController
    private lateinit var mockSystemBridgeConnectionManager: SystemBridgeConnectionManager
    private lateinit var mockShizukuAdapter: ShizukuAdapter
    private lateinit var mockPermissionAdapter: PermissionAdapter
    private lateinit var mockAccessibilityServiceAdapter: AccessibilityServiceAdapter
    private lateinit var mockNetworkAdapter: NetworkAdapter

    @Before
    fun init() {
        fakePreferences = FakePreferenceRepository()
        mockSuAdapter = mock()
        mockSystemBridgeSetupController = mock()
        mockSystemBridgeConnectionManager = mock()
        mockShizukuAdapter = mock()
        mockPermissionAdapter = mock()
        mockAccessibilityServiceAdapter = mock()
        mockNetworkAdapter = mock()

        useCase = SystemBridgeSetupUseCaseImpl(
            preferences = fakePreferences,
            suAdapter = mockSuAdapter,
            systemBridgeSetupController = mockSystemBridgeSetupController,
            systemBridgeConnectionManager = mockSystemBridgeConnectionManager,
            shizukuAdapter = mockShizukuAdapter,
            permissionAdapter = mockPermissionAdapter,
            accessibilityServiceAdapter = mockAccessibilityServiceAdapter,
            networkAdapter = mockNetworkAdapter,
        )
    }

    @Test
    fun `set isSystemBridgeStoppedByUser to true when stopping system bridge`() = runTest {
        useCase.stopSystemBridge()

        assertThat(
            fakePreferences.get(Keys.isSystemBridgeStoppedByUser).first(),
            `is`(true),
        )
        verify(mockSystemBridgeConnectionManager).stopSystemBridge()
    }

    @Test
    fun `set isSystemBridgeEmergencyKilled and isSystemBridgeStoppedByUser to false when starting system bridge with root`() =
        runTest {
            useCase.startSystemBridgeWithRoot()

            assertThat(
                fakePreferences.get(Keys.isSystemBridgeEmergencyKilled).first(),
                `is`(false),
            )
            assertThat(
                fakePreferences.get(Keys.isSystemBridgeStoppedByUser).first(),
                `is`(false),
            )
        }

    @Test
    fun `set isSystemBridgeEmergencyKilled and isSystemBridgeStoppedByUser to false when starting system bridge with shizuku`() =
        runTest {
            useCase.startSystemBridgeWithShizuku()

            assertThat(
                fakePreferences.get(Keys.isSystemBridgeEmergencyKilled).first(),
                `is`(false),
            )
            assertThat(
                fakePreferences.get(Keys.isSystemBridgeStoppedByUser).first(),
                `is`(false),
            )
        }

    @Test
    fun `set isSystemBridgeEmergencyKilled and isSystemBridgeStoppedByUser to false when starting system bridge with adb`() =
        runTest {
            useCase.startSystemBridgeWithAdb()

            assertThat(
                fakePreferences.get(Keys.isSystemBridgeEmergencyKilled).first(),
                `is`(false),
            )
            assertThat(
                fakePreferences.get(Keys.isSystemBridgeStoppedByUser).first(),
                `is`(false),
            )
        }

    @Test
    fun `set isSystemBridgeEmergencyKilled and isSystemBridgeStoppedByUser to false when auto starting system bridge with adb`() =
        runTest {
            useCase.startSystemBridgeWithAdb()

            assertThat(
                fakePreferences.get(Keys.isSystemBridgeEmergencyKilled).first(),
                `is`(false),
            )
            assertThat(
                fakePreferences.get(Keys.isSystemBridgeStoppedByUser).first(),
                `is`(false),
            )
        }
}
