package io.github.sds100.keymapper.system.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.inputmethod.ToggleCompatibleImeUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.first
import splitties.toast.toast
import javax.inject.Inject

/**
 * Created by sds100 on 12/06/2020.
 */
@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class ToggleKeyMapperKeyboardTile : TileService(), LifecycleOwner {

    @Inject
    lateinit var useCase: ToggleCompatibleImeUseCase

    @Inject
    lateinit var resourceProvider: ResourceProvider

    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)

        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        launchRepeatOnLifecycle(Lifecycle.State.STARTED) {
            qsTile?.let { tile ->
                tile.icon = Icon.createWithResource(
                    this@ToggleKeyMapperKeyboardTile,
                    R.drawable.ic_tile_keyboard
                )
                tile.label = str(R.string.tile_toggle_keymapper_keyboard)
                tile.contentDescription = str(R.string.tile_toggle_keymapper_keyboard)
                tile.state = Tile.STATE_INACTIVE

                tile.updateTile()
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
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

        lifecycleScope.launchWhenStarted {
            if (!useCase.sufficientPermissions.first()) {
                toast(R.string.error_insufficient_permissions)
                return@launchWhenStarted
            }

            useCase.toggle().onSuccess {
                toast(resourceProvider.getString(R.string.toast_chose_keyboard, it.label))

            }.onFailure {
                toast(it.getFullMessage(resourceProvider))
            }
        }
    }

    override fun getLifecycle() = lifecycleRegistry
}