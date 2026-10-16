package io.github.sds100.keymapper.base.actions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television")
@OptIn(ExperimentalMaterial3Api::class)
class HomeAssistantTvFocusTest {
    @get:Rule
    val compose = createComposeRule()

    private var saved = 0
    private var action by mutableStateOf(
        ActionData.HomeAssistantService(
            description = "Light",
            serverUrl = "https://example.test",
            accessToken = "test-token",
            domain = "light",
            service = "toggle",
            entityId = "light.living_room",
            serviceData = "",
        ),
    )

    @Before
    fun showAction() {
        compose.setContent {
            KeyMapperTheme {
                HomeAssistantBottomSheetContent(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    state = action,
                    connectionConfigured = true,
                    onServiceChanged = { action = action.copy(service = it) },
                    onDoneClick = { saved++ },
                    onDismissRequest = {},
                )
            }
        }
    }

    @Test
    fun focusingDoesNotEditUntilOkAndLeavingStopsEditing() {
        compose.onNodeWithText("Description")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .assert(hasSetTextAction().not())
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assert(hasSetTextAction())
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Edit connection").assertIsFocused()
        compose.onNodeWithText("Description").assert(hasSetTextAction().not())
    }

    @Test
    fun selectingServiceRestoresFocusAndRemoteCanReachDone() {
        compose.onNodeWithText("Service: Toggle")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText("Turn on")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText("Service: Turn on").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Service").assertIsFocused()
            .assert(hasSetTextAction().not())
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Service data JSON (optional)").assertIsFocused()
            .assert(hasSetTextAction().not())
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Done").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, saved) }
    }
}
