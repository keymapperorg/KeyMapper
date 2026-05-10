package io.github.sds100.keymapper.trigger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ShareUtils
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KofiSymbol
import io.github.sds100.keymapper.base.utils.ui.compose.openUriSafe

private val KofiButtonOrange = Color(0xFFFF5A16)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTriggersScreenFoss(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val ctx = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.support_key_mapper_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                R.string.bottom_app_bar_back_content_description,
                            ),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.support_key_mapper_subtitle),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.support_key_mapper_sub_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.foss_advanced_triggers_support_body_main),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            val emailSubject = stringResource(R.string.customer_email_subject_foss_support)

            Text(
                text = buildContactUsAnnotatedString(
                    onContactUsClick = { ShareUtils.sendBugReportEmail(ctx, emailSubject) },
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.foss_advanced_triggers_support_values),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.foss_advanced_triggers_support_perks),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            val uriHandler = LocalUriHandler.current
            val ctx = LocalContext.current
            val kofiUrl = stringResource(R.string.url_kofi)
            val googlePlayUrl = stringResource(R.string.url_play_store_listing)

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { uriHandler.openUriSafe(ctx, kofiUrl) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KofiButtonOrange,
                    contentColor = Color.White,
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = KeyMapperIcons.KofiSymbol,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = Color.Unspecified,
                    )
                    Text(stringResource(R.string.foss_advanced_triggers_kofi_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    uriHandler.openUriSafe(ctx, googlePlayUrl)
                },
            ) {
                Text(stringResource(R.string.purchasing_download_key_mapper_from_google_play))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun buildContactUsAnnotatedString(onContactUsClick: () -> Unit): AnnotatedString {
    return buildAnnotatedString {
        append(stringResource(R.string.foss_advanced_triggers_support_contact_prefix))

        withLink(
            LinkAnnotation.Clickable(tag = "foss_support_contact_us") {
                onContactUsClick()
            },
        ) {
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(stringResource(R.string.foss_advanced_triggers_support_contact_link))
            }
        }

        append(stringResource(R.string.foss_advanced_triggers_support_contact_suffix))
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        AdvancedTriggersScreenFoss(
            onBack = {},
        )
    }
}
