package io.github.sds100.keymapper.service.tiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.keymapsPaused
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.AccessibilityUtils
import io.github.sds100.keymapper.util.collectWhenStarted
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 12/06/2020.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ToggleKeymapsTile : TileService(), LifecycleOwner {

    private val state: State
        get() = when {
            !AccessibilityUtils.isServiceEnabled(this) -> State.DISABLED

            else -> if (globalPreferences.keymapsPaused.firstBlocking()) {
                State.PAUSED
            } else {
                State.RESUMED
            }
        }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                MyAccessibilityService.ACTION_ON_START,
                MyAccessibilityService.ACTION_ON_STOP -> invalidateTile()
            }
        }
    }

    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {

        lifecycleRegistry = LifecycleRegistry(this)

        IntentFilter().apply {
            addAction(MyAccessibilityService.ACTION_ON_START)
            addAction(MyAccessibilityService.ACTION_ON_STOP)

            registerReceiver(broadcastReceiver, this)
        }

        invalidateTile()

        lifecycleRegistry.currentState = Lifecycle.State.CREATED

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
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        globalPreferences.keymapsPaused.collectWhenStarted(this, {
            invalidateTile()
        })

        super.onStartListening()
    }

    override fun onStopListening() {

        invalidateTile()

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onStopListening()
    }

    override fun onDestroy() {

        unregisterReceiver(broadcastReceiver)

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()

        if (!AccessibilityUtils.isServiceEnabled(this)) return

        globalPreferences.toggle(Keys.keymapsPaused)

        qsTile?.updateTile()
    }

    private fun invalidateTile() {
        qsTile ?: return

        when (state) {
            State.PAUSED -> {
                qsTile.label = str(R.string.tile_resume)
                qsTile.contentDescription = str(R.string.tile_resume)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_resume)
                qsTile.state = Tile.STATE_ACTIVE
            }

            State.RESUMED -> {
                qsTile.label = str(R.string.tile_pause)
                qsTile.contentDescription = str(R.string.tile_pause)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_pause)
                qsTile.state = Tile.STATE_INACTIVE
            }

            State.DISABLED -> {
                qsTile.label = str(R.string.tile_service_disabled)
                qsTile.contentDescription = str(R.string.tile_accessibility_service_disabled_content_description)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_error)
                qsTile.state = Tile.STATE_UNAVAILABLE
            }
        }

        qsTile.updateTile()
    }

    override fun getLifecycle() = lifecycleRegistry

    private enum class State {
        PAUSED, RESUMED, DISABLED
    }
}