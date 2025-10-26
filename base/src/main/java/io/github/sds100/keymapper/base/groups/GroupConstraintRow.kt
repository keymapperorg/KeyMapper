package io.github.sds100.keymapper.base.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.drawable
import io.github.sds100.keymapper.common.utils.KMError

@Composable
fun GroupConstraintRow(
    modifier: Modifier = Modifier,
    constraints: List<ComposeChipModel>,
    mode: ConstraintMode,
    parentConstraintCount: Int,
    onNewConstraintClick: () -> Unit = {},
    onRemoveConstraintClick: (String) -> Unit = {},
    onFixConstraintClick: (KMError) -> Unit = {},
    enabled: Boolean = true,
) {
    BoxWithConstraints(modifier = modifier) {
        val maxChipWidth =
            LocalDensity.current.run {
                (this@BoxWithConstraints.constraints.maxWidth / 2).toDp()
            }

        FlowRow(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            for ((index, constraint) in constraints.withIndex()) {
                when (constraint) {
                    is ComposeChipModel.Normal ->
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                            ConstraintButton(
                                modifier = Modifier.widthIn(max = maxChipWidth),
                                text = constraint.text,
                                onRemoveClick = { onRemoveConstraintClick(constraint.id) },
                                // Only allow clicking on error chips
                                enabled = enabled,
                                icon = {
                                    if (constraint.icon is ComposeIconInfo.Vector) {
                                        Icon(
                                            modifier =
                                                Modifier
                                                    .size(24.dp)
                                                    .padding(end = 8.dp),
                                            imageVector = constraint.icon.imageVector,
                                            contentDescription = null,
                                        )
                                    } else if (constraint.icon is ComposeIconInfo.Drawable) {
                                        Icon(
                                            modifier =
                                                Modifier
                                                    .size(24.dp)
                                                    .padding(end = 8.dp),
                                            painter = rememberDrawablePainter(constraint.icon.drawable),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                        )
                                    }
                                },
                            )
                        }

                    is ComposeChipModel.Error ->
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer) {
                            ConstraintErrorButton(
                                modifier = Modifier.widthIn(max = maxChipWidth),
                                text = constraint.text,
                                onClick = { onFixConstraintClick(constraint.error) },
                                onRemoveClick = { onRemoveConstraintClick(constraint.id) },
                                // Only allow clicking on error chips
                                enabled = enabled,
                            )
                        }
                }

                if (index < constraints.lastIndex) {
                    when (mode) {
                        ConstraintMode.AND ->
                            Text(
                                text = stringResource(R.string.constraint_mode_and),
                                style = MaterialTheme.typography.labelMedium,
                            )

                        ConstraintMode.OR ->
                            Text(
                                text = stringResource(R.string.constraint_mode_or),
                                style = MaterialTheme.typography.labelMedium,
                            )
                    }
                }
            }

            if (parentConstraintCount > 0) {
                Text(
                    modifier =
                        Modifier
                            .padding(horizontal = 8.dp),
                    text =
                        pluralStringResource(
                            R.plurals.home_groups_inherited_constraints,
                            parentConstraintCount,
                            parentConstraintCount,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            NewConstraintButton(
                onClick = onNewConstraintClick,
                showText = constraints.isEmpty(),
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun NewConstraintButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    showText: Boolean = true,
    enabled: Boolean,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        Surface(
            modifier = modifier,
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f)),
            color = Color.Transparent,
            enabled = enabled,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.home_group_new_constraint_button),
                )

                if (showText) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_group_new_constraint_button),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConstraintButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: @Composable () -> Unit,
    onRemoveClick: () -> Unit = {},
    enabled: Boolean,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .heightIn(min = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon()

                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    modifier = Modifier.size(16.dp),
                    onClick = onRemoveClick,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.home_group_delete_constraint_button),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConstraintErrorButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit = {},
    enabled: Boolean,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            onClick = onClick,
            enabled = enabled,
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .heightIn(min = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                )

                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    modifier = Modifier.size(16.dp),
                    onClick = onRemoveClick,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.home_group_delete_constraint_button),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        Surface {
            GroupConstraintRow(
                constraints = emptyList(),
                mode = ConstraintMode.AND,
                parentConstraintCount = 0,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewOneItem() {
    KeyMapperTheme {
        Surface {
            GroupConstraintRow(
                constraints =
                    listOf(
                        ComposeChipModel.Normal(
                            id = "1",
                            text = "Device is locked",
                            icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                        ),
                    ),
                mode = ConstraintMode.OR,
                parentConstraintCount = 1,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewMultipleItems() {
    val ctx = LocalContext.current

    KeyMapperTheme {
        Surface {
            GroupConstraintRow(
                constraints =
                    listOf(
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
                        ComposeChipModel.Normal(
                            id = "2",
                            text = "Key Mapper is open",
                            icon = null,
                        ),
                        ComposeChipModel.Error(
                            id = "2",
                            text = "Key Mapper not found",
                            error = KMError.AppNotFound("io.github.sds100.keymapper"),
                        ),
                    ),
                mode = ConstraintMode.AND,
                parentConstraintCount = 3,
            )
        }
    }
}
