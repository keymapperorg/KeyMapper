package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalMaterial3Api::class)
class HomeAssistantFocusTest {
    private var connectionConfigured by mutableStateOf(false)

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun showSheetOverFocusableScreen() {
        compose.setContent {
            KeyMapperTheme {
                Column {
                    OutlinedTextField(
                        modifier = Modifier.testTag("background"),
                        value = "Background screen",
                        onValueChange = {},
                    )
                    HomeAssistantBottomSheetContent(
                        connectionConfigured = connectionConfigured,
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        onDismissRequest = {},
                        state = ActionData.HomeAssistantService(
                            description = "Living room",
                            serverUrl = "http://homeassistant.local:8123",
                            accessToken = "test-token",
                            domain = "light",
                            service = "toggle",
                            entityId = "light.living_room",
                            serviceData = "",
                        ),
                    )
                }
            }
        }
        compose.onNodeWithText("Description")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText("Description").assertIsFocused()
    }

    @Test
    fun configuredConnectionHidesCredentialsAndKeepsNavigationInsideSheet() {
        compose.runOnIdle { connectionConfigured = true }
        compose.onNodeWithText("Server URL").assertDoesNotExist()
        compose.onNodeWithText("Long-Lived Access Token").assertDoesNotExist()
        compose.onNodeWithText("Description").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Edit connection").assertIsFocused()
        compose.onNodeWithTag("background").assertIsNotFocused()
    }

    @Test
    fun downMovesThroughFieldsInsideSheet() {
        compose.onNodeWithText("Description").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Server URL").assertIsFocused()
        compose.onNodeWithText("Server URL").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Long-Lived Access Token").assertIsFocused()
        compose.onNodeWithTag("background").assertIsNotFocused()
    }

    @Test
    fun upReturnsToDescriptionInsideSheet() {
        compose.onNodeWithText("Server URL")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText("Server URL").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithText("Description").assertIsFocused()
        compose.onNodeWithTag("background").assertIsNotFocused()
    }

    @Test
    fun keyboardNextMovesInsideSheet() {
        compose.onNodeWithText("Description").performImeAction()
        compose.onNodeWithText("Server URL").assertIsFocused()
        compose.onNodeWithTag("background").assertIsNotFocused()
    }
}
