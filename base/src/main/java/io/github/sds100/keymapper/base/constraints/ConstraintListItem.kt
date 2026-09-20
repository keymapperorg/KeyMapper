package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CompactErrorButton
import io.github.sds100.keymapper.base.utils.ui.compose.CompactFilledTonalButton
import io.github.sds100.keymapper.base.utils.ui.compose.CompactOutlinedButton
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.drawable

@Composable
fun ConstraintListItem(
    modifier: Modifier = Modifier,
    model: ConstraintListItemModel,
    isDragging: Boolean = false,
    onRemoveClick: () -> Unit = {},
    onFixClick: () -> Unit = {},
    onNotClick: () -> Unit = {},
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = if (isDragging) {
            CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        } else {
            CardDefaults.elevatedCardElevation()
        },
    ) {
        val listItemModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp)

        BoxWithConstraints {
            when {
                maxWidth < 400.dp -> {
                    ConstraintListItemMedium(
                        modifier = listItemModifier,
                        model = model,
                        onFixClick = onFixClick,
                        onNotClick = onNotClick,
                        onRemoveClick = onRemoveClick,
                    )
                }

                else -> {
                    ConstraintListItemLarge(
                        modifier = listItemModifier,
                        model = model,
                        onFixClick = onFixClick,
                        onNotClick = onNotClick,
                        onRemoveClick = onRemoveClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConstraintListItemLarge(
    modifier: Modifier = Modifier,
    model: ConstraintListItemModel,
    onFixClick: () -> Unit,
    onNotClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))

        ConstraintIcon(icon = model.icon)

        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = model.text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (model.error != null && model.isErrorFixable) {
            CompactErrorButton(onClick = onFixClick) {
                Text(stringResource(R.string.button_fix))
            }
        }

        Spacer(Modifier.width(8.dp))

        NotToggle(isNot = model.isNot, onClick = onNotClick)

        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides 16.dp,
        ) {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = stringResource(R.string.constraint_list_item_remove),
                )
            }
        }
    }
}

@Composable
private fun ConstraintListItemMedium(
    modifier: Modifier = Modifier,
    model: ConstraintListItemModel,
    onFixClick: () -> Unit,
    onNotClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))

        ConstraintIcon(icon = model.icon)

        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = model.text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides 16.dp,
        ) {
            if (model.error != null && model.isErrorFixable) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = onFixClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = stringResource(R.string.button_fix),
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            NotToggle(isNot = model.isNot, onClick = onNotClick)

            IconButton(onClick = onRemoveClick) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = stringResource(R.string.constraint_list_item_remove),
                )
            }
        }
    }
}

@Composable
private fun NotToggle(modifier: Modifier = Modifier, isNot: Boolean, onClick: () -> Unit) {
    val contentDescription = stringResource(R.string.constraint_list_item_not)

    val modifier = modifier
        .toggleable(value = isNot, role = Role.Checkbox, onValueChange = { onClick() })
        .semantics { this.contentDescription = contentDescription }

    if (isNot) {
        CompactFilledTonalButton(
            modifier = modifier,
            onClick = onClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.inversePrimary,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Text(text = stringResource(R.string.constraint_not), fontWeight = FontWeight.Black)
        }
    } else {
        CompactOutlinedButton(
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.inversePrimary),
            onClick = onClick,
        ) {
            Text(text = stringResource(R.string.constraint_not))
        }
    }
}

@Composable
private fun ConstraintIcon(modifier: Modifier = Modifier, icon: ComposeIconInfo) {
    when (icon) {
        is ComposeIconInfo.Vector -> Icon(
            modifier = modifier.size(24.dp),
            imageVector = icon.imageVector,
            contentDescription = null,
        )

        is ComposeIconInfo.Drawable -> {
            val painter = rememberDrawablePainter(icon.drawable)
            Icon(
                modifier = modifier.size(24.dp),
                painter = painter,
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
    }
}

@PreviewLightDark
@Preview(widthDp = 200)
@Composable
private fun VectorPreview() {
    KeyMapperTheme {
        ConstraintListItem(
            model = ConstraintListItemModel(
                id = "id",
                icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
                text = "Clear all",
            ),
        )
    }
}

@PreviewLightDark
@Preview(widthDp = 200)
@Composable
private fun NotErrorPreview() {
    KeyMapperTheme {
        ConstraintListItem(
            model = ConstraintListItemModel(
                id = "id",
                icon = ComposeIconInfo.Vector(Icons.Outlined.FlashlightOn),
                text = "Flashlight is on",
                isNot = true,
                error = "Permission need to control camera",
                isErrorFixable = true,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DrawablePreview() {
    val drawable = LocalContext.current.drawable(R.mipmap.ic_launcher_round)

    KeyMapperTheme {
        ConstraintListItem(
            model = ConstraintListItemModel(
                id = "id",
                text = "Key Mapper is in foreground",
                icon = ComposeIconInfo.Drawable(drawable),
            ),
        )
    }
}
