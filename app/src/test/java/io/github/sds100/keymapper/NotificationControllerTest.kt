package io.github.sds100.keymapper

import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.onboarding.FakeOnboardingUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.notifications.ManageNotificationsUseCase
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.notifications.NotificationModel
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Created by sds100 on 25/04/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NotificationControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var controller: NotificationController
    private lateinit var mockManageNotifications: ManageNotificationsUseCase
    private lateinit var mockResourceProvider: ResourceProvider
    private lateinit var fakeOnboarding: FakeOnboardingUseCase

    private lateinit var onActionClick: MutableSharedFlow<String>

    @Before
    fun init() {
        onActionClick = MutableSharedFlow()

        mockManageNotifications = mock {
            on { onActionClick }.then { onActionClick }
            on { showToggleMappingsNotification }.then { flow<Boolean> { } }
            on { showImePickerNotification }.then { flow<Boolean> { } }
        }

        mockResourceProvider = mock()
        fakeOnboarding = FakeOnboardingUseCase()

        controller = NotificationController(
            testScope,
            mockManageNotifications,
            pauseMappings = mock {
                on { isPaused }.then { flow<Boolean> {} }
            },
            showImePicker = mock(),
            controlAccessibilityService = mock {
                on { serviceState }.then { flow<ServiceState> {} }
            },
            toggleCompatibleIme = mock {
                on { sufficientPermissions }.then { flow<Boolean> {} }
            },
            hideInputMethod = mock {
                on { onHiddenChange }.then { flow<Boolean> {} }
            },
            areFingerprintGesturesSupported = mock {
                on { isSupported }.then { flow<Boolean?> {} }
            },
            onboardingUseCase = fakeOnboarding,
            resourceProvider = mockResourceProvider,
            dispatchers = TestDispatcherProvider(testDispatcher),
        )
    }

    @Test
    fun `click setup chosen devices notification, open app and approve`() =
        runTest(testDispatcher) {
            val value = async {
                controller.openApp.first()
            }

            onActionClick.emit(NotificationController.ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN)

            assertThat(value.await(), `is`(""))
            assertThat(fakeOnboarding.approvedSetupChosenDevicesAgainNotification, `is`(true))
        }

    @Test
    fun `show setup chosen devices notification`() =
        runTest(testDispatcher) {
            // GIVEN
            val title = "title"
            val text = "text"
            whenever(mockResourceProvider.getString(R.string.notification_setup_chosen_devices_again_title)).then { title }
            whenever(mockResourceProvider.getString(R.string.notification_setup_chosen_devices_again_text)).then { text }

            // WHEN
            fakeOnboarding.showSetupChosenDevicesAgainNotification.value = true

            // THEN
            verify(
                mockResourceProvider,
                times(1),
            ).getString(R.string.notification_setup_chosen_devices_again_title)

            verify(
                mockResourceProvider,
                times(1),
            ).getString(R.string.notification_setup_chosen_devices_again_text)

            val expectedNotification = NotificationModel(
                id = NotificationController.ID_SETUP_CHOSEN_DEVICES_AGAIN,
                channel = NotificationController.CHANNEL_NEW_FEATURES,
                icon = R.drawable.ic_notification_settings,
                title = title,
                text = text,
                onClickActionId = NotificationController.ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN,
                showOnLockscreen = false,
                onGoing = false,
                priority = NotificationCompat.PRIORITY_LOW,
                actions = emptyList(),
                autoCancel = true,
                bigTextStyle = true,
            )

            verify(mockManageNotifications, times(1)).show(expectedNotification)

            // this should be called when the notification is clicked
            assertThat(fakeOnboarding.approvedSetupChosenDevicesAgainNotification, `is`(false))
        }
}
