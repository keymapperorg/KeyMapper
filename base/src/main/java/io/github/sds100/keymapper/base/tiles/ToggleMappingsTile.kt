package io.github.sds100.keymapper.base.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.UseCases
import AccessibilityServiceState
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.base.utils.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.utils.str
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine



@RequiresApi(Build.VERSION_CODES.N)
class ToggleMappingsTile :
    TileService(),
    LifecycleOwner {

    private val serviceAdapter by lazy { ServiceLocator.accessibilityServiceAdapter(this) }
    private val useCase by lazy { UseCases.pauseKeyMaps(this) }

    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)

        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        launchRepeatOnLifecycle(Lifecycle.State.STARTED) {
            combine(serviceAdapter.state, useCase.isPaused) { serviceState, isPaused ->
                qsTile ?: return@combine

                val ctx = this@ToggleMappingsTile

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    updateQsTile(serviceState, ctx, isPaused)
                } else {
                    updateQsTilePreSdk29(serviceState, ctx, isPaused)
                }
            }.collect()
        }
    }

    private fun updateQsTilePreSdk29(
        serviceState: AccessibilityServiceState,
        ctx: ToggleMappingsTile,
        isPaused: Boolean,
    ) {
        when {
            serviceState == AccessibilityServiceState.DISABLED -> {
                qsTile.label = str(R.string.tile_service_disabled)
                qsTile.contentDescription =
                    str(R.string.tile_accessibility_service_disabled_content_description)
                qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_error)
                qsTile.state = Tile.STATE_UNAVAILABLE
            }

            isPaused -> {
                qsTile.label = str(R.string.tile_resume_title)
                qsTile.contentDescription = str(R.string.tile_resume_title)
                qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_pause)
                qsTile.state = Tile.STATE_INACTIVE
            }

            !isPaused -> {
                qsTile.label = str(R.string.tile_pause_title)
                qsTile.contentDescription = str(R.string.tile_pause_title)
                qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_resume)
                qsTile.state = Tile.STATE_ACTIVE
            }
        }

        qsTile.updateTile()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateQsTile(
        serviceState: AccessibilityServiceState,
        ctx: ToggleMappingsTile,
        isPaused: Boolean,
    ) {
        when {
            serviceState == AccessibilityServiceState.DISABLED -> {
                qsTile.label = str(R.string.app_name)
                qsTile.subtitle = str(R.string.tile_service_disabled)
                qsTile.contentDescription =
                    str(R.string.tile_accessibility_service_disabled_content_description)
                qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_error)
                qsTile.state = Tile.STATE_UNAVAILABLE
            }

            isPaused -> {
                qsTile.label = str(R.string.app_name)
                qsTile.subtitle = str(R.string.tile_paused_subtitle)
                qsTile.contentDescription = str(R.string.tile_resume_title)
                qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_pause)
                qsTile.state = Tile.STATE_INACTIVE
            }

            !isPaused -> {
                qsTile.label = str(R.string.app_name)
                qsTile.subtitle = str(R.string.tile_running_subtitle)
                qsTile.contentDescription = str(R.string.tile_pause_title)
                qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_resume)
                qsTile.state = Tile.STATE_ACTIVE
            }
        }

        qsTile.updateTile()
    }

    override fun onStartListening() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        super.onStartListening()
    }

    override fun onStopListening() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onStopListening()
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()

        if (serviceAdapter.state.value == AccessibilityServiceState.DISABLED) return

        if (useCase.isPaused.firstBlocking()) {
            useCase.resume()
        } else {
            useCase.pause()
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
