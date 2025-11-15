package io.github.sds100.keymapper.base.logging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette

@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    viewModel: LogViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    val log = viewModel.log.collectAsStateWithLifecycle().value

    LogScreen(
        modifier = modifier,
        onBackClick = onBackClick,
        onCopyToClipboardClick = viewModel::onCopyToClipboardClick,
        onShareClick = viewModel::onShareFileClick,
        onClearLogClick = viewModel::onClearLogClick,
        content = {
            Content(
                modifier = Modifier.fillMaxSize(),
                logListItems = log,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onCopyToClipboardClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onClearLogClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_pref_view_and_share_log)) },
                actions = {
                    OutlinedButton(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = onClearLogClick,
                    ) {
                        Text(stringResource(R.string.action_clear_log))
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_go_back),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.action_share_log),
                    )
                }

                IconButton(onClick = onCopyToClipboardClick) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.action_copy_log),
                    )
                }
            }
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
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(modifier: Modifier = Modifier, logListItems: List<LogListItem>) {
    val listState = rememberLazyListState()

    // Scroll to the bottom when a new item is added
    LaunchedEffect(logListItems) {
        if (logListItems.isNotEmpty()) {
            listState.animateScrollToItem(logListItems.size - 1)
        }
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(logListItems, key = { it.id }) { item ->
                val color = when (item.severity) {
                    LogSeverity.ERROR -> MaterialTheme.colorScheme.error
                    LogSeverity.WARNING -> LocalCustomColorsPalette.current.orange
                    LogSeverity.INFO -> LocalCustomColorsPalette.current.green
                    else -> LocalContentColor.current
                }

                Row {
                    Text(
                        text = item.time,
                        color = color,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.message,
                        color = color,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        LogScreen(
            content = {
                Content(
                    logListItems = listOf(
                        LogListItem(1, "12:34:56.789", LogSeverity.INFO, "This is an info message"),
                        LogListItem(
                            2,
                            "12:34:57.123",
                            LogSeverity.WARNING,
                            "This is a warning message",
                        ),
                        LogListItem(
                            3,
                            "12:34:58.456",
                            LogSeverity.ERROR,
                            "This is an error message. It is a bit long to see how it overflows inside the available space.",
                        ),
                        LogListItem(4, "12:34:59.000", LogSeverity.INFO, "Another info message"),
                        LogListItem(
                            5,
                            "12:35:00.000",
                            LogSeverity.ERROR,
                            "Error recording trigger",
                        ),
                        LogListItem(6, "12:35:01.000", LogSeverity.WARNING, "I am a warning"),
                        LogListItem(7, "12:35:02.000", LogSeverity.INFO, "I am some info..."),
                        LogListItem(8, "12:35:03.000", LogSeverity.INFO, "This more info"),
                    ),
                )
            },
        )
    }
}
