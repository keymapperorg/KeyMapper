package io.github.sds100.keymapper.base.promode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow

@Composable
fun ProModeScreen(
    modifier: Modifier = Modifier,
    viewModel: ProModeViewModel,
    onNavigateBack: () -> Unit,
) {
    val proModeWarningState by viewModel.proModeWarningState.collectAsStateWithLifecycle()

    ProModeScreen(modifier = modifier, onBackClick = onNavigateBack) {
        Content(
            proModeWarningState = proModeWarningState,
            onWarningButtonClick = viewModel::onWarningButtonClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProModeScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pro_mode_app_bar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_go_back),
                        )
                    }
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
            content()
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    proModeWarningState: ProModeWarningState,
    onWarningButtonClick: () -> Unit = {},
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        WarningCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            state = proModeWarningState,
            onButtonClick = onWarningButtonClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (proModeWarningState is ProModeWarningState.Understood) {
            OptionsHeaderRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = Icons.Rounded.Checklist,
                text = stringResource(R.string.pro_mode_set_up_title),
            )

            Spacer(modifier = Modifier.height(8.dp))

            SetupCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                color = LocalCustomColorsPalette.current.magiskTeal,
                icon = Icons.Rounded.Numbers,
                title = stringResource(R.string.pro_mode_root_detected_title),
                content = {
                    Text(
                        text = stringResource(R.string.pro_mode_root_detected_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                buttonText = stringResource(R.string.pro_mode_root_detected_button),
            )

            Spacer(modifier = Modifier.height(8.dp))

            SetupCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                color = LocalCustomColorsPalette.current.shizukuBlue,
                icon = Icons.Rounded.Android,
                title = stringResource(R.string.pro_mode_shizuku_detected_title),
                content = {
                    Text(
                        text = stringResource(R.string.pro_mode_shizuku_detected_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                buttonText = stringResource(R.string.pro_mode_shizuku_detected_button),
            )
        } else {
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.pro_mode_settings_unavailable_text),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WarningCard(
    modifier: Modifier = Modifier,
    state: ProModeWarningState,
    onButtonClick: () -> Unit = {},
) {
    OutlinedCard(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.pro_mode_warning_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.pro_mode_warning_text),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp),
            onClick = onButtonClick,
            enabled = state is ProModeWarningState.Idle,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            if (state is ProModeWarningState.Understood) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null)

                Spacer(modifier = Modifier.width(8.dp))
            }

            val text = when (state) {
                is ProModeWarningState.CountingDown -> stringResource(
                    R.string.pro_mode_warning_understand_button_countdown,
                    state.seconds,
                )

                ProModeWarningState.Idle -> stringResource(R.string.pro_mode_warning_understand_button_not_completed)
                ProModeWarningState.Understood -> stringResource(R.string.pro_mode_warning_understand_button_completed)
            }

            Text(text)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupCard(
    modifier: Modifier = Modifier,
    color: Color,
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
    buttonText: String,
    onButtonClick: () -> Unit = {},
) {
    OutlinedCard(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp),
            onClick = onButtonClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = color,
                contentColor = LocalCustomColorsPalette.current.contentColorFor(color),
            ),
        ) {
            Text(buttonText)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        ProModeScreen {
            Content(
                proModeWarningState = ProModeWarningState.Understood,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDark() {
    KeyMapperTheme(darkTheme = true) {
        ProModeScreen {
            Content(
                proModeWarningState = ProModeWarningState.Understood,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewCountingDown() {
    KeyMapperTheme {
        ProModeScreen {
            Content(
                proModeWarningState = ProModeWarningState.CountingDown(
                    seconds = 5,
                ),
            )
        }
    }
}
