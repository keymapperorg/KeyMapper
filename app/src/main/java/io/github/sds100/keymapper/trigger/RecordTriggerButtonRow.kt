package io.github.sds100.keymapper.trigger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canopas.lib.showcase.IntroShowcase
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.util.ui.compose.KeyMapperTapTarget
import io.github.sds100.keymapper.util.ui.compose.keyMapperShowcaseStyle

@Composable
fun RecordTriggerButtonRow(
    modifier: Modifier = Modifier,
    onRecordTriggerClick: () -> Unit = {},
    recordTriggerState: RecordTriggerState,
    onAdvancedTriggersClick: () -> Unit = {},
    showRecordTriggerTapTarget: Boolean = false,
    onRecordTriggerTapTargetCompleted: () -> Unit = {},
    onSkipTapTarget: () -> Unit = {},
    showAdvancedTriggerTapTarget: Boolean = false,
    onAdvancedTriggerTapTargetCompleted: () -> Unit = {},
) {
    Row(modifier) {
        IntroShowcase(
            showIntroShowCase = showRecordTriggerTapTarget,
            onShowCaseCompleted = onRecordTriggerTapTargetCompleted,
            dismissOnClickOutside = true,
        ) {
            RecordTriggerButton(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Bottom)
                    .introShowCaseTarget(0, style = keyMapperShowcaseStyle()) {
                        KeyMapperTapTarget(
                            OnboardingTapTarget.RECORD_TRIGGER,
                            onSkipClick = onSkipTapTarget,
                        )
                    },
                recordTriggerState,
                onClick = onRecordTriggerClick,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IntroShowcase(
            showIntroShowCase = showAdvancedTriggerTapTarget,
            onShowCaseCompleted = onAdvancedTriggerTapTargetCompleted,
            dismissOnClickOutside = true,
        ) {
            AdvancedTriggersButton(
                modifier = Modifier
                    .weight(1f)
                    .introShowCaseTarget(0, style = keyMapperShowcaseStyle()) {
                        KeyMapperTapTarget(
                            OnboardingTapTarget.ADVANCED_TRIGGERS,
                            showSkipButton = false,
                        )
                    },
                isEnabled = recordTriggerState !is RecordTriggerState.CountingDown,
                onClick = onAdvancedTriggersClick,
            )
        }
    }
}

@Composable
private fun RecordTriggerButton(
    modifier: Modifier,
    state: RecordTriggerState,
    onClick: () -> Unit,
) {
    val colors = ButtonDefaults.filledTonalButtonColors().copy(
        containerColor = LocalCustomColorsPalette.current.red,
        contentColor = LocalCustomColorsPalette.current.onRed,
    )

    val text: String = when (state) {
        is RecordTriggerState.CountingDown ->
            stringResource(R.string.button_recording_trigger_countdown, state.timeLeft)

        else ->
            stringResource(R.string.button_record_trigger)
    }

    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        colors = colors,
    ) {
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AdvancedTriggersButton(
    modifier: Modifier,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = modifier) {
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            enabled = isEnabled,
            onClick = onClick,
        ) {
            Text(
                text = stringResource(R.string.button_advanced_triggers),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun PreviewCountingDown() {
    KeyMapperTheme {
        Surface {
            RecordTriggerButtonRow(
                modifier = Modifier.fillMaxWidth(),
                recordTriggerState = RecordTriggerState.CountingDown(3),
            )
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun PreviewStopped() {
    KeyMapperTheme {
        Surface {
            RecordTriggerButtonRow(
                modifier = Modifier.fillMaxWidth(),
                recordTriggerState = RecordTriggerState.Idle,
            )
        }
    }
}
