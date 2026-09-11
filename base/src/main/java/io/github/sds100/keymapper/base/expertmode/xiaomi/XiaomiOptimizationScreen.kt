package io.github.sds100.keymapper.base.expertmode.xiaomi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme

@Composable
fun XiaomiOptimizationScreen(
    modifier: Modifier = Modifier,
    viewModel: XiaomiOptimizationViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val userMessage = viewModel.userMessage

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage)
            viewModel.onUserMessageShown()
        }
    }

    XiaomiOptimizationScreen(
        modifier = modifier,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::onBackClick,
        onApplyBatteryFixesClick = viewModel::onApplyBatteryFixesClick,
        onMiuiOptimizationChange = viewModel::onMiuiOptimizationChange,
        onOpenAutostartClick = viewModel::onOpenAutostartClick,
        onOpenBatterySaverClick = viewModel::onOpenBatterySaverClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun XiaomiOptimizationScreen(
    modifier: Modifier = Modifier,
    uiState: XiaomiOptimizationUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBackClick: () -> Unit = {},
    onApplyBatteryFixesClick: () -> Unit = {},
    onMiuiOptimizationChange: (Boolean) -> Unit = {},
    onOpenAutostartClick: () -> Unit = {},
    onOpenBatterySaverClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.displayCutoutPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.xiaomi_optimization_title)) },
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
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.xiaomi_optimization_hero_text),
                    style = MaterialTheme.typography.bodyMedium,
                )

                BatteryFixesCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    isBatteryFixesApplied = uiState.isBatteryFixesApplied,
                    isApplying = uiState.isApplyingBatteryFixes,
                    onApplyClick = onApplyBatteryFixesClick,
                )

                Spacer(modifier = Modifier.height(8.dp))

                MiuiOptimizationCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    isMiuiOptimizationDisabled = uiState.isMiuiOptimizationDisabled,
                    isToggling = uiState.isTogglingMiuiOptimization,
                    onCheckedChange = onMiuiOptimizationChange,
                )

                Spacer(modifier = Modifier.height(8.dp))

                OpenSettingsStepCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    icon = Icons.Rounded.PlayArrow,
                    title = stringResource(R.string.xiaomi_optimization_autostart_title),
                    description = stringResource(
                        R.string.xiaomi_optimization_autostart_description,
                    ),
                    onOpenClick = onOpenAutostartClick,
                )

                Spacer(modifier = Modifier.height(8.dp))

                OpenSettingsStepCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(R.string.xiaomi_optimization_battery_saver_title),
                    description = stringResource(
                        R.string.xiaomi_optimization_battery_saver_description,
                    ),
                    onOpenClick = onOpenBatterySaverClick,
                )

                Spacer(modifier = Modifier.height(8.dp))

                StepCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.xiaomi_optimization_lock_recents_title),
                    description = stringResource(
                        R.string.xiaomi_optimization_lock_recents_description,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BatteryFixesCard(
    modifier: Modifier = Modifier,
    isBatteryFixesApplied: Boolean?,
    isApplying: Boolean,
    onApplyClick: () -> Unit,
) {
    StepCard(
        modifier = modifier,
        icon = Icons.Rounded.BatteryChargingFull,
        title = stringResource(R.string.xiaomi_optimization_auto_apply_title),
        description = stringResource(R.string.xiaomi_optimization_auto_apply_description),
    ) {
        if (isBatteryFixesApplied == null) {
            Text(
                text = stringResource(R.string.xiaomi_optimization_not_connected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        FilledTonalButton(
            modifier = Modifier.align(Alignment.End),
            onClick = onApplyClick,
            enabled = isBatteryFixesApplied == false && !isApplying,
        ) {
            if (isApplying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            val buttonText = if (isBatteryFixesApplied == true) {
                stringResource(R.string.xiaomi_optimization_applied)
            } else {
                stringResource(R.string.xiaomi_optimization_auto_apply_button)
            }

            Text(buttonText)
        }
    }
}

@Composable
private fun MiuiOptimizationCard(
    modifier: Modifier = Modifier,
    isMiuiOptimizationDisabled: Boolean?,
    isToggling: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    StepCard(
        modifier = modifier,
        icon = Icons.Rounded.BatteryChargingFull,
        title = stringResource(R.string.xiaomi_optimization_advanced_title),
        description = stringResource(R.string.xiaomi_optimization_advanced_warning),
    ) {
        if (isMiuiOptimizationDisabled == null) {
            Text(
                text = stringResource(R.string.xiaomi_optimization_not_connected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.xiaomi_optimization_advanced_switch),
                style = MaterialTheme.typography.bodyMedium,
            )

            Switch(
                checked = isMiuiOptimizationDisabled == true,
                onCheckedChange = onCheckedChange,
                enabled = isMiuiOptimizationDisabled != null && !isToggling,
            )
        }

        Text(
            text = stringResource(R.string.xiaomi_optimization_advanced_reboot_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OpenSettingsStepCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    onOpenClick: () -> Unit,
) {
    StepCard(
        modifier = modifier,
        icon = icon,
        title = title,
        description = description,
    ) {
        OutlinedButton(
            modifier = Modifier.align(Alignment.End),
            onClick = onOpenClick,
        ) {
            Text(stringResource(R.string.xiaomi_optimization_open_button))
        }
    }
}

@Composable
private fun StepCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    OutlinedCard(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(Modifier.size(24.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )

            content?.invoke(this)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun PreviewConnected() {
    KeyMapperTheme {
        XiaomiOptimizationScreen(
            uiState = XiaomiOptimizationUiState(
                isMiuiOptimizationDisabled = false,
                isBatteryFixesApplied = false,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewDisconnected() {
    KeyMapperTheme {
        XiaomiOptimizationScreen(
            uiState = XiaomiOptimizationUiState(
                isMiuiOptimizationDisabled = null,
                isBatteryFixesApplied = null,
            ),
        )
    }
}
