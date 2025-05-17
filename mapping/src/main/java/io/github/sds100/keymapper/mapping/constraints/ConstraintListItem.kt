package io.github.sds100.keymapper.mapping.constraints

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.base.util.drawable
import io.github.sds100.keymapper.base.util.ui.compose.ComposeIconInfo

@Composable
fun ConstraintListItem(
    modifier: Modifier = Modifier,
    model: io.github.sds100.keymapper.mapping.constraints.ConstraintListItemModel,
    onRemoveClick: () -> Unit = {},
    onFixClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .height(IntrinsicSize.Min)
                .padding(start = 16.dp, end = 16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(8.dp))

                if (model.error == null) {
                    when (model.icon) {
                        is ComposeIconInfo.Vector -> Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = model.icon.imageVector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )

                        is ComposeIconInfo.Drawable -> {
                            val painter = rememberDrawablePainter(model.icon.drawable)
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painter,
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }
                    }
                }

                val primaryText = model.text

                Spacer(Modifier.width(8.dp))

                TextColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    primaryText = primaryText,
                    errorText = model.error,
                )

                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides 16.dp,
                ) {
                    if (model.error != null && model.isErrorFixable) {
                        FilledTonalButton(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                            onClick = onFixClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.button_fix),
                            )
                        }
                    }

                    IconButton(onClick = onRemoveClick) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.constraint_list_item_remove),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        if (model.constraintModeLink == null) {
            // Important! Show an empty spacer so the height of the card remains constant
            // while dragging. If the height changes while dragging it can lead to janky
            // behavior.
            Spacer(Modifier.height(32.dp))
        } else {
            Spacer(Modifier.height(4.dp))

            val text = when (model.constraintModeLink) {
                ConstraintMode.AND -> stringResource(R.string.constraint_mode_and)
                ConstraintMode.OR -> stringResource(R.string.constraint_mode_or)
            }

            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = text,
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TextColumn(
    modifier: Modifier = Modifier,
    primaryText: String,
    errorText: String? = null,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = primaryText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview
@Composable
private fun VectorPreview() {
    ConstraintListItem(
        model = io.github.sds100.keymapper.mapping.constraints.ConstraintListItemModel(
            id = "id",
            icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
            constraintModeLink = ConstraintMode.AND,
            text = "Clear all",
            error = null,
            isErrorFixable = true,
        ),
    )
}

@Preview
@Composable
private fun DrawablePreview() {
    val drawable = LocalContext.current.drawable(R.mipmap.ic_launcher_round)

    ConstraintListItem(
        model = io.github.sds100.keymapper.mapping.constraints.ConstraintListItemModel(
            id = "id",
            text = "Dismiss most recent notification",
            error = null,
            isErrorFixable = true,
            icon = ComposeIconInfo.Drawable(drawable),
            constraintModeLink = ConstraintMode.OR,
        ),
    )
}
