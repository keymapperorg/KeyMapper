package io.github.sds100.keymapper.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.SignalCellular4Bar
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PhoneShape = RoundedCornerShape(56.dp)
private val ScreenShape = RoundedCornerShape(44.dp)

/**
 * The width the app content is laid out at before being scaled to fit the phone screen.
 */
private const val APP_WIDTH_DP = 400f

/**
 * Decorates an app screen like the Play Store screenshots: headline, subtitle and a phone frame
 * that bleeds off the bottom. Designed for a w540dp-h960dp-xxhdpi canvas (1620x2880).
 *
 * Must be inside a MaterialTheme because the fake status bar uses its colors.
 */
@Composable
fun StoreScreenshotFrame(
    headline: String,
    subtitle: String,
    statusBarColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headline,
            modifier = Modifier.padding(horizontal = 48.dp),
            color = Color.Black,
            fontSize = 56.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )

        Text(
            text = subtitle,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            color = Color.Black,
            fontSize = 30.sp,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        // The phone is taller than the remaining space so it bleeds off the bottom.
        Box(
            Modifier
                .weight(1f)
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .padding(horizontal = 28.dp),
        ) {
            Phone(statusBarColor = statusBarColor, content = content)
        }
    }
}

@Composable
private fun Phone(statusBarColor: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 19.5f),
    ) {
        SideButton(top = 216.dp, height = 120.dp)
        SideButton(top = 422.dp, height = 63.dp)

        Box(
            Modifier
                .fillMaxSize()
                .padding(end = 3.dp)
                .border(2.dp, Color(0xFF5F5F5F), PhoneShape)
                .background(Color.Black, PhoneShape)
                .padding(13.dp)
                .clip(ScreenShape)
                .background(statusBarColor),
        ) {
            BoxWithConstraints {
                val density = LocalDensity.current
                val scale = constraints.maxWidth / with(density) { APP_WIDTH_DP.dp.toPx() }

                CompositionLocalProvider(
                    LocalDensity provides Density(density.density * scale, density.fontScale),
                ) {
                    Column {
                        StatusBar()
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.SideButton(top: Dp, height: Dp) {
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .padding(top = top)
            .size(width = 4.dp, height = height)
            .background(Color(0xFF3A3A3A), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)),
    )
}

@Composable
private fun StatusBar() {
    val color = MaterialTheme.colorScheme.onSurface

    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 24.dp),
    ) {
        Text(
            text = "16:00",
            modifier = Modifier.align(Alignment.CenterStart),
            color = color,
            fontSize = 14.sp,
        )

        Box(
            Modifier
                .align(Alignment.Center)
                .size(20.dp)
                .background(Color(0xFF111111), CircleShape),
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.SignalWifi4Bar, null, Modifier.size(16.dp), tint = color)
            Text(text = "5G", color = color, fontSize = 14.sp)
            Icon(Icons.Rounded.SignalCellular4Bar, null, Modifier.size(16.dp), tint = color)
            Icon(Icons.Rounded.BatteryFull, null, Modifier.size(16.dp), tint = color)
            Text(text = "100%", color = color, fontSize = 14.sp)
        }
    }
}
