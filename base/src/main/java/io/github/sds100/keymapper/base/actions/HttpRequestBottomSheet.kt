package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperDropdownMenu
import io.github.sds100.keymapper.system.network.HttpMethod
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpRequestBottomSheet(delegate: CreateActionDelegate) {
    rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.httpRequestBottomSheetState != null) {
        HttpRequestBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                delegate.httpRequestBottomSheetState = null
            },
            state = delegate.httpRequestBottomSheetState!!,
            onSelectMethod = {
                delegate.httpRequestBottomSheetState =
                    delegate.httpRequestBottomSheetState?.copy(method = it)
            },
            onDescriptionChanged = {
                delegate.httpRequestBottomSheetState =
                    delegate.httpRequestBottomSheetState?.copy(description = it)
            },
            onUrlChanged = {
                delegate.httpRequestBottomSheetState =
                    delegate.httpRequestBottomSheetState?.copy(url = it)
            },
            onBodyChanged = {
                delegate.httpRequestBottomSheetState =
                    delegate.httpRequestBottomSheetState?.copy(body = it)
            },
            onAuthorizationChanged = {
                delegate.httpRequestBottomSheetState =
                    delegate.httpRequestBottomSheetState?.copy(authorizationHeader = it)
            },
            onDoneClick = {
                val result = delegate.httpRequestBottomSheetState ?: return@HttpRequestBottomSheet
                delegate.httpRequestBottomSheetState = null
                delegate.actionResult.update { result }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HttpRequestBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: ActionData.HttpRequest,
    onSelectMethod: (HttpMethod) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
    onUrlChanged: (String) -> Unit = {},
    onBodyChanged: (String) -> Unit = {},
    onAuthorizationChanged: (String) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var methodExpanded by rememberSaveable { mutableStateOf(false) }

    val descriptionEmptyErrorString =
        stringResource(R.string.action_http_request_description_empty_error)
    val urlEmptyErrorString = stringResource(R.string.action_http_request_url_empty_error)
    val malformedUrlErrorString = stringResource(R.string.action_http_request_malformed_url_error)

    var descriptionError: String? by rememberSaveable { mutableStateOf(null) }
    var urlError: String? by rememberSaveable { mutableStateOf(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.action_http_request),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            KeyMapperDropdownMenu(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 16.dp),
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = it },
                label = { Text(stringResource(R.string.action_http_request_method_label)) },
                selectedValue = state.method,
                values = HttpMethod.entries.map { it to it.toString() },
                onValueChanged = onSelectMethod,

            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = state.description,
                label = { Text(stringResource(R.string.action_http_request_description_label)) },
                onValueChange = {
                    descriptionError = null
                    onDescriptionChanged(it)
                },
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = true,
                    keyboardType = KeyboardType.Uri,
                ),
                isError = descriptionError != null,
                supportingText = {
                    if (descriptionError != null) {
                        Text(
                            text = descriptionError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = state.url,
                label = { Text(stringResource(R.string.action_http_request_url_label)) },
                onValueChange = {
                    urlError = null
                    onUrlChanged(it)
                },
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                ),
                isError = urlError != null,
                supportingText = {
                    if (urlError != null) {
                        Text(
                            text = urlError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = state.body,
                label = { Text(stringResource(R.string.action_http_request_body_label)) },
                onValueChange = {
                    onBodyChanged(it)
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = state.authorizationHeader,
                label = { Text(stringResource(R.string.action_http_request_authorization_label)) },
                onValueChange = {
                    onAuthorizationChanged(it)
                },
                supportingText = {
                    Text(stringResource(R.string.action_http_request_authorization_supporting_text))
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.neg_cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (state.description.isBlank()) {
                            descriptionError = descriptionEmptyErrorString
                        }

                        if (state.url.isBlank()) {
                            urlError = urlEmptyErrorString
                        }

                        if (state.url.toHttpUrlOrNull() == null) {
                            urlError = malformedUrlErrorString
                        }

                        if (descriptionError == null && urlError == null) {
                            onDoneClick()
                        }
                    },
                ) {
                    Text(stringResource(R.string.pos_done))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )
        HttpRequestBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ActionData.HttpRequest(
                description = "",
                method = HttpMethod.GET,
                url = "",
                body = "",
                authorizationHeader = "",
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewFilled() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )
        HttpRequestBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ActionData.HttpRequest(
                description = "Example HTTP request",
                method = HttpMethod.GET,
                url = "https://example.com",
                body = "Hello, world!",
                authorizationHeader = "Bearer token",
            ),
        )
    }
}
