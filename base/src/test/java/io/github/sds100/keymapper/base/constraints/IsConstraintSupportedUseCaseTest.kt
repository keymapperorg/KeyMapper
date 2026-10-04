package io.github.sds100.keymapper.base.constraints

import android.content.pm.PackageManager
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IsConstraintSupportedUseCaseTest {

    private lateinit var mockSystemFeatureAdapter: SystemFeatureAdapter
    private lateinit var mockCameraAdapter: CameraAdapter
    private lateinit var useCase: IsConstraintSupportedUseCase

    @Before
    fun init() {
        mockSystemFeatureAdapter = mock()
        mockCameraAdapter = mock()

        useCase = IsConstraintSupportedUseCaseImpl(mockSystemFeatureAdapter, mockCameraAdapter)
    }

    @Test
    fun `bluetooth constraint is unsupported without bluetooth feature`() {
        whenever(mockSystemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            .thenReturn(false)

        assertThat(
            useCase.isSupported(ConstraintId.BT_DEVICE_CONNECTED),
            `is`(KMError.SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)),
        )
    }

    @Test
    fun `bluetooth constraint is supported with bluetooth feature`() {
        whenever(mockSystemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            .thenReturn(true)

        assertThat(useCase.isSupported(ConstraintId.BT_DEVICE_CONNECTED), `is`(nullValue()))
    }

    @Test
    fun `hinge constraint is unsupported without hinge sensor`() {
        whenever(
            mockSystemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE),
        ).thenReturn(false)

        assertThat(
            useCase.isSupported(ConstraintId.HINGE_OPEN),
            `is`(KMError.SystemFeatureNotSupported(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)),
        )
    }

    @Test
    fun `flashlight constraint is unsupported without any flash`() {
        assertThat(
            useCase.isSupported(ConstraintId.FLASHLIGHT_ON),
            `is`(KMError.SystemFeatureNotSupported(PackageManager.FEATURE_CAMERA_FLASH)),
        )
    }

    @Test
    fun `constraint with no hardware requirement is supported`() {
        assertThat(useCase.isSupported(ConstraintId.SCREEN_ON), `is`(nullValue()))
    }
}
