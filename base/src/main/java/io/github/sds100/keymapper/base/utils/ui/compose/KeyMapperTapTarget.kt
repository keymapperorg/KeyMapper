package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.canopas.lib.showcase.component.ShowcaseStyle
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.onboarding.OnboardingTapTarget

@Composable
fun KeyMapperTapTarget(
    tapTarget: OnboardingTapTarget,
    showSkipButton: Boolean = true,
    onSkipClick: () -> Unit = {},
) {
    val textColor = MaterialTheme.colorScheme.onPrimary
    Column {
        Text(
            text = stringResource(tapTarget.titleRes),
            color = textColor,
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(tapTarget.messageRes),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (showSkipButton) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSkipClick,
                border = BorderStroke(
                    width = 1.dp,
                    color = textColor,
                ),
            ) {
                Text(
                    text = stringResource(R.string.tap_target_skip_tutorial_button),
                    color = textColor,
                )
            }
        }
    }
}

@Composable
fun keyMapperShowcaseStyle(): ShowcaseStyle {
    return ShowcaseStyle(
        backgroundColor = MaterialTheme.colorScheme.primary,
        backgroundAlpha = 0.99f,
    )
}
