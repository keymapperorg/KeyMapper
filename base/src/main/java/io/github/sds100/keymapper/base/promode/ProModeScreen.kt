package io.github.sds100.keymapper.base.promode

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.sds100.keymapper.base.utils.ui.compose.icons.FakeShizuku
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcon
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.common.utils.State

@Composable
fun ProModeScreen(
    modifier: Modifier = Modifier,
    viewModel: ProModeViewModel,
    onNavigateBack: () -> Unit,
) {
    val proModeWarningState by viewModel.warningState.collectAsStateWithLifecycle()
    val proModeSetupState by viewModel.setupState.collectAsStateWithLifecycle()

    ProModeScreen(modifier = modifier, onBackClick = onNavigateBack) {
        Content(
            warningState = proModeWarningState,
            setupState = proModeSetupState,
            onWarningButtonClick = viewModel::onWarningButtonClick,
            onStopServiceClick = viewModel::onStopServiceClick,
            onShizukuButtonClick = viewModel::onShizukuButtonClick,
            onRootButtonClick = viewModel::onRootButtonClick,
            onSetupWithKeyMapperClick = viewModel::onSetupWithKeyMapperClick,
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
    warningState: ProModeWarningState,
    setupState: State<ProModeSetupState>,
    onWarningButtonClick: () -> Unit = {},
    onShizukuButtonClick: () -> Unit = {},
    onStopServiceClick: () -> Unit = {},
    onRootButtonClick: () -> Unit = {},
    onSetupWithKeyMapperClick: () -> Unit = {},
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        WarningCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            state = warningState,
            onButtonClick = onWarningButtonClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (warningState is ProModeWarningState.Understood) {
            when (setupState) {
                is State.Loading -> {
                    CircularProgressIndicator()
                }

                is State.Data -> {
                    SetupSection(
                        modifier = Modifier.fillMaxWidth(),
                        state = setupState.data,
                        onShizukuButtonClick = onShizukuButtonClick,
                        onStopServiceClick = onStopServiceClick,
                        onRootButtonClick = onRootButtonClick,
                        onSetupWithKeyMapperClick = onSetupWithKeyMapperClick,
                    )
                }
            }
        } else {
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.pro_mode_settings_unavailable_text),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupSection(
    modifier: Modifier,
    state: ProModeSetupState,
    onRootButtonClick: () -> Unit = {},
    onShizukuButtonClick: () -> Unit,
    onStopServiceClick: () -> Unit,
    onSetupWithKeyMapperClick: () -> Unit
) {
    Column(modifier) {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Icons.Rounded.Checklist,
            text = stringResource(R.string.pro_mode_set_up_title),
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (state) {
            ProModeSetupState.Started -> ProModeStartedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                onStopClick = onStopServiceClick
            )

            is ProModeSetupState.Stopped -> {
                if (state.isRootGranted) {
                    SetupCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = LocalCustomColorsPalette.current.magiskTeal,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Numbers,
                                contentDescription = null,
                                tint = LocalCustomColorsPalette.current.magiskTeal
                            )
                        },
                        title = stringResource(R.string.pro_mode_root_detected_title),
                        content = {
                            Text(
                                text = stringResource(R.string.pro_mode_root_detected_text),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        buttonText = stringResource(R.string.pro_mode_root_detected_button_start_service),
                        onButtonClick = onRootButtonClick
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                val shizukuButtonText: String? = when (state.shizukuSetupState) {
                    ShizukuSetupState.INSTALLED -> stringResource(R.string.pro_mode_shizuku_detected_button_start)
                    ShizukuSetupState.STARTED -> stringResource(R.string.pro_mode_shizuku_detected_button_request_permission)
                    ShizukuSetupState.PERMISSION_GRANTED -> stringResource(R.string.pro_mode_shizuku_detected_button_start_service)
                    ShizukuSetupState.NOT_FOUND -> null
                }

                if (shizukuButtonText != null) {
                    SetupCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = LocalCustomColorsPalette.current.shizukuBlue,
                        icon = {
                            Image(
                                imageVector = KeyMapperIcons.FakeShizuku,
                                contentDescription = null,
                            )
                        },
                        title = stringResource(R.string.pro_mode_shizuku_detected_title),
                        content = {
                            Text(
                                text = stringResource(R.string.pro_mode_shizuku_detected_text),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        buttonText = shizukuButtonText,
                        onButtonClick = onShizukuButtonClick
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val setupKeyMapperText: String = when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> stringResource(R.string.pro_mode_set_up_with_key_mapper_button_incompatible)
                    else -> stringResource(R.string.pro_mode_set_up_with_key_mapper_button)
                }

                SetupCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    icon = {
                        Image(
                            modifier = Modifier.padding(2.dp),
                            imageVector = KeyMapperIcons.KeyMapperIcon,
                            contentDescription = null,
                        )
                    },
                    title = stringResource(R.string.pro_mode_set_up_with_key_mapper_title),
                    content = {
                        if (state.setupProgress < 1) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                progress = { state.setupProgress })
                        }
                    },
                    buttonText = setupKeyMapperText,
                    onButtonClick = onSetupWithKeyMapperClick,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                )
            }
        }
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
private fun ProModeStartedCard(
    modifier: Modifier = Modifier,
    onStopClick: () -> Unit = {},
) {
    OutlinedCard(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = LocalCustomColorsPalette.current.green
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.pro_mode_service_started),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.width(16.dp))

            TextButton(
                onClick = onStopClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.pro_mode_stop_service_button))
            }

            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
private fun SetupCard(
    modifier: Modifier = Modifier,
    color: Color,
    icon: @Composable () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    buttonText: String,
    onButtonClick: () -> Unit = {},
    enabled: Boolean = true
) {
    OutlinedCard(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(Modifier.size(24.dp)) {
                icon()
            }

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
            enabled = enabled,
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
                warningState = ProModeWarningState.Understood,
                setupState = State.Data(
                    ProModeSetupState.Stopped(
                        isRootGranted = false,
                        shizukuSetupState = ShizukuSetupState.PERMISSION_GRANTED,
                        setupProgress = 0.5f
                    )
                )
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
                warningState = ProModeWarningState.Understood,
                setupState = State.Data(ProModeSetupState.Started)
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
                warningState = ProModeWarningState.CountingDown(
                    seconds = 5,
                ),
                setupState = State.Loading
            )
        }
    }
}
