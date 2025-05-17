package io.github.sds100.keymapper.base.actions.uielement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.common.state.State
import io.github.sds100.keymapper.util.ui.compose.CheckBoxText
import io.github.sds100.keymapper.util.ui.compose.KeyMapperDropdownMenu
import io.github.sds100.keymapper.util.ui.compose.SearchAppBarActions
import io.github.sds100.keymapper.util.ui.compose.WindowSizeClassExt.compareTo

@Composable
fun ChooseElementScreen(
    modifier: Modifier = Modifier,
    state: State<SelectUiElementState>,
    query: String?,
    onCloseSearch: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onClickElement: (Long) -> Unit = {},
    onSelectInteractionType: (NodeInteractionType?) -> Unit = {},
    onAdditionalElementsCheckedChange: (Boolean) -> Unit = {},
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val widthSizeClass = windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass
    val heightSizeClass = windowAdaptiveInfo.windowSizeClass.windowHeightSizeClass

    Scaffold(
        modifier.displayCutoutPadding(),
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.imePadding(),
                actions = {
                    SearchAppBarActions(
                        onCloseSearch = onCloseSearch,
                        onNavigateBack = onNavigateBack,
                        onQueryChange = onQueryChange,
                        enabled = state is State.Data,
                        query = query,
                    )
                },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = innerPadding.calculateStartPadding(layoutDirection)
        val endPadding = innerPadding.calculateEndPadding(layoutDirection)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = startPadding,
                    end = endPadding,
                ),
        ) {
            Column {
                Text(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 8.dp,
                    ),
                    text = stringResource(R.string.action_interact_ui_element_choose_element_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                if (heightSizeClass == WindowHeightSizeClass.COMPACT || widthSizeClass >= WindowWidthSizeClass.EXPANDED) {
                    Row {
                        InfoSection(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .weight(1f),
                            state = state,
                            onSelectInteractionType = onSelectInteractionType,
                            onAdditionalElementsCheckedChange = onAdditionalElementsCheckedChange,
                        )

                        ListSection(
                            modifier = Modifier.weight(1f),
                            state = state,
                            onClickElement = onClickElement,
                        )
                    }
                } else {
                    InfoSection(
                        state = state,
                        onSelectInteractionType = onSelectInteractionType,
                        onAdditionalElementsCheckedChange = onAdditionalElementsCheckedChange,
                    )

                    ListSection(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        onClickElement = onClickElement,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    modifier: Modifier = Modifier,
    state: State<SelectUiElementState>,
    onSelectInteractionType: (NodeInteractionType?) -> Unit,
    onAdditionalElementsCheckedChange: (Boolean) -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.action_interact_ui_element_choose_element_text),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_interact_ui_element_choose_element_not_found_subtitle),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.action_interact_ui_element_choose_element_not_found_text),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state is State.Data) {
            var interactionTypeExpanded by rememberSaveable { mutableStateOf(false) }

            CheckBoxText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                text = stringResource(R.string.action_interact_ui_element_checkbox_additional_elements),
                isChecked = state.data.showAdditionalElements,
                onCheckedChange = onAdditionalElementsCheckedChange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            KeyMapperDropdownMenu(
                modifier = Modifier.padding(horizontal = 16.dp),
                expanded = interactionTypeExpanded,
                onExpandedChange = { interactionTypeExpanded = it },
                label = { Text(stringResource(R.string.action_interact_ui_element_filter_interaction_type_dropdown)) },
                values = state.data.interactionTypes,
                selectedValue = state.data.selectedInteractionType,
                onValueChanged = onSelectInteractionType,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ListSection(
    modifier: Modifier = Modifier,
    state: State<SelectUiElementState>,
    onClickElement: (Long) -> Unit,
) {
    when (state) {
        State.Loading -> LoadingList(modifier = modifier.fillMaxSize())
        is State.Data -> {
            val listItems = state.data.listItems

            Column(modifier = modifier) {
                if (listItems.isEmpty()) {
                    EmptyList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                } else {
                    LoadedList(
                        modifier = Modifier.fillMaxSize(),
                        listItems = listItems,
                        onClick = onClickElement,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingList(modifier: Modifier = Modifier) {
    Box(modifier) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun EmptyList(modifier: Modifier = Modifier) {
    Box(modifier) {
        val shrug = stringResource(R.string.shrug)
        val text = stringResource(R.string.ui_element_list_empty)
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = buildAnnotatedString {
                withStyle(MaterialTheme.typography.headlineLarge.toSpanStyle()) {
                    append(shrug)
                }
                appendLine()
                appendLine()
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle()) {
                    append(text)
                }
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadedList(
    modifier: Modifier = Modifier,
    listItems: List<UiElementListItemModel>,
    onClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(listItems, key = { it.id }) { model ->
            UiElementListItem(
                modifier = Modifier.fillMaxWidth(),
                model = model,
                onClick = { onClick(model.id) },
            )
        }
    }
}

@Composable
private fun UiElementListItem(
    modifier: Modifier = Modifier,
    model: UiElementListItemModel,
    onClick: () -> Unit,
) {
    OutlinedCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (model.nodeViewResourceId != null) {
                Text(
                    text = "View ID: ${model.nodeViewResourceId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (model.nodeText != null) {
                Text(
                    text = "\"${model.nodeText}\"",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (model.nodeClassName != null) {
                TextWithLeadingLabel(
                    title = stringResource(R.string.action_interact_ui_element_class_name_label),
                    text = model.nodeClassName,
                )
            }

            if (model.nodeTooltipHint != null) {
                TextWithLeadingLabel(
                    title = stringResource(R.string.action_interact_ui_element_tooltip_label),
                    text = model.nodeTooltipHint,
                )
            }

            if (model.nodeUniqueId != null) {
                TextWithLeadingLabel(
                    title = stringResource(R.string.action_interact_ui_element_unique_id_label),
                    text = model.nodeUniqueId,
                )
            }

            TextWithLeadingLabel(
                title = stringResource(R.string.action_interact_ui_element_interaction_types_label),
                text = model.interactionTypesText,
            )
        }
    }
}

@Composable
private fun TextWithLeadingLabel(
    modifier: Modifier = Modifier,
    title: String,
    text: String,
) {
    val text = buildAnnotatedString {
        pushStyle(
            MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold).toSpanStyle(),
        )
        append(title)
        pop()
        append(": ")
        append(text)
    }

    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview
@Composable
private fun Empty() {
    KeyMapperTheme {
        ChooseElementScreen(
            state = State.Data(
                SelectUiElementState(
                    listItems = emptyList(),
                    interactionTypes = emptyList(),
                    selectedInteractionType = null,
                    showAdditionalElements = false,
                ),
            ),
            query = "Key Mapper",
        )
    }
}

@Preview
@Composable
private fun Loading() {
    KeyMapperTheme {
        ChooseElementScreen(
            state = State.Loading,
            query = null,
        )
    }
}

private val listItems = listOf(
    UiElementListItemModel(
        id = 1L,
        nodeText = "Open Settings",
        nodeClassName = "android.widget.ImageButton",
        nodeViewResourceId = "menu_button",
        nodeUniqueId = "123456789",
        nodeTooltipHint = "Open menu",
        interactionTypesText = "Tap, Tap and hold, Scroll forward",
        interactionTypes = setOf(
            NodeInteractionType.CLICK,
            NodeInteractionType.LONG_CLICK,
            NodeInteractionType.SCROLL_FORWARD,
        ),
        interacted = true,
    ),
)

private val loadedState = SelectUiElementState(
    listItems = listItems,
    interactionTypes = listOf(
        null to "Any",
        NodeInteractionType.CLICK to "Tap",
        NodeInteractionType.LONG_CLICK to "Tap and hold",
    ),
    selectedInteractionType = null,
    showAdditionalElements = true,
)

@Preview
@Composable
private fun LoadedPortrait() {
    KeyMapperTheme {
        ChooseElementScreen(
            state = State.Data(loadedState),
            query = "Key Mapper",
        )
    }
}

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun LoadedPhoneLandscape() {
    KeyMapperTheme {
        ChooseElementScreen(
            state = State.Data(loadedState),
            query = "Key Mapper",
        )
    }
}

@Preview(device = Devices.TABLET)
@Composable
private fun LoadedTablet() {
    KeyMapperTheme {
        ChooseElementScreen(
            state = State.Data(loadedState),
            query = "Key Mapper",
        )
    }
}

@Preview(device = Devices.NEXUS_7)
@Composable
private fun LoadedTabletVertical() {
    KeyMapperTheme {
        ChooseElementScreen(
            state = State.Data(loadedState),
            query = "Key Mapper",
        )
    }
}
