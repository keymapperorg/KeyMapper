package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.camera.CameraLens
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

/**
 * The constraints that were the inverse of another constraint were replaced by the
 * positive constraint with isNot. They are migrated when they are read.
 */
@Suppress("DEPRECATION")
class ConstraintEntityMapperTest {

    companion object {
        private const val UID = "constraint_uid"
    }

    @Test
    fun `app not in foreground is migrated to NOT app in foreground`() {
        val entity = ConstraintEntity(
            UID,
            ConstraintEntity.APP_NOT_FOREGROUND,
            EntityExtra(ConstraintEntity.EXTRA_PACKAGE_NAME, "com.example"),
        )

        assertMigratedToNot(entity, ConstraintData.AppInForeground("com.example"))
    }

    @Test
    fun `app not playing media is migrated to NOT app playing media`() {
        val entity = ConstraintEntity(
            UID,
            ConstraintEntity.APP_NOT_PLAYING_MEDIA,
            EntityExtra(ConstraintEntity.EXTRA_PACKAGE_NAME, "com.example"),
        )

        assertMigratedToNot(entity, ConstraintData.AppPlayingMedia("com.example"))
    }

    @Test
    fun `no media playing is migrated to NOT media playing`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.NO_MEDIA_PLAYING)

        assertMigratedToNot(entity, ConstraintData.MediaPlaying)
    }

    @Test
    fun `bluetooth device disconnected is migrated to NOT bluetooth device connected`() {
        val entity = ConstraintEntity(
            UID,
            ConstraintEntity.BT_DEVICE_DISCONNECTED,
            EntityExtra(ConstraintEntity.EXTRA_BT_ADDRESS, "00:11:22:33:44:55"),
            EntityExtra(ConstraintEntity.EXTRA_BT_NAME, "Headphones"),
        )

        assertMigratedToNot(
            entity,
            ConstraintData.BtDeviceConnected(
                bluetoothAddress = "00:11:22:33:44:55",
                deviceName = "Headphones",
            ),
        )
    }

    @Test
    fun `screen off is migrated to NOT screen on`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.SCREEN_OFF)

        assertMigratedToNot(entity, ConstraintData.ScreenOn)
    }

    @Test
    fun `back flashlight off is migrated to NOT back flashlight on`() {
        val entity = ConstraintEntity(
            UID,
            ConstraintEntity.FLASHLIGHT_OFF,
            EntityExtra(ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS, "option_lens_back"),
        )

        assertMigratedToNot(entity, ConstraintData.FlashlightOn(CameraLens.BACK))
    }

    @Test
    fun `front flashlight off is migrated to NOT front flashlight on`() {
        val entity = ConstraintEntity(
            UID,
            ConstraintEntity.FLASHLIGHT_OFF,
            EntityExtra(ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS, "option_lens_front"),
        )

        assertMigratedToNot(entity, ConstraintData.FlashlightOn(CameraLens.FRONT))
    }

    @Test
    fun `wifi off is migrated to NOT wifi on`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.WIFI_OFF)

        assertMigratedToNot(entity, ConstraintData.WifiOn)
    }

    @Test
    fun `wifi disconnected from a network is migrated to NOT wifi connected to the network`() {
        val entity = ConstraintEntity(
            UID,
            ConstraintEntity.WIFI_DISCONNECTED,
            EntityExtra(ConstraintEntity.EXTRA_SSID, "home"),
        )

        assertMigratedToNot(entity, ConstraintData.WifiConnected(ssid = "home"))
    }

    @Test
    fun `wifi disconnected from any network is migrated to NOT wifi connected to any network`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.WIFI_DISCONNECTED)

        assertMigratedToNot(entity, ConstraintData.WifiConnected(ssid = null))
    }

    @Test
    fun `input method not chosen is migrated to NOT input method chosen`() {
        val entity = ConstraintEntity(
            UID,
            ConstraintEntity.IME_NOT_CHOSEN,
            EntityExtra(ConstraintEntity.EXTRA_IME_ID, "ime_id"),
            EntityExtra(ConstraintEntity.EXTRA_IME_LABEL, "Gboard"),
        )

        assertMigratedToNot(entity, ConstraintData.ImeChosen(imeId = "ime_id", imeLabel = "Gboard"))
    }

    @Test
    fun `keyboard not showing is migrated to NOT keyboard showing`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.KEYBOARD_NOT_SHOWING)

        assertMigratedToNot(entity, ConstraintData.KeyboardShowing)
    }

    @Test
    fun `device is unlocked is migrated to NOT device is locked`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.DEVICE_IS_UNLOCKED)

        assertMigratedToNot(entity, ConstraintData.DeviceIsLocked)
    }

    @Test
    fun `lock screen not showing is migrated to NOT lock screen showing`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.LOCK_SCREEN_NOT_SHOWING)

        assertMigratedToNot(entity, ConstraintData.LockScreenShowing)
    }

    @Test
    fun `discharging is migrated to NOT charging`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.DISCHARGING)

        assertMigratedToNot(entity, ConstraintData.Charging)
    }

    @Test
    fun `notification panel not showing is migrated to NOT notification panel showing`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.NOTIFICATION_PANEL_NOT_SHOWING)

        assertMigratedToNot(entity, ConstraintData.NotificationPanelShowing)
    }

    @Test
    fun `hinge open is not migrated because it is not the inverse of hinge closed`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.HINGE_OPEN)

        assertThat(
            ConstraintEntityMapper.fromEntity(entity),
            `is`(Constraint(uid = UID, data = ConstraintData.HingeOpen, isNot = false)),
        )
    }

    @Test
    fun `not in phone call is not migrated because it is not the inverse of in phone call`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.NOT_IN_PHONE_CALL)

        assertThat(
            ConstraintEntityMapper.fromEntity(entity),
            `is`(Constraint(uid = UID, data = ConstraintData.NotInPhoneCall, isNot = false)),
        )
    }

    @Test
    fun `constraint with isNot false is not inverted`() {
        val entity = ConstraintEntity(UID, ConstraintEntity.SCREEN_ON)

        assertThat(
            ConstraintEntityMapper.fromEntity(entity),
            `is`(Constraint(uid = UID, data = ConstraintData.ScreenOn, isNot = false)),
        )
    }

    @Test
    fun `constraint with isNot true is inverted`() {
        val entity = ConstraintEntity(
            type = ConstraintEntity.SCREEN_ON,
            extras = emptyList(),
            uid = UID,
            isNot = true,
        )

        assertThat(
            ConstraintEntityMapper.fromEntity(entity),
            `is`(Constraint(uid = UID, data = ConstraintData.ScreenOn, isNot = true)),
        )
    }

    @Test
    fun `deprecated constraint type with isNot true becomes the positive constraint`() {
        val entity = ConstraintEntity(
            type = ConstraintEntity.SCREEN_OFF,
            extras = emptyList(),
            uid = UID,
            isNot = true,
        )

        assertThat(
            ConstraintEntityMapper.fromEntity(entity),
            `is`(Constraint(uid = UID, data = ConstraintData.ScreenOn, isNot = false)),
        )
    }

    @Test
    fun `save NOT constraint with the positive type and isNot true`() {
        val constraint = Constraint(
            uid = UID,
            data = ConstraintData.AppInForeground("com.example"),
            isNot = true,
        )

        val entity = ConstraintEntityMapper.toEntity(constraint)

        assertThat(entity.type, `is`(ConstraintEntity.APP_FOREGROUND))
        assertThat(entity.isNot, `is`(true))
        assertThat(
            entity.extras.getData(ConstraintEntity.EXTRA_PACKAGE_NAME).valueOrNull(),
            `is`("com.example"),
        )
    }

    @Test
    fun `isNot is false if the constraint is not inverted`() {
        val constraint = Constraint(uid = UID, data = ConstraintData.ScreenOn)

        val entity = ConstraintEntityMapper.toEntity(constraint)

        assertThat(entity.isNot, `is`(false))
    }

    @Test
    fun `migrated constraints are saved with the positive type and loaded the same`() {
        val deprecatedEntities = listOf(
            ConstraintEntity(
                UID,
                ConstraintEntity.APP_NOT_FOREGROUND,
                EntityExtra(ConstraintEntity.EXTRA_PACKAGE_NAME, "com.example"),
            ),
            ConstraintEntity(
                UID,
                ConstraintEntity.APP_NOT_PLAYING_MEDIA,
                EntityExtra(ConstraintEntity.EXTRA_PACKAGE_NAME, "com.example"),
            ),
            ConstraintEntity(UID, ConstraintEntity.NO_MEDIA_PLAYING),
            ConstraintEntity(
                UID,
                ConstraintEntity.BT_DEVICE_DISCONNECTED,
                EntityExtra(ConstraintEntity.EXTRA_BT_ADDRESS, "00:11:22:33:44:55"),
                EntityExtra(ConstraintEntity.EXTRA_BT_NAME, "Headphones"),
            ),
            ConstraintEntity(UID, ConstraintEntity.SCREEN_OFF),
            ConstraintEntity(
                UID,
                ConstraintEntity.FLASHLIGHT_OFF,
                EntityExtra(ConstraintEntity.EXTRA_FLASHLIGHT_CAMERA_LENS, "option_lens_back"),
            ),
            ConstraintEntity(UID, ConstraintEntity.WIFI_OFF),
            ConstraintEntity(
                UID,
                ConstraintEntity.WIFI_DISCONNECTED,
                EntityExtra(ConstraintEntity.EXTRA_SSID, "home"),
            ),
            ConstraintEntity(
                UID,
                ConstraintEntity.IME_NOT_CHOSEN,
                EntityExtra(ConstraintEntity.EXTRA_IME_ID, "ime_id"),
                EntityExtra(ConstraintEntity.EXTRA_IME_LABEL, "Gboard"),
            ),
            ConstraintEntity(UID, ConstraintEntity.KEYBOARD_NOT_SHOWING),
            ConstraintEntity(UID, ConstraintEntity.DEVICE_IS_UNLOCKED),
            ConstraintEntity(UID, ConstraintEntity.LOCK_SCREEN_NOT_SHOWING),
            ConstraintEntity(UID, ConstraintEntity.DISCHARGING),
            ConstraintEntity(UID, ConstraintEntity.NOTIFICATION_PANEL_NOT_SHOWING),
        )

        for (deprecatedEntity in deprecatedEntities) {
            val migrated = ConstraintEntityMapper.fromEntity(deprecatedEntity)
            val savedEntity = ConstraintEntityMapper.toEntity(migrated)

            assertThat(
                "${deprecatedEntity.type} must not be saved with a deprecated type",
                savedEntity.type == deprecatedEntity.type,
                `is`(false),
            )
            assertThat(ConstraintEntityMapper.fromEntity(savedEntity), `is`(migrated))
        }
    }

    private fun assertMigratedToNot(entity: ConstraintEntity, expectedData: ConstraintData) {
        assertThat(
            ConstraintEntityMapper.fromEntity(entity),
            `is`(Constraint(uid = UID, data = expectedData, isNot = true)),
        )
    }
}
