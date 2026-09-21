package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ConstraintErrorSnapshotTest {

    private lateinit var mockPackageManagerAdapter: PackageManagerAdapter
    private lateinit var mockPermissionAdapter: PermissionAdapter
    private lateinit var mockInputMethodAdapter: InputMethodAdapter
    private lateinit var snapshot: ConstraintErrorSnapshot

    @Before
    fun init() {
        mockPackageManagerAdapter = mock()
        mockPermissionAdapter = mock()
        mockInputMethodAdapter = mock()
        val mockSystemFeatureAdapter: SystemFeatureAdapter = mock()
        val mockCameraAdapter: CameraAdapter = mock()

        snapshot = LazyConstraintErrorSnapshot(
            mockPackageManagerAdapter,
            mockPermissionAdapter,
            mockSystemFeatureAdapter,
            mockInputMethodAdapter,
            mockCameraAdapter,
        )
    }

    private fun setAppMissing(packageName: String) {
        whenever(mockPackageManagerAdapter.isAppEnabled(packageName)).thenReturn(Success(true))
        whenever(mockPackageManagerAdapter.isAppInstalled(packageName)).thenReturn(false)
    }

    private fun setAppDisabled(packageName: String) {
        // getAppError returns as soon as it finds the app is disabled, without checking
        // whether it's installed, so isAppInstalled is never called for a disabled app.
        whenever(mockPackageManagerAdapter.isAppEnabled(packageName)).thenReturn(Success(false))
    }

    @Test
    fun `negated app in foreground constraint has no error when app is uninstalled`() {
        setAppMissing("com.uninstalled")

        val constraint = Constraint(
            data = ConstraintData.AppInForeground("com.uninstalled"),
            isNot = true,
        )

        assertThat(snapshot.getError(constraint), `is`(nullValue()))
    }

    @Test
    fun `negated app in foreground constraint has no error when app is disabled`() {
        setAppDisabled("com.disabled")

        val constraint = Constraint(
            data = ConstraintData.AppInForeground("com.disabled"),
            isNot = true,
        )

        assertThat(snapshot.getError(constraint), `is`(nullValue()))
    }

    @Test
    fun `non-negated app in foreground constraint still errors when app is uninstalled`() {
        setAppMissing("com.uninstalled")

        val constraint = Constraint(data = ConstraintData.AppInForeground("com.uninstalled"))

        assertThat(
            snapshot.getError(constraint),
            `is`(KMError.AppNotFound("com.uninstalled") as KMError?),
        )
    }

    @Test
    fun `non-negated app in foreground constraint still errors when app is disabled`() {
        setAppDisabled("com.disabled")

        val constraint = Constraint(data = ConstraintData.AppInForeground("com.disabled"))

        assertThat(
            snapshot.getError(constraint),
            `is`(KMError.AppDisabled("com.disabled") as KMError?),
        )
    }

    @Test
    fun `app not found error includes the saved app name when it is available`() {
        setAppMissing("com.uninstalled")

        val constraint = Constraint(
            data = ConstraintData.AppInForeground("com.uninstalled", appName = "Example"),
        )

        assertThat(
            snapshot.getError(constraint),
            `is`(KMError.AppNotFound("com.uninstalled", "Example") as KMError?),
        )
    }
}
