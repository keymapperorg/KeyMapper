package io.github.sds100.keymapper.base.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.inputmethod.ToggleCompatibleImeUseCase
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.utils.ui.str
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class ToggleKeyMapperKeyboardTile :
    TileService(),
    LifecycleOwner {

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
                    R.drawable.ic_tile_keyboard,
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

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()

        lifecycleScope.launchWhenStarted {
            if (!useCase.sufficientPermissions.first()) {
                Toast.makeText(
                    this@ToggleKeyMapperKeyboardTile,
                    R.string.error_insufficient_permissions,
                    Toast.LENGTH_SHORT,
                ).show()
                return@launchWhenStarted
            }

            useCase.toggle().onSuccess {
                Toast.makeText(
                    this@ToggleKeyMapperKeyboardTile,
                    str(R.string.toast_chose_keyboard, it.label),
                    Toast.LENGTH_SHORT,
                ).show()
            }.onFailure {
                Toast.makeText(
                    this@ToggleKeyMapperKeyboardTile,
                    it.getFullMessage(resourceProvider),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
