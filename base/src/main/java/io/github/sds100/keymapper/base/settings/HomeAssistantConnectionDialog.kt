package io.github.sds100.keymapper.base.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.HomeAssistantConnection
import io.github.sds100.keymapper.base.actions.remoteFieldNavigation
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.RemoteOutlinedTextField
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.network.HomeAssistantEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
internal fun HomeAssistantConnectionDialog(
    connection: HomeAssistantConnection?,
    onSave: (HomeAssistantConnection) -> Unit,
    onDismiss: () -> Unit,
    onVerify: (suspend (HomeAssistantConnection) -> KMResult<List<HomeAssistantEntity>>)? = null,
) {
    val scope = rememberCoroutineScope()
    var verificationJob by remember { mutableStateOf<Job?>(null) }
    var verifying by remember { mutableStateOf(false) }
    var verificationResult by remember {
        mutableStateOf<KMResult<List<HomeAssistantEntity>>?>(null)
    }

    fun resetVerification() {
        verificationJob?.cancel()
        verifying = false
        verificationResult = null
    }

    var saveFailed by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf(connection?.serverUrl ?: "") }
    var token by remember { mutableStateOf(connection?.accessToken ?: "") }
    Dialog(onDismissRequest = onDismiss) {
        // Resolve focus inside this dialog's window, not the underlying screen.
        val focusManager = LocalFocusManager.current
        val next = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
        Surface {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.home_assistant_settings_title))
                Text(stringResource(R.string.home_assistant_settings_summary))
                RemoteOutlinedTextField(
                    modifier = Modifier.fillMaxWidth().remoteFieldNavigation(focusManager),
                    value = url,
                    onValueChange = {
                        resetVerification()
                        url = it
                    },
                    label = { Text(stringResource(R.string.action_ha_server_url_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = next,
                )
                RemoteOutlinedTextField(
                    modifier = Modifier.fillMaxWidth().remoteFieldNavigation(focusManager),
                    value = token,
                    onValueChange = {
                        resetVerification()
                        token = it
                    },
                    label = { Text(stringResource(R.string.action_ha_access_token_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = next,
                )
                TextButton(
                    enabled = !verifying &&
                        onVerify != null &&
                        url.trim().toHttpUrlOrNull() != null &&
                        token.isNotBlank(),
                    onClick = {
                        val verify = onVerify ?: return@TextButton
                        val draft = HomeAssistantConnection(url.trim().trimEnd('/'), token.trim())
                        verifying = true
                        verificationResult = null
                        verificationJob = scope.launch {
                            val result = try {
                                verify(draft)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                KMError.UnknownIOError
                            }
                            ensureActive()
                            verificationResult = result
                            verifying = false
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            if (verifying) {
                                R.string.home_assistant_verifying
                            } else {
                                R.string.home_assistant_verify
                            },
                        ),
                    )
                }
                when (val result = verificationResult) {
                    is Success -> Text(
                        stringResource(R.string.home_assistant_verify_success, result.value.size),
                    )
                    is KMError -> Text(
                        stringResource(R.string.home_assistant_verify_failure),
                        color = MaterialTheme.colorScheme.error,
                    )
                    null -> Unit
                }
                if (saveFailed) {
                    Text(stringResource(R.string.home_assistant_save_error))
                }
                TextButton(
                    enabled =
                    !verifying && url.trim().toHttpUrlOrNull() != null && token.isNotBlank(),
                    onClick = {
                        saveFailed =
                            runCatching { onSave(HomeAssistantConnection(url, token)) }.isFailure
                    },
                ) {
                    Text(stringResource(R.string.pos_done))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.neg_cancel))
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewHomeAssistantConnectionDialog() {
    KeyMapperTheme {
        HomeAssistantConnectionDialog(
            connection = HomeAssistantConnection("http://homeassistant.local:8123", ""),
            onSave = {},
            onDismiss = {},
        )
    }
}
