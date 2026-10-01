package io.github.sds100.keymapper.screenshots

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.groups.GroupListItemModel
import io.github.sds100.keymapper.base.home.HomeKeyMapListScreen
import io.github.sds100.keymapper.base.home.KeyMapAppBarState
import io.github.sds100.keymapper.base.home.KeyMapList
import io.github.sds100.keymapper.base.home.KeyMapListAppBar
import io.github.sds100.keymapper.base.home.KeyMapListHeader
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.base.utils.ui.compose.CollapsableFloatingActionButton
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.common.utils.State
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h800dp-xxhdpi", sdk = [35])
class KeyMapListScreenshot {

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun keyMapList() {
        val appBarState = KeyMapAppBarState.RootGroup(
            subGroups = listOf(
                GroupListItemModel(
                    uid = "0",
                    name = "Navigation",
                    icon = ComposeIconInfo.Vector(Icons.Outlined.Map),
                ),
                GroupListItemModel(uid = "1", name = "Lockscreen"),
                GroupListItemModel(uid = "2", name = "Gaming"),
            ),
        )

        captureRoboImage("output/key_map_list.png") {
            KeyMapperTheme {
                HomeKeyMapListScreen(
                    appBarContent = { KeyMapListAppBar(state = appBarState) },
                    listContent = {
                        KeyMapList(
                            listItems = State.Data(keyMaps()),
                            header = {
                                KeyMapListHeader(state = appBarState, extraContent = {})
                            },
                        )
                    },
                    floatingActionButton = {
                        CollapsableFloatingActionButton(text = "New key map", showText = true)
                    },
                    selectionBottomSheet = {},
                )
            }
        }
    }

    private fun keyMaps(): List<KeyMapListItemModel> {
        return listOf(
            keyMap(
                uid = "0",
                triggerKeys = listOf("Ctrl Left (Gaming Keyboard)", "O (Gaming Keyboard)"),
                actions = listOf(
                    chip("Type 'ø'", Icons.AutoMirrored.Outlined.ShortText),
                ),
                constraints = listOf(
                    chip("Messenger is open", Icons.AutoMirrored.Outlined.Message),
                ),
            ),
            keyMap(
                uid = "1",
                triggerKeys = listOf("Long press Back (Wired Mouse)"),
                actions = listOf(chip("Screenshot", Icons.Outlined.Fullscreen)),
                options = listOf("Vibrate"),
            ),
            keyMap(
                uid = "2",
                triggerKeys = listOf("Button A (Wireless Controller)"),
                actions = listOf(
                    chip("Input KEYCODE_SPACE • Repeat", Icons.Rounded.Abc),
                ),
            ),
        )
    }

    private fun keyMap(
        uid: String,
        triggerKeys: List<String>,
        actions: List<ComposeChipModel>,
        constraints: List<ComposeChipModel> = emptyList(),
        options: List<String> = emptyList(),
    ): KeyMapListItemModel {
        return KeyMapListItemModel(
            isSelected = false,
            KeyMapListItemModel.Content(
                uid = uid,
                triggerKeys = triggerKeys,
                triggerSeparatorIcon = Icons.Outlined.Add,
                actions = actions,
                constraintMode = ConstraintMode.AND,
                constraints = constraints,
                options = options,
                isEnabled = true,
                hasError = false,
            ),
        )
    }

    private fun chip(text: String, icon: ImageVector): ComposeChipModel {
        return ComposeChipModel.Normal(
            id = text,
            icon = ComposeIconInfo.Vector(icon),
            text = text,
        )
    }
}
