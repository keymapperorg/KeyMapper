package io.github.sds100.keymapper.base.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.common.utils.State

@Composable
fun KeyMapList(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    listItems: State<List<KeyMapListItemModel>>,
    header: (@Composable () -> Unit)? = null,
    footerText: String? = stringResource(R.string.home_key_map_list_footer_text),
    isSelectable: Boolean = false,
    onClickKeyMap: (String) -> Unit = {},
    onLongClickKeyMap: (String) -> Unit = {},
    onSelectedChange: (String, Boolean) -> Unit = { _, _ -> },
    onFixClick: (String) -> Unit = {},
    bottomListPadding: Dp = 100.dp,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemSpacing = 8.dp
    // The header is flush with the app bar because it is the same color as it.
    val topPadding = if (header == null) 8.dp else 0.dp
    val bottomPadding = 8.dp

    // The loading and empty states must fill the space left over by the header, like they would
    // with a weight of 1f in a Column, so the header is measured.
    var headerHeight by remember { mutableStateOf(0.dp) }

    Surface(modifier = modifier) {
        BoxWithConstraints {
            val remainingHeight = (
                maxHeight - headerHeight - topPadding - bottomListPadding - bottomPadding -
                    if (header == null) 0.dp else itemSpacing
                ).coerceAtLeast(0.dp)

            // Wait for the header to be measured so the loading and empty states are not laid out
            // a header too tall on the first frame and then jump into place.
            val isRemainingHeightMeasured = header == null || headerHeight > 0.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                // The header is in the list rather than the app bar so that it scrolls away and
                // does not take up vertical space on small screens or in landscape.
                if (header != null) {
                    item(key = "header") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged {
                                    headerHeight = with(density) { it.height.toDp() }
                                },
                        ) {
                            header()
                        }
                    }
                }

                when (listItems) {
                    is State.Loading -> {
                        if (isRemainingHeightMeasured) {
                            item(key = "loading") {
                                LoadingList(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(remainingHeight),
                                )
                            }
                        }
                    }

                    is State.Data -> {
                        if (listItems.data.isEmpty()) {
                            if (isRemainingHeightMeasured) {
                                item(key = "empty") {
                                    EmptyKeyMapList(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(remainingHeight),
                                    )
                                }
                            }
                        } else {
                            items(listItems.data, key = { it.uid }) { model ->
                                KeyMapListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    isSelectable = isSelectable,
                                    model = model,
                                    onClickKeyMap = { onClickKeyMap(model.content.uid) },
                                    onLongClickKeyMap = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongClickKeyMap(model.content.uid)
                                    },
                                    onSelectedChange = { onSelectedChange(model.content.uid, it) },
                                    onFixClick = { onFixClick(model.content.uid) },
                                )
                            }

                            if (footerText != null) {
                                item(key = "footer") {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        text = footerText,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }

                            // Give some space at the end of the list so that the FAB doesn't block
                            // the items.
                            item(key = "bottom_padding") {
                                Spacer(Modifier.height(bottomListPadding))
                            }
                        }
                    }
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
private fun EmptyKeyMapList(modifier: Modifier = Modifier) {
    Box(modifier) {
        val shrug = stringResource(R.string.shrug)
        val text = stringResource(R.string.home_key_map_list_empty)
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
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

@Preview
@Composable
private fun ListPreview() {
    KeyMapperTheme {
        KeyMapList(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Data(sameKeyMapListItems()),
            bottomListPadding = 100.dp,
        )
    }
}

@Preview
@Composable
private fun SelectableListPreview() {
    KeyMapperTheme {
        KeyMapList(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Data(sameKeyMapListItems()),
            isSelectable = true,
            bottomListPadding = 100.dp,
        )
    }
}

@Preview
@Composable
private fun EmptyPreview() {
    KeyMapperTheme {
        KeyMapList(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Data(emptyList()),
            bottomListPadding = 100.dp,
        )
    }
}

@Preview
@Composable
private fun LoadingPreview() {
    KeyMapperTheme {
        KeyMapList(
            modifier = Modifier.fillMaxSize(),
            listItems = State.Loading,
            bottomListPadding = 100.dp,
        )
    }
}
