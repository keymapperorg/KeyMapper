package io.github.sds100.keymapper.util.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

sealed class KMIcon {
    data class ImageVector(val imageVector: androidx.compose.ui.graphics.vector.ImageVector) : KMIcon()
    data class DrawableResource(@DrawableRes val id: Int) : KMIcon()
}

@Composable
fun Icon(modifier: Modifier = Modifier, icon: KMIcon) {
    when (icon) {
        is KMIcon.DrawableResource -> {
            Icon(modifier = modifier, painter = painterResource(icon.id), contentDescription = null)
        }
        is KMIcon.ImageVector -> {
            val painter = rememberVectorPainter(icon.imageVector)
            Icon(modifier = modifier, painter = painter, contentDescription = null)
        }
    }
}