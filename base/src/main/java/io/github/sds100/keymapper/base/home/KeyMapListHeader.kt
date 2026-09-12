package io.github.sds100.keymapper.base.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.groups.GroupBreadcrumbRow
import io.github.sds100.keymapper.base.groups.GroupConstraintRow
import io.github.sds100.keymapper.base.groups.GroupListItemModel
import io.github.sds100.keymapper.base.groups.GroupRow
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.RadioButtonText
import io.github.sds100.keymapper.base.utils.ui.drawable
import io.github.sds100.keymapper.common.utils.KMError

/**
 * The warnings, groups, breadcrumbs and group constraints that are shown above the key maps.
 * This is placed in the key map list rather than the app bar so that it scrolls away and does not
 * permanently take up vertical space on small screens or in landscape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyMapListHeader(
    modifier: Modifier = Modifier,
    state: KeyMapAppBarState,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onFixWarningClick: (String) -> Unit = {},
    onNewGroupClick: () -> Unit = {},
    onGroupClick: (String?) -> Unit = {},
    onNewConstraintClick: () -> Unit = {},
    onRemoveConstraintClick: (String) -> Unit = {},
    onConstraintModeChanged: (ConstraintMode) -> Unit = {},
    onFixConstraintClick: (KMError) -> Unit = {},
    onKeyMapsEnabledChange: (Boolean) -> Unit = {},
) {
    // This is taken from the AppBar color code so the header is the same color as the app bar
    // above it.
    val colorTransitionFraction by
        remember(scrollBehavior) {
            // derivedStateOf to prevent redundant recompositions when the content scrolls.
            derivedStateOf {
                val overlappingFraction = scrollBehavior.state.overlappedFraction
                if (overlappingFraction > 0.01f) 1f else 0f
            }
        }

    val appBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors()

    val appBarContainerColor by animateColorAsState(
        targetValue = lerp(
            appBarColors.containerColor,
            appBarColors.scrolledContainerColor,
            FastOutLinearInEasing.transform(colorTransitionFraction),
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    when (state) {
        is KeyMapAppBarState.RootGroup -> RootGroupHeader(
            modifier = modifier.fillMaxWidth(),
            state = state,
            containerColor = appBarContainerColor,
            onFixWarningClick = onFixWarningClick,
            onNewGroupClick = onNewGroupClick,
            onGroupClick = onGroupClick,
        )

        is KeyMapAppBarState.ChildGroup -> ChildGroupHeader(
            modifier = modifier.fillMaxWidth(),
            state = state,
            onNewGroupClick = onNewGroupClick,
            onGroupClick = onGroupClick,
            onNewConstraintClick = onNewConstraintClick,
            onRemoveConstraintClick = onRemoveConstraintClick,
            onConstraintModeChanged = onConstraintModeChanged,
            onFixConstraintClick = onFixConstraintClick,
            onKeyMapsEnabledChange = onKeyMapsEnabledChange,
        )

        // The groups and breadcrumbs are shown in the selection bottom sheet instead.
        is KeyMapAppBarState.Selecting -> Spacer(modifier.fillMaxWidth())
    }
}

@Composable
private fun RootGroupHeader(
    modifier: Modifier = Modifier,
    state: KeyMapAppBarState.RootGroup,
    containerColor: Color,
    onFixWarningClick: (String) -> Unit,
    onNewGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
) {
    Column(modifier) {
        AnimatedVisibility(visible = state.warnings.isNotEmpty()) {
            // Use separate Surfaces so the animation doesn't jump when they both disappear
            // going into selection mode.
            Surface(color = containerColor) {
                HomeWarningList(
                    modifier = Modifier.padding(bottom = 8.dp),
                    warnings = state.warnings,
                    onFixClick = onFixWarningClick,
                )
            }
        }

        Surface(color = containerColor) {
            GroupRow(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                groups = state.subGroups,
                onNewGroupClick = onNewGroupClick,
                onGroupClick = onGroupClick,
                isSubgroups = false,
            )
        }
    }
}

@Composable
private fun ChildGroupHeader(
    modifier: Modifier = Modifier,
    state: KeyMapAppBarState.ChildGroup,
    onNewGroupClick: () -> Unit,
    onGroupClick: (String?) -> Unit,
    onNewConstraintClick: () -> Unit,
    onRemoveConstraintClick: (String) -> Unit,
    onConstraintModeChanged: (ConstraintMode) -> Unit,
    onFixConstraintClick: (KMError) -> Unit,
    onKeyMapsEnabledChange: (Boolean) -> Unit,
) {
    val enabled = !state.isEditingGroupName

    Column(modifier) {
        // The constraints and the enabled switch are part of the app bar so they use the same
        // color as it.
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column {
                GroupConstraintRow(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    constraints = state.constraints,
                    mode = state.constraintMode,
                    parentConstraintCount = state.parentConstraintCount,
                    onFixConstraintClick = onFixConstraintClick,
                    onNewConstraintClick = onNewConstraintClick,
                    onRemoveConstraintClick = onRemoveConstraintClick,
                    enabled = enabled,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(visible = state.constraints.size > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButtonText(
                                text = stringResource(R.string.constraint_mode_and),
                                isSelected = state.constraintMode == ConstraintMode.AND,
                                isEnabled = enabled,
                                onSelected = {
                                    onConstraintModeChanged(ConstraintMode.AND)
                                },
                            )

                            RadioButtonText(
                                text = stringResource(R.string.constraint_mode_or),
                                isSelected = state.constraintMode == ConstraintMode.OR,
                                isEnabled = enabled,
                                onSelected = {
                                    onConstraintModeChanged(ConstraintMode.OR)
                                },
                            )

                            VerticalDivider(
                                modifier = Modifier.height(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    val text = when (state.keyMapsEnabled) {
                        SelectedKeyMapsEnabled.ALL -> stringResource(
                            R.string.home_enabled_key_maps_enabled,
                        )

                        SelectedKeyMapsEnabled.MIXED -> stringResource(
                            R.string.home_enabled_key_maps_mixed,
                        )

                        SelectedKeyMapsEnabled.NONE, null -> stringResource(
                            R.string.home_enabled_key_maps_disabled,
                        )
                    }

                    Switch(
                        checked = state.keyMapsEnabled == SelectedKeyMapsEnabled.ALL,
                        onCheckedChange = onKeyMapsEnabledChange,
                        enabled = state.keyMapsEnabled != null,
                    )

                    Spacer(Modifier.width(16.dp))

                    Text(text = text, style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.width(16.dp))
                }
            }
        }

        Surface {
            Column {
                GroupBreadcrumbRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    groups = state.breadcrumbs,
                    onGroupClick = onGroupClick,
                    enabled = enabled,
                )

                GroupRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    groups = state.subGroups,
                    onNewGroupClick = onNewGroupClick,
                    onGroupClick = onGroupClick,
                    enabled = enabled,
                    isSubgroups = true,
                )
            }
        }
    }
}

@Composable
internal fun constraintsSampleList(): List<ComposeChipModel> {
    val ctx = LocalContext.current

    return listOf(
        ComposeChipModel.Normal(
            id = "1",
            text = "Device is locked",
            icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
        ),
        ComposeChipModel.Normal(
            id = "2",
            text = "Key Mapper is open",
            icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
        ),
        ComposeChipModel.Error(
            id = "2",
            text = "Key Mapper not found",
            error = KMError.AppNotFound("io.github.sds100.keymapper"),
        ),
    )
}

@Composable
internal fun groupSampleList(): List<GroupListItemModel> {
    val ctx = LocalContext.current

    return listOf(
        GroupListItemModel(
            uid = "1",
            name = "Lockscreen",
            icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
        ),
        GroupListItemModel(
            uid = "2",
            name = "Key Mapper",
            icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
        ),
        GroupListItemModel(
            uid = "3",
            name = "Key Mapper",
            icon = null,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RootGroupHeaderPreview() {
    val state = KeyMapAppBarState.RootGroup(
        subGroups = groupSampleList(),
        warnings = listOf(
            HomeWarningListItem(
                id = "0",
                text = stringResource(R.string.home_error_accessibility_service_is_disabled),
            ),
            HomeWarningListItem(
                id = "1",
                text = stringResource(R.string.home_error_is_battery_optimised),
            ),
        ),
        isPaused = true,
    )

    KeyMapperTheme {
        Surface {
            KeyMapListHeader(modifier = Modifier.fillMaxWidth(), state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RootGroupHeaderNoWarningsPreview() {
    val state = KeyMapAppBarState.RootGroup(
        subGroups = groupSampleList(),
        warnings = emptyList(),
        isPaused = false,
    )

    KeyMapperTheme(darkTheme = true) {
        Surface {
            KeyMapListHeader(modifier = Modifier.fillMaxWidth(), state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ChildGroupHeaderPreview() {
    val state = KeyMapAppBarState.ChildGroup(
        groupName = "Lockscreen",
        subGroups = groupSampleList(),
        constraints = constraintsSampleList(),
        parentConstraintCount = 1,
        constraintMode = ConstraintMode.AND,
        breadcrumbs = groupSampleList(),
        isEditingGroupName = false,
        isNewGroup = false,
        keyMapsEnabled = SelectedKeyMapsEnabled.ALL,
    )

    KeyMapperTheme {
        Surface {
            KeyMapListHeader(modifier = Modifier.fillMaxWidth(), state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ChildGroupHeaderDarkPreview() {
    val state = KeyMapAppBarState.ChildGroup(
        groupName = "Lockscreen",
        subGroups = emptyList(),
        constraints = emptyList(),
        parentConstraintCount = 0,
        constraintMode = ConstraintMode.AND,
        breadcrumbs = emptyList(),
        isEditingGroupName = false,
        isNewGroup = false,
        keyMapsEnabled = SelectedKeyMapsEnabled.MIXED,
    )

    KeyMapperTheme(darkTheme = true) {
        Surface {
            KeyMapListHeader(modifier = Modifier.fillMaxWidth(), state = state)
        }
    }
}
