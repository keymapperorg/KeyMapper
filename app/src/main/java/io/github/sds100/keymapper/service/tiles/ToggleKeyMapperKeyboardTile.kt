package io.github.sds100.keymapper.service.tiles

import android.Manifest
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.observe
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.KeyMapperImeService
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.PermissionUtils
import io.github.sds100.keymapper.util.result.onSuccess
import io.github.sds100.keymapper.util.str
import splitties.toast.toast

/**
 * Created by sds100 on 12/06/2020.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ToggleKeyMapperKeyboardTile : TileService(), LifecycleOwner {

    private val mLifecycleRegistry = LifecycleRegistry(this)

    private val mState: State
        get() = if (KeyMapperImeService.isServiceEnabled()) {
            State.DEFAULT
        } else {
            State.DISABLED
        }

    override fun onCreate() {

        mLifecycleRegistry.currentState = Lifecycle.State.STARTED

        KeyMapperImeService.provideBus().observe(this) {
            if (it?.peekContent()?.first == KeyMapperImeService.EVENT_ON_SERVICE_STARTED
                || it?.peekContent()?.first == KeyMapperImeService.EVENT_ON_SERVICE_STOPPED) {
                invalidateTile()
            }
        }

        invalidateTile()
        super.onCreate()
    }

    override fun onTileAdded() {
        super.onTileAdded()

        invalidateTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()

        invalidateTile()
    }

    override fun onStartListening() {

        invalidateTile()
        super.onStartListening()
    }

    override fun onStopListening() {

        invalidateTile()
        super.onStopListening()
    }

    override fun onDestroy() {
        super.onDestroy()

        mLifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun onClick() {
        super.onClick()

        invalidateTile()

        when (mState) {
            State.DEFAULT ->
                if (KeyMapperImeService.isInputMethodChosen()) {
                    AppPreferences.defaultIme?.let {
                        KeyboardUtils.switchIme(it)

                        KeyboardUtils.getInputMethodLabel(it).onSuccess { imeLabel ->
                            toast(str(R.string.toast_chose_keyboard, imeLabel))
                        }
                    }

                } else {
                    if (PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
                        AppPreferences.defaultIme = KeyboardUtils.getChosenImeId(this)
                        KeyboardUtils.switchToKeyMapperIme(this)
                        toast(R.string.toast_chose_keymapper_keyboard)
                    } else {
                        toast(R.string.error_need_write_secure_settings_permission)
                    }
                }

            State.DISABLED -> {
                KeyboardUtils.enableKeyMapperIme()
            }
        }
    }

    private fun invalidateTile() {
        qsTile ?: return

        when (mState) {
            State.DEFAULT -> {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_keyboard)
                qsTile.label = str(R.string.tile_toggle_keymapper_keyboard)
                qsTile.state = Tile.STATE_INACTIVE
            }

            State.DISABLED -> {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_error)
                qsTile.label = str(R.string.tile_service_disabled)
                qsTile.state = Tile.STATE_INACTIVE
            }
        }

        qsTile.updateTile()
    }

    override fun getLifecycle() = mLifecycleRegistry

    private enum class State {
        DEFAULT, DISABLED
    }
}