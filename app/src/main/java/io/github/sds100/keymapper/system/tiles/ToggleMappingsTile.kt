package io.github.sds100.keymapper.system.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.UseCases
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

/**
 * Created by sds100 on 12/06/2020.
 */

@RequiresApi(Build.VERSION_CODES.N)
class ToggleMappingsTile : TileService(), LifecycleOwner {

    private val serviceAdapter by lazy { ServiceLocator.accessibilityServiceAdapter(this) }
    private val useCase by lazy { UseCases.pauseMappings(this) }

    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)

        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        launchRepeatOnLifecycle(Lifecycle.State.STARTED) {
            combine(serviceAdapter.state, useCase.isPaused) { serviceState, isPaused ->
                qsTile ?: return@combine

                val ctx = this@ToggleMappingsTile

                when {
                    serviceState == ServiceState.DISABLED -> {
                        qsTile.label = str(R.string.tile_service_disabled)
                        qsTile.contentDescription =
                            str(R.string.tile_accessibility_service_disabled_content_description)
                        qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_error)
                        qsTile.state = Tile.STATE_UNAVAILABLE
                    }

                    isPaused -> {
                        qsTile.label = str(R.string.tile_resume)
                        qsTile.contentDescription = str(R.string.tile_resume)
                        qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_resume)
                        qsTile.state = Tile.STATE_ACTIVE
                    }

                    !isPaused -> {
                        qsTile.label = str(R.string.tile_pause)
                        qsTile.contentDescription = str(R.string.tile_pause)
                        qsTile.icon = Icon.createWithResource(ctx, R.drawable.ic_tile_pause)
                        qsTile.state = Tile.STATE_INACTIVE
                    }
                }

                qsTile.updateTile()
            }.collect()
        }
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

        if (serviceAdapter.state.value == ServiceState.DISABLED) return

        if (useCase.isPaused.firstBlocking()) {
            useCase.resume()
        } else {
            useCase.pause()
        }
    }

    override fun getLifecycle() = lifecycleRegistry
}