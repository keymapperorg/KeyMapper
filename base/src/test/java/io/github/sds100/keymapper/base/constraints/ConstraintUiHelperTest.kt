package io.github.sds100.keymapper.base.constraints

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
class ConstraintUiHelperTest {

    private lateinit var mockUseCase: DisplayConstraintUseCase
    private lateinit var helper: ConstraintUiHelper

    @Before
    fun init() {
        mockUseCase = mock()

        val mockResourceProvider: ResourceProvider = mock()
        whenever(mockResourceProvider.getString(any(), any<Any>())).thenAnswer {
            it.getArgument<Any>(1).toString()
        }

        helper = ConstraintUiHelper(mockUseCase, mockResourceProvider)
    }

    @Test
    fun `app in foreground title uses the live app name when the app is installed`() {
        whenever(mockUseCase.getAppName("com.example")).thenReturn(Success("Live Name"))

        val constraint = Constraint(
            data = ConstraintData.AppInForeground("com.example", appName = "Saved Name"),
        )

        assertThat(helper.getTitle(constraint), `is`("Live Name"))
    }

    @Test
    fun `app in foreground title falls back to the saved app name when the app is uninstalled`() {
        whenever(mockUseCase.getAppName("com.example"))
            .thenReturn(KMError.AppNotFound("com.example"))

        val constraint = Constraint(
            data = ConstraintData.AppInForeground("com.example", appName = "Saved Name"),
        )

        assertThat(helper.getTitle(constraint), `is`("Saved Name"))
    }

    @Test
    fun `app in foreground title falls back to the raw package name when no name was ever saved`() {
        whenever(mockUseCase.getAppName("com.example"))
            .thenReturn(KMError.AppNotFound("com.example"))

        val constraint = Constraint(
            data = ConstraintData.AppInForeground("com.example", appName = null),
        )

        assertThat(helper.getTitle(constraint), `is`("com.example"))
    }
}
