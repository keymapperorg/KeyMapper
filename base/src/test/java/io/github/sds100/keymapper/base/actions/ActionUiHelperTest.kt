package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.Success
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ActionUiHelperTest {

    private lateinit var mockUseCase: DisplayActionUseCase
    private lateinit var helper: ActionUiHelper

    @Before
    fun init() {
        mockUseCase = mock()

        val mockResourceProvider: ResourceProvider = mock()
        whenever(mockResourceProvider.getString(any(), any<Any>())).thenAnswer {
            it.getArgument<Any>(1).toString()
        }

        helper = ActionUiHelper(mockUseCase, mockResourceProvider)
    }

    @Test
    fun `open app title uses the live app name when the app is installed`() {
        whenever(mockUseCase.getAppName("com.example")).thenReturn(Success("Live Name"))

        val action = ActionData.App(packageName = "com.example", savedAppName = "Saved Name")

        assertThat(helper.getTitle(action, showDeviceDescriptors = false), `is`("Live Name"))
    }

    @Test
    fun `open app title falls back to the saved app name when the app is uninstalled`() {
        whenever(mockUseCase.getAppName("com.example"))
            .thenReturn(KMError.AppNotFound("com.example"))

        val action = ActionData.App(packageName = "com.example", savedAppName = "Saved Name")

        assertThat(helper.getTitle(action, showDeviceDescriptors = false), `is`("Saved Name"))
    }

    @Test
    fun `open app title falls back to the raw package name when no name was ever saved`() {
        whenever(mockUseCase.getAppName("com.example"))
            .thenReturn(KMError.AppNotFound("com.example"))

        val action = ActionData.App(packageName = "com.example", savedAppName = null)

        assertThat(helper.getTitle(action, showDeviceDescriptors = false), `is`("com.example"))
    }
}
