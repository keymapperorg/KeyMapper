package io.github.sds100.keymapper.service.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 12/06/2020.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ToggleKeyMapperKeyboardTile : TileService() {

    private val state: State
        get() = if (KeyboardUtils.isCompatibleImeEnabled()) {
            State.DEFAULT
        } else {
            State.DISABLED
        }

    override fun onCreate() {

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

    override fun onClick() {
        super.onClick()

        invalidateTile()

        when (state) {
            State.DEFAULT -> KeyboardUtils.toggleCompatibleIme(this, fromForeground = false)

            State.DISABLED -> {
                KeyboardUtils.enableCompatibleInputMethodsRoot()
            }
        }
    }

    private fun invalidateTile() {
        qsTile ?: return

        when (state) {
            State.DEFAULT -> {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_keyboard)
                qsTile.label = str(R.string.tile_toggle_keymapper_keyboard)
                qsTile.contentDescription = str(R.string.tile_toggle_keymapper_keyboard)
                qsTile.state = Tile.STATE_INACTIVE
            }

            State.DISABLED -> {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_error)
                qsTile.label = str(R.string.tile_service_disabled)
                qsTile.contentDescription = str(R.string.tile_keymapper_keyboard_service_disabled_content_description)
                qsTile.state = Tile.STATE_INACTIVE
            }
        }

        qsTile.updateTile()
    }

    private enum class State {
        DEFAULT, DISABLED
    }
}