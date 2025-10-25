package io.github.sds100.keymapper.base.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.compose.KeyMapperTheme

@Composable
fun TipCard(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    buttonText: String? = null,
    isDismissable: Boolean = true,
    onDismiss: () -> Unit = {},
    onButtonClick: () -> Unit = {},
) {
    OutlinedCard(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (buttonText != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .align(Alignment.End),
                        onClick = onButtonClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(buttonText)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isDismissable) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    onClick = onDismiss,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                }
            }
    }
}

@Preview
@Composable
private fun TipCardPreview() {
    KeyMapperTheme {
        TipCard(
            title = "Tip Title",
            message = "This is a helpful tip message that explains something important to the user. It can be multiple lines long and provides useful information.",
            buttonText = "Button"
        )
    }
}
