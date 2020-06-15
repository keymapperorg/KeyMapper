package io.github.sds100.keymapper.service.tiles

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
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.AccessibilityUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 12/06/2020.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ToggleKeymapsTile : TileService(), LifecycleOwner {

    private val mLifecycleRegistry = LifecycleRegistry(this)

    private val mState: State
        get() = when {
            !AccessibilityUtils.isServiceEnabled(this) -> State.DISABLED

            else -> if (MyAccessibilityService.provideIsPaused().value == true) {
                State.PAUSED
            } else {
                State.RESUMED
            }
        }

    override fun onCreate() {

        mLifecycleRegistry.currentState = Lifecycle.State.STARTED

        MyAccessibilityService.provideIsPaused().observe(this) {
            invalidateTile()
        }

        MyAccessibilityService.provideBus().observe(this) {
            if (it?.peekContent()?.first == MyAccessibilityService.EVENT_ON_SERVICE_STARTED
                || it?.peekContent()?.first == MyAccessibilityService.EVENT_ON_SERVICE_STOPPED) {
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

        if (!AccessibilityUtils.isServiceEnabled(this)) {
            return
        }

        MyAccessibilityService.provideIsPaused().value = mState != State.PAUSED
    }

    private fun invalidateTile() {
        qsTile ?: return

        when (mState) {
            State.PAUSED -> {
                qsTile.label = str(R.string.tile_resume)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_resume)
                qsTile.state = Tile.STATE_ACTIVE
            }

            State.RESUMED -> {
                qsTile.label = str(R.string.tile_pause)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_pause)
                qsTile.state = Tile.STATE_INACTIVE
            }

            State.DISABLED -> {
                qsTile.label = str(R.string.tile_service_disabled)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_error)
                qsTile.state = Tile.STATE_UNAVAILABLE
            }
        }

        qsTile.updateTile()
    }

    override fun getLifecycle() = mLifecycleRegistry

    private enum class State {
        PAUSED, RESUMED, DISABLED
    }
}