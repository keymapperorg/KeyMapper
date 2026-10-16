package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.settings.HomeAssistantConnectionDialog
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.network.HomeAssistantEntity
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeAssistantConnectionDialogTest {
    private var verifiedConnection: HomeAssistantConnection? = null
    private var saves = 0
    private var verificationResponse: KMResult<List<HomeAssistantEntity>> = Success(emptyList())
    private var verificationGate: CompletableDeferred<KMResult<List<HomeAssistantEntity>>>? = null

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun showConnectionSettings() {
        compose.setContent {
            KeyMapperTheme {
                Column {
                    OutlinedTextField(
                        modifier = Modifier.testTag("background"),
                        value = "Settings screen",
                        onValueChange = {},
                    )
                    HomeAssistantConnectionDialog(
                        connection = HomeAssistantConnection("https://example.test", "test-token"),
                        onSave = { saves++ },
                        onDismiss = {},
                        onVerify = {
                            verifiedConnection = it
                            verificationGate?.await() ?: verificationResponse
                        },
                    )
                }
            }
        }
        compose.onNodeWithText("Server URL")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    }

    @Test
    fun remoteMovesBetweenConnectionFieldsWithoutFocusingBackground() {
        compose.onNodeWithText("Server URL").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Long-Lived Access Token").assertIsFocused()
        compose.onNodeWithText("Long-Lived Access Token").performKeyInput {
            pressKey(Key.DirectionUp)
        }
        compose.onNodeWithText("Server URL").assertIsFocused()
        compose.onNodeWithTag("background").assertIsNotFocused()
    }

    @Test
    fun keyboardNextMovesToToken() {
        compose.onNodeWithText("Server URL").performImeAction()
        compose.onNodeWithText("Long-Lived Access Token").assertIsFocused()
        compose.onNodeWithTag("background").assertIsNotFocused()
    }

    @Test
    fun invalidConnectionCannotBeSaved() {
        compose.onNodeWithText("Server URL").performTextReplacement("")
        compose.onNodeWithText("Done").assertIsNotEnabled()
        compose.onNodeWithText("Verify connection").assertIsNotEnabled()
    }

    @Test
    fun verifiesUnsavedConnectionWithoutSavingIt() {
        compose.onNodeWithText("Server URL").performTextReplacement(" https://other.test/ ")
        compose.onNodeWithText("Verify connection").performClick()
        compose.onNodeWithText("Connection verified. Entities available: 0").assertExists()
        compose.runOnIdle {
            assertEquals(
                HomeAssistantConnection("https://other.test", "test-token"),
                verifiedConnection,
            )
            assertEquals(0, saves)
        }
    }

    @Test
    fun failedVerificationIsShownAndClearedWhenInputChanges() {
        verificationResponse = KMError.UnknownIOError
        compose.onNodeWithText("Verify connection").performClick()
        val failure = "Verification failed. Check the URL, access token, and network connection."
        compose.onNodeWithText(failure).assertExists()
        compose.onNodeWithText("Server URL").performTextReplacement("https://other.test")
        compose.onNodeWithText(failure).assertDoesNotExist()
    }

    @Test
    fun editingDuringVerificationCancelsItAndDiscardsOldResult() {
        val gate = CompletableDeferred<KMResult<List<HomeAssistantEntity>>>()
        verificationGate = gate
        compose.onNodeWithText("Verify connection").performClick()
        compose.onNodeWithText("Verifying…").assertIsNotEnabled()
        compose.onNodeWithText("Done").assertIsNotEnabled()
        compose.onNodeWithText("Server URL").performTextReplacement("https://other.test")
        gate.complete(Success(emptyList()))
        compose.onNodeWithText("Verify connection").assertIsEnabled()
        compose.onNodeWithText("Connection verified. Entities available: 0").assertDoesNotExist()
    }
}
