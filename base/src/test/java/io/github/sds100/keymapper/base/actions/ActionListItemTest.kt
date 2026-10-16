package io.github.sds100.keymapper.base.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w600dp-h600dp")
class ActionListItemTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun remoteCanReachTestBetweenDuplicateAndSettings() {
        var tested = 0
        var expanded = 0
        compose.setContent {
            KeyMapperTheme {
                ActionListItem(
                    model = ActionListItemModel(
                        id = "light",
                        title = "Living room light",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.Home),
                    ),
                    index = 0,
                    isExpanded = true,
                    isDragging = false,
                    isReorderingEnabled = false,
                    onTestClick = { tested++ },
                    onExpandedChange = { expanded++ },
                )
            }
        }
        compose.onNodeWithContentDescription("Edit").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionLeft) }
        compose.onNodeWithContentDescription("Test").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle {
            assertEquals(1, tested)
            assertEquals(0, expanded)
        }
    }
}
