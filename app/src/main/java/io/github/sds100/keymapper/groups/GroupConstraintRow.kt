package io.github.sds100.keymapper.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo

@Composable
fun GroupConstraintRow(
    modifier: Modifier = Modifier,
    constraints: List<ComposeChipModel>,
    onNewConstraintClick: () -> Unit = {},
    onRemoveConstraintClick: (String) -> Unit = {},
) {
    FlowRow(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NewConstraintButton(
            onClick = onNewConstraintClick,
            showText = constraints.isEmpty(),
        )

        for (constraint in constraints) {
            val contentColor = when (constraint) {
                is ComposeChipModel.Error -> MaterialTheme.colorScheme.onErrorContainer
                is ComposeChipModel.Normal -> MaterialTheme.colorScheme.onSurface
            }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                ConstraintButton(
                    text = constraint.text,
                    isError = constraint is ComposeChipModel.Error,
                    onRemoveClick = { onRemoveConstraintClick(constraint.id) },
                    icon = {
                        when (constraint) {
                            is ComposeChipModel.Normal -> {
                                if (constraint.icon is ComposeIconInfo.Vector) {
                                    Icon(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 8.dp),
                                        imageVector = constraint.icon.imageVector,
                                        contentDescription = null,
                                    )
                                } else if (constraint.icon is ComposeIconInfo.Drawable) {
                                    Icon(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 8.dp),
                                        painter = rememberDrawablePainter(constraint.icon.drawable),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                    )
                                }
                            }

                            is ComposeChipModel.Error -> {
                                Icon(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 8.dp),
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NewConstraintButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    showText: Boolean = true,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        Surface(
            modifier = modifier.height(28.dp),
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f)),
            color = Color.Transparent,
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
    isError: Boolean,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        val color = if (isError) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        }

        Surface(
            modifier = modifier.height(28.dp),
            shape = MaterialTheme.shapes.small,
            color = color,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon()

                Text(
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall,
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(modifier = Modifier.size(16.dp), onClick = onRemoveClick) {
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
            GroupConstraintRow(constraints = emptyList())
        }
    }
}

@Preview
@Composable
private fun PreviewOneItem() {
    KeyMapperTheme {
        Surface {
            GroupConstraintRow(
                constraints = listOf(
                    ComposeChipModel.Normal(
                        id = "1",
                        text = "Device is locked",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                    ),
                ),
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
                constraints = listOf(
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
                        error = Error.AppNotFound(Constants.PACKAGE_NAME),
                    ),
                ),
            )
        }
    }
}
