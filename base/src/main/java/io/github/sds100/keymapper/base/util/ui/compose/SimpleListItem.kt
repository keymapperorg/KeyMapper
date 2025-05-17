package io.github.sds100.keymapper.base.util.ui.compose

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.util.drawable

@Composable
fun SimpleListItemHeader(
    modifier: Modifier = Modifier,
    text: String,
) {
    Surface {
        Text(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
fun SimpleListItemFixedHeight(
    modifier: Modifier = Modifier,
    model: SimpleListItemModel,
    onClick: () -> Unit = {},
) {
    SimpleListItem(
        modifier = modifier.height(56.dp),
        model = model,
        onClick = onClick,
    )
}

@Composable
fun SimpleListItem(
    modifier: Modifier = Modifier,
    model: SimpleListItemModel,
    onClick: () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 16.dp,
    ) {
        OutlinedCard(
            modifier = modifier.height(IntrinsicSize.Min),
            onClick = onClick,
            enabled = model.isEnabled,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(16.dp))

                when (model.icon) {
                    is ComposeIconInfo.Vector -> Icon(
                        modifier = Modifier.size(26.dp),
                        imageVector = model.icon.imageVector,
                        contentDescription = null,
                        tint = LocalContentColor.current,
                    )

                    is ComposeIconInfo.Drawable -> {
                        val painter = rememberDrawablePainter(model.icon.drawable)
                        Icon(
                            modifier = Modifier.size(26.dp),
                            painter = painter,
                            contentDescription = null,
                            tint = Color.Unspecified,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .heightIn(min = 36.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = model.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (model.subtitle == null) {
                            2
                        } else {
                            1
                        },
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (model.subtitle != null) {
                        Text(
                            text = model.subtitle,
                            color = if (model.isSubtitleError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewHeader() {
    KeyMapperTheme {
        Surface {
            SimpleListItemHeader(
                modifier = Modifier.fillMaxWidth(),
                text = "Connectivity",
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        Surface {
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                model = SimpleListItemModel(
                    "app",
                    title = "Enable WiFi",
                    icon = ComposeIconInfo.Vector(Icons.Rounded.Wifi),
                    subtitle = "Requires root",
                    isSubtitleError = true,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDisabled() {
    KeyMapperTheme {
        Surface {
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                model = SimpleListItemModel(
                    "app",
                    title = "Enable WiFi",
                    icon = ComposeIconInfo.Vector(Icons.Rounded.Wifi),
                    subtitle = "Requires root",
                    isSubtitleError = true,
                    isEnabled = false,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDrawable() {
    KeyMapperTheme {
        Surface {
            SimpleListItem(
                model = SimpleListItemModel(
                    "app",
                    title = "Key Mapper",
                    icon = ComposeIconInfo.Drawable(LocalContext.current.drawable(R.mipmap.ic_launcher_round)),
                    subtitle = null,
                    isSubtitleError = true,
                ),
            )
        }
    }
}
