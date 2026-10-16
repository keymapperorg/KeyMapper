package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.settings.HomeAssistantConnectionDialog
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
import io.github.sds100.keymapper.base.utils.ui.compose.RemoteOutlinedTextField
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.network.HomeAssistantEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAssistantBottomSheet(delegate: CreateActionDelegate) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val savedConnection by delegate.homeAssistantConnection.collectAsStateWithLifecycle(
        initialValue = null,
    )
    var connectionSaveFailed by remember { mutableStateOf(false) }
    var showConnectionSettings by remember { mutableStateOf(false) }

    if (showConnectionSettings) {
        HomeAssistantConnectionDialog(
            connection = savedConnection,
            onVerify = { delegate.fetchHomeAssistantEntities(it.serverUrl, it.accessToken) },
            onSave = {
                delegate.saveHomeAssistantConnection(it)
                showConnectionSettings = false
            },
            onDismiss = { showConnectionSettings = false },
        )
    }

    if (delegate.homeAssistantBottomSheetState != null) {
        HomeAssistantBottomSheetContent(
            sheetState = sheetState,
            onDismissRequest = {
                delegate.homeAssistantBottomSheetState = null
            },
            state = delegate.homeAssistantBottomSheetState!!.let { action ->
                savedConnection?.let {
                    action.copy(serverUrl = it.serverUrl, accessToken = it.accessToken)
                } ?: action
            },
            connectionConfigured = savedConnection != null,
            connectionSaveFailed = connectionSaveFailed,
            onConfigureConnection = { showConnectionSettings = true },
            fetchEntities = delegate::fetchHomeAssistantEntities,
            onDescriptionChanged = {
                delegate.homeAssistantBottomSheetState =
                    delegate.homeAssistantBottomSheetState?.copy(description = it)
            },
            onServerUrlChanged = {
                delegate.homeAssistantBottomSheetState =
                    delegate.homeAssistantBottomSheetState?.copy(
                        serverUrl = it,
                        entityId = "",
                        domain = "",
                    )
            },
            onAccessTokenChanged = {
                delegate.homeAssistantBottomSheetState =
                    delegate.homeAssistantBottomSheetState?.copy(
                        accessToken = it,
                        entityId = "",
                        domain = "",
                    )
            },
            onEntityIdChanged = {
                delegate.homeAssistantBottomSheetState =
                    delegate.homeAssistantBottomSheetState?.copy(
                        entityId = it,
                        domain = it.substringBefore("."),
                    )
            },
            onServiceChanged = {
                delegate.homeAssistantBottomSheetState =
                    delegate.homeAssistantBottomSheetState?.copy(service = it)
            },
            onServiceDataChanged = {
                delegate.homeAssistantBottomSheetState =
                    delegate.homeAssistantBottomSheetState?.copy(serviceData = it)
            },
            onDoneClick = {
                val result = delegate.homeAssistantBottomSheetState
                    ?: return@HomeAssistantBottomSheetContent
                if (savedConnection == null) {
                    val saved = runCatching {
                        delegate.saveHomeAssistantConnection(
                            HomeAssistantConnection(result.serverUrl, result.accessToken),
                        )
                    }
                    if (saved.isFailure) {
                        connectionSaveFailed = true
                        return@HomeAssistantBottomSheetContent
                    }
                }
                delegate.homeAssistantBottomSheetState = null
                delegate.actionResult.update {
                    result.copy(
                        serverUrl = "",
                        accessToken = "",
                        service = result.service.trim(),
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeAssistantBottomSheetContent(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: ActionData.HomeAssistantService,
    connectionConfigured: Boolean = false,
    connectionSaveFailed: Boolean = false,
    onConfigureConnection: () -> Unit = {},
    fetchEntities: (suspend (String, String) -> KMResult<List<HomeAssistantEntity>>)? = null,
    onDescriptionChanged: (String) -> Unit = {},
    onServerUrlChanged: (String) -> Unit = {},
    onAccessTokenChanged: (String) -> Unit = {},
    onEntityIdChanged: (String) -> Unit = {},
    onServiceChanged: (String) -> Unit = {},
    onServiceDataChanged: (String) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val descriptionFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }
    val tokenFocus = remember { FocusRequester() }
    val serviceFocus = remember { FocusRequester() }
    val customServiceFocus = remember { FocusRequester() }
    val serviceDataFocus = remember { FocusRequester() }
    val doneFocus = remember { FocusRequester() }
    var restoreServiceFocus by remember { mutableStateOf(false) }

    var entities by remember { mutableStateOf<List<HomeAssistantEntity>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.serverUrl, state.accessToken, refresh) {
        entities = null
        fetchError = false
        isLoading = false
        val fetch = fetchEntities ?: return@LaunchedEffect
        if (state.serverUrl.trim().toHttpUrlOrNull() == null || state.accessToken.isBlank()) {
            Timber.tag(
                "HomeAssistant",
            ).d("Entity fetch skipped: valid server URL and token required")
            return@LaunchedEffect
        }
        isLoading = true
        try {
            Timber.tag("HomeAssistant").d("Entity picker: waiting for connection input to settle")
            delay(600)
            when (val result = fetch(state.serverUrl, state.accessToken)) {
                is Success -> {
                    entities = result.value.sortedBy { it.friendlyName.lowercase() }
                    Timber.tag(
                        "HomeAssistant",
                    ).d("Entity picker: displaying %d entities", result.value.size)
                }
                else -> {
                    fetchError = true
                    Timber.tag(
                        "HomeAssistant",
                    ).e("Entity picker: fetch failed (%s)", result.javaClass.simpleName)
                }
            }
            isLoading = false
        } catch (e: CancellationException) {
            Timber.tag(
                "HomeAssistant",
            ).d("Entity fetch cancelled: connection input changed or picker closed")
            throw e
        }
    }

    var serviceDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val descriptionEmptyErrorString =
        stringResource(R.string.action_ha_description_empty_error)
    val serverUrlEmptyErrorString =
        stringResource(R.string.action_ha_server_url_empty_error)
    val malformedUrlErrorString =
        stringResource(R.string.action_ha_malformed_url_error)
    val tokenEmptyErrorString =
        stringResource(R.string.action_ha_access_token_empty_error)
    val entityEmptyErrorString =
        stringResource(R.string.action_ha_entity_empty_error)

    var descriptionError: String? by rememberSaveable { mutableStateOf(null) }
    var serverUrlError: String? by rememberSaveable { mutableStateOf(null) }
    var tokenError: String? by rememberSaveable { mutableStateOf(null) }
    var entityError: String? by rememberSaveable { mutableStateOf(null) }
    var serviceError by rememberSaveable { mutableStateOf(false) }

    val services = listOf(
        "turn_on" to stringResource(R.string.action_ha_turn_on),
        "turn_off" to stringResource(R.string.action_ha_turn_off),
        "toggle" to stringResource(R.string.action_ha_toggle),
    ).let { presets ->
        if (presets.any { it.first == state.service }) {
            presets
        } else {
            presets + (state.service to state.service)
        }
    }

    KMBottomSheet(
        title = stringResource(R.string.action_home_assistant_service),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = {
            descriptionError = null
            serverUrlError = null
            tokenError = null
            entityError = null
            serviceError = state.service.isBlank()

            if (state.description.isBlank()) {
                descriptionError = descriptionEmptyErrorString
            }

            if (state.serverUrl.isBlank()) {
                serverUrlError = serverUrlEmptyErrorString
            } else if (state.serverUrl.trim().toHttpUrlOrNull() == null) {
                serverUrlError = malformedUrlErrorString
            }

            if (state.accessToken.isBlank()) {
                tokenError = tokenEmptyErrorString
            }

            if (state.entityId.isBlank()) {
                entityError = entityEmptyErrorString
            }

            if (descriptionError == null &&
                serverUrlError == null &&
                tokenError == null &&
                entityError == null &&
                state.service.isNotBlank()
            ) {
                onDoneClick()
            }
        },
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        posButtonModifier = Modifier.focusRequester(doneFocus).focusProperties {
            up = serviceDataFocus
            previous = serviceDataFocus
        },
    ) {
        if (connectionSaveFailed) {
            Text(
                stringResource(R.string.home_assistant_save_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        // The sheet has its own window and focus owner. Read this inside its
        // content; the caller's focus manager controls the screen behind it.
        val focusManager = LocalFocusManager.current
        val nextFieldActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
        )

        RemoteOutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(descriptionFocus)
                .focusProperties {
                    if (!connectionConfigured) {
                        next = urlFocus
                        down = urlFocus
                    }
                }
                .remoteFieldNavigation(focusManager),
            keyboardActions = nextFieldActions,
            value = state.description,
            label = { Text(stringResource(R.string.action_ha_description_label)) },
            onValueChange = {
                descriptionError = null
                onDescriptionChanged(it)
            },
            maxLines = 1,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
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

        if (connectionConfigured) {
            Text(stringResource(R.string.home_assistant_using_saved_connection))
            TextButton(onClick = onConfigureConnection) {
                Text(stringResource(R.string.home_assistant_edit_connection))
            }
        } else {
            RemoteOutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(urlFocus)
                    .focusProperties {
                        previous = descriptionFocus
                        up = descriptionFocus
                        next = tokenFocus
                        down = tokenFocus
                    }
                    .remoteFieldNavigation(focusManager),
                keyboardActions = nextFieldActions,
                value = state.serverUrl,
                label = { Text(stringResource(R.string.action_ha_server_url_label)) },
                onValueChange = {
                    serverUrlError = null
                    onServerUrlChanged(it)
                },
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                ),
                isError = serverUrlError != null,
                supportingText = {
                    if (serverUrlError != null) {
                        Text(
                            text = serverUrlError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text(stringResource(R.string.action_ha_server_url_supporting_text))
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            RemoteOutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(tokenFocus)
                    .focusProperties {
                        previous = urlFocus
                        up = urlFocus
                    }
                    .remoteFieldNavigation(focusManager),
                keyboardActions = nextFieldActions,
                value = state.accessToken,
                label = { Text(stringResource(R.string.action_ha_access_token_label)) },
                onValueChange = {
                    tokenError = null
                    onAccessTokenChanged(it)
                },
                maxLines = 1,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                ),
                isError = tokenError != null,
                supportingText = {
                    if (tokenError != null) {
                        Text(
                            text = tokenError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(stringResource(R.string.home_assistant_save_connection_hint))
        }

        TextButton(
            enabled = !isLoading &&
                state.serverUrl.trim().toHttpUrlOrNull() != null &&
                state.accessToken.isNotBlank(),
            onClick = { refresh++ },
        ) {
            Text(stringResource(R.string.action_ha_fetch_entities))
        }
        if (isLoading) {
            Text(stringResource(R.string.action_ha_fetching_entities))
        }
        if (fetchError) {
            Text(
                stringResource(R.string.action_ha_fetch_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.action_ha_selected_entity, state.entityId),
        )
        if (entityError != null) {
            Text(entityError!!, color = MaterialTheme.colorScheme.error)
        }
        entities?.let { available ->
            RemoteOutlinedTextField(
                modifier = Modifier.fillMaxWidth().remoteFieldNavigation(focusManager),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = nextFieldActions,
                value = search,
                onValueChange = { search = it },
                label = { Text(stringResource(R.string.action_ha_search_entities)) },
                singleLine = true,
            )
            val filtered = available.filter {
                it.friendlyName.contains(search, ignoreCase = true) ||
                    it.entityId.contains(search, ignoreCase = true)
            }
            if (filtered.isEmpty()) {
                Text(stringResource(R.string.action_ha_no_entities))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                    items(filtered, key = { it.entityId }) { entity ->
                        ListItem(
                            modifier = Modifier.clickable {
                                entityError = null
                                onEntityIdChanged(entity.entityId)
                                if (state.description.isBlank()) {
                                    onDescriptionChanged(entity.friendlyName)
                                }
                            },
                            headlineContent = { Text(entity.friendlyName) },
                            supportingContent = { Text("${entity.entityId} · ${entity.state}") },
                            trailingContent = {
                                if (entity.entityId == state.entityId) {
                                    Text(stringResource(R.string.action_ha_selected))
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LaunchedEffect(serviceDropdownExpanded, restoreServiceFocus) {
            if (!serviceDropdownExpanded && restoreServiceFocus) {
                serviceFocus.requestFocus()
                restoreServiceFocus = false
            }
        }
        Box {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(serviceFocus)
                    .focusProperties {
                        next = customServiceFocus
                        down = customServiceFocus
                    }
                    .remoteFieldNavigation(focusManager),
                onClick = { serviceDropdownExpanded = true },
            ) {
                Text(
                    stringResource(
                        R.string.home_assistant_service_selection,
                        services.first { it.first == state.service }.second,
                    ),
                )
            }
            DropdownMenu(
                expanded = serviceDropdownExpanded,
                onDismissRequest = {
                    serviceDropdownExpanded = false
                    restoreServiceFocus = true
                },
            ) {
                services.forEach { (service, title) ->
                    DropdownMenuItem(
                        text = { Text(title) },
                        onClick = {
                            serviceError = false
                            onServiceChanged(service)
                            serviceDropdownExpanded = false
                            restoreServiceFocus = true
                        },
                    )
                }
            }
        }

        RemoteOutlinedTextField(
            modifier = Modifier.fillMaxWidth()
                .focusRequester(customServiceFocus)
                .focusProperties {
                    previous = serviceFocus
                    up = serviceFocus
                    next = serviceDataFocus
                    down = serviceDataFocus
                }
                .remoteFieldNavigation(focusManager),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = nextFieldActions,
            value = state.service,
            onValueChange = {
                serviceError = false
                onServiceChanged(it)
            },
            isError = serviceError,
            label = { Text(stringResource(R.string.action_ha_service_label)) },
            supportingText = { Text(stringResource(R.string.action_ha_service_help)) },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        RemoteOutlinedTextField(
            modifier = Modifier.fillMaxWidth()
                .focusRequester(serviceDataFocus)
                .focusProperties {
                    previous = customServiceFocus
                    up = customServiceFocus
                    next = doneFocus
                    down = doneFocus
                }
                .remoteFieldNavigation(focusManager),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { doneFocus.requestFocus() }),
            value = state.serviceData,
            label = { Text(stringResource(R.string.action_ha_service_data_label)) },
            onValueChange = {
                onServiceDataChanged(it)
            },
        )
    }
}

// Text fields consume D-pad arrows for cursor movement. Handle vertical navigation
// first so a TV remote can leave the editor and reach fields below the visible area.
internal fun Modifier.remoteFieldNavigation(focusManager: FocusManager): Modifier =
    onPreviewKeyEvent { event ->
        val direction = when (event.key) {
            Key.DirectionDown -> FocusDirection.Next
            Key.DirectionUp -> FocusDirection.Previous
            else -> return@onPreviewKeyEvent false
        }
        if (event.type == KeyEventType.KeyDown) {
            focusManager.moveFocus(direction)
        } else {
            false
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )
        HomeAssistantBottomSheetContent(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ActionData.HomeAssistantService(
                description = "",
                serverUrl = "http://",
                accessToken = "",
                domain = "",
                service = "turn_on",
                entityId = "",
                serviceData = "",
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun PreviewFilled() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )
        HomeAssistantBottomSheetContent(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ActionData.HomeAssistantService(
                description = "Toggle living room light",
                serverUrl = "http://192.168.1.100:8123",
                accessToken = "eyJ0eXAiOiJKV1Q...",
                domain = "light",
                service = "toggle",
                entityId = "light.living_room",
                serviceData = "",
            ),
        )
    }
}
