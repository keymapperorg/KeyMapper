package io.github.sds100.keymapper.screenshots

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.groups.GroupListItemModel
import io.github.sds100.keymapper.base.home.HomeKeyMapListScreen
import io.github.sds100.keymapper.base.home.KeyMapAppBarState
import io.github.sds100.keymapper.base.home.KeyMapList
import io.github.sds100.keymapper.base.home.KeyMapListAppBar
import io.github.sds100.keymapper.base.home.KeyMapListHeader
import io.github.sds100.keymapper.base.home.SelectedKeyMapsEnabled
import io.github.sds100.keymapper.base.utils.ui.drawable
import io.github.sds100.keymapper.common.utils.State
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w540dp-h960dp-xxhdpi", sdk = [35])
class Screenshot1 {

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun navigationGroup() {
        captureRoboImage("../fastlane/metadata/android/en-US/images/phoneScreenshots/1.png") {
            val ctx = LocalContext.current
            val maps = ctx.drawable(R.drawable.google_maps)

            val appBarState = KeyMapAppBarState.ChildGroup(
                groupName = "Navigation",
                constraints = listOf(chip("Maps is in foreground", maps)),
                constraintMode = ConstraintMode.AND,
                parentConstraintCount = 0,
                subGroups = emptyList(),
                // The "Home" breadcrumb is always added by the breadcrumb row.
                breadcrumbs = listOf(GroupListItemModel(uid = "0", name = "Navigation")),
                isEditingGroupName = false,
                isNewGroup = false,
                keyMapsEnabled = SelectedKeyMapsEnabled.ALL,
            )

            val keyMaps = listOf(
                keyMap(
                    uid = "0",
                    triggerKeys = listOf("Volume down"),
                    actions = listOf(chip("Answer phone call", Icons.Outlined.Call)),
                    constraints = listOf(chip("Phone ringing", Icons.Outlined.RingVolume)),
                ),
                keyMap(
                    uid = "1",
                    triggerKeys = listOf("Floating button Music (Motorbike 🏍️)"),
                    actions = listOf(
                        chip("Open YouTube Music", ctx.drawable(R.drawable.youtube_music)),
                    ),
                    constraints = listOf(
                        chip("j3r is connected", Icons.Outlined.BluetoothConnected),
                    ),
                ),
                keyMap(
                    uid = "2",
                    triggerKeys = listOf("Side key/power button"),
                    actions = listOf(
                        chip("Driving", maps),
                        chip("Increase display brightness", Icons.Outlined.BrightnessHigh),
                    ),
                ),
            )

            KeyMapperTheme(darkTheme = false) {
                StoreScreenshotFrame(
                    headline = "Change what your buttons do",
                    subtitle = "Or add extra functionality!",
                    statusBarColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    HomeKeyMapListScreen(
                        appBarContent = { KeyMapListAppBar(state = appBarState) },
                        listContent = {
                            KeyMapList(
                                listItems = State.Data(keyMaps),
                                header = {
                                    KeyMapListHeader(state = appBarState, extraContent = {})
                                },
                            )
                        },
                        floatingActionButton = {},
                        selectionBottomSheet = {},
                    )
                }
            }
        }
    }
}
